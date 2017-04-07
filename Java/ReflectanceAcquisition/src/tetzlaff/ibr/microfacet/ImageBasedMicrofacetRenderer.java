package tetzlaff.ibr.microfacet;

import java.io.File;
import java.io.IOException;
import java.util.List;

import tetzlaff.gl.AlphaBlendingFunction;
import tetzlaff.gl.AlphaBlendingFunction.Weight;
import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.ColorFormat.DataType;
import tetzlaff.gl.Context;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.LightController;
import tetzlaff.gl.helpers.Material;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.OverrideableLightController;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.ibr.IBRDrawable;
import tetzlaff.ibr.IBRLoadOptions;
import tetzlaff.ibr.IBRLoadingMonitor;
import tetzlaff.ibr.IBRSettings;
import tetzlaff.ibr.ViewSet;
import tetzlaff.ibr.ulf.ULFRenderer;

public class ImageBasedMicrofacetRenderer<ContextType extends Context<ContextType>> implements IBRDrawable<ContextType>
{
	private ContextType context;
	private Program<ContextType> program;
	private Program<ContextType> shadowProgram;
	private ULFRenderer<ContextType> ulfRenderer;
	private ImageBasedMicrofacetDistribution<ContextType> microfacetDistribution;
	private LightController lightController;
	private IBRLoadingMonitor callback;
	private boolean suppressErrors = false;
	
	private Texture3D<ContextType> shadowMaps;
	private FramebufferObject<ContextType> shadowFramebuffer;
	private Renderable<ContextType> shadowRenderable;

	private Program<ContextType> lightProgram;
	private VertexBuffer<ContextType> lightVertices;
	private Texture2D<ContextType> lightTexture;
	private Renderable<ContextType> lightRenderable;
	
    private boolean btfRequested;
    private int btfWidth, btfHeight;
    private File btfExportPath;
	
	public ImageBasedMicrofacetRenderer(ContextType context, Program<ContextType> program, Program<ContextType> shadowProgram, 
			File xmlFile, File meshFile, IBRLoadOptions loadOptions, CameraController cameraController, LightController lightController)
    {
		this.context = context;
		
		this.program = program;
		this.shadowProgram = shadowProgram;

    	this.lightController = lightController;
    	
    	this.ulfRenderer = new ULFRenderer<ContextType>(context, program, xmlFile, meshFile, loadOptions, cameraController);
    }

	@Override
	public void initialize() 
	{
		ulfRenderer.initialize();
		
		if (this.lightProgram == null)
    	{
	    	try
	        {
	    		this.lightProgram = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/ibr/light.frag"))
	    				.createProgram();
	    		this.lightVertices = context.createRectangle();
	    		this.lightRenderable = context.createRenderable(this.lightProgram);
	    		this.lightRenderable.addVertexBuffer("position", lightVertices);
	    		
	    		FloatVertexList lightTextureData = new FloatVertexList(1, 4096);
	    		
	    		int k = 0;
	    		for (int i = 0; i < 64; i++)
	    		{
    				double x = i * 2.0 / 63.0 - 1.0;
	    			
	    			for (int j = 0; j < 64; j++)
	    			{
	    				double y = j * 2.0 / 63.0 - 1.0;
	    				
	    				double rSq = x*x + y*y;
	    				lightTextureData.set(k, 0, (float)(Math.cos(Math.min(Math.sqrt(rSq), 1.0) * Math.PI) + 1.0) * 0.5f);
	    				k++;
	    			}
	    		}
	    		
	    		this.lightTexture = context.get2DColorTextureBuilder(64, 64, lightTextureData)
    					.setInternalFormat(ColorFormat.R8)
    					.setLinearFilteringEnabled(true)
    					.setMipmapsEnabled(true)
    					.createTexture();
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
    	}
		
		ulfRenderer.setResampleSetupCallback((modelView) ->
		{
			setupForDraw();
			
			if (lightController instanceof OverrideableLightController)
			{
		    	float scale = new Vector3(microfacetDistribution.ulf.viewSet.getCameraPose(0)
		    			.times(new Vector4(microfacetDistribution.ulf.proxy.getCentroid(), 1.0f))).length();
				
				((OverrideableLightController)lightController).overrideCameraPose(
						Matrix4.scale(1.0f / scale)
							.times(modelView)
							.times(Matrix4.translate(microfacetDistribution.ulf.proxy.getCentroid()))
			    			.times(new Matrix4(new Matrix3(microfacetDistribution.ulf.viewSet.getCameraPose(0).transpose())))
							.times(Matrix4.scale(scale)));
			}
			
			for (int i = 0; i < lightController.getLightCount(); i++)
			{
				generateShadowMaps(i);
				setupLight(i, 0);
			}
		});
		
		ulfRenderer.setResampleCompleteCallback(() -> 
		{
			if (lightController instanceof OverrideableLightController)
			{
				((OverrideableLightController)lightController).removeCameraPoseOverride();
			}
		});
		
		ulfRenderer.setFidelitySetupCallback((index) ->
		{
			setupForDraw();
			
			ViewSet<ContextType> vset = microfacetDistribution.ulf.viewSet;
			Vector3 lightIntensity = vset.getLightIntensity(vset.getLightIndex(index));
			Vector4 lightPos = vset.getCameraPose(index).quickInverse(0.002f).times(new Vector4(vset.getLightPosition(vset.getLightIndex(index)), 1.0f));
			
			setupLightForFidelity(lightIntensity, new Vector3(lightPos));
		});
		
		try
		{
			String diffuseTextureName = null;
			String normalTextureName = null;
			String specularTextureName = null;
			String roughnessTextureName = null;
			
			// TODO Use more information from the material.  Currently just pulling texture names.
			Material material = ulfRenderer.getLightField().proxy.getMaterial();
			if (material != null)
			{
				if (material.getDiffuseMap() != null)
				{
					diffuseTextureName = material.getDiffuseMap().getMapName();
				}

				if (material.getNormalMap() != null)
				{
					normalTextureName = material.getNormalMap().getMapName();
				}

				if (material.getSpecularMap() != null)
				{
					specularTextureName = material.getSpecularMap().getMapName();
				}

				if (material.getRoughnessMap() != null)
				{
					roughnessTextureName = material.getRoughnessMap().getMapName();
				}
			}
			
			if (ulfRenderer.getLightField().viewSet.getGeometryFileName() != null)
			{
				String prefix = ulfRenderer.getLightField().viewSet.getGeometryFileName().split("\\.")[0];
				diffuseTextureName = diffuseTextureName != null ? diffuseTextureName : prefix + "_Kd.png";
				normalTextureName = normalTextureName != null ? normalTextureName : prefix + "_norm.png";
				specularTextureName = specularTextureName != null ? specularTextureName : prefix + "_Ks.png";
				roughnessTextureName = roughnessTextureName != null ? roughnessTextureName : prefix + "_Pr.png";
			}
			else
			{
				diffuseTextureName = diffuseTextureName != null ? diffuseTextureName : "diffuse.png";
				normalTextureName = normalTextureName != null ? normalTextureName : "normal.png";
				specularTextureName = specularTextureName != null ? specularTextureName : "specular.png";
				roughnessTextureName = roughnessTextureName != null ? roughnessTextureName : "roughness.png";
			}
			
			microfacetDistribution = new ImageBasedMicrofacetDistribution<ContextType>(
					ulfRenderer.getLightField(), 
					new File(ulfRenderer.getGeometryFile().getParentFile(), diffuseTextureName),
					new File(ulfRenderer.getGeometryFile().getParentFile(), normalTextureName),
					new File(ulfRenderer.getGeometryFile().getParentFile(), specularTextureName),
					new File(ulfRenderer.getGeometryFile().getParentFile(), roughnessTextureName),
					new File(ulfRenderer.getGeometryFile().getParentFile(), "mfd.csv"), 
					context);
			
			shadowMaps = context.get2DDepthTextureArrayBuilder(2048, 2048, lightController.getLightCount()).createTexture();

			shadowFramebuffer = context.getFramebufferObjectBuilder(2048, 2048)
				.addDepthAttachment()
				.createFramebufferObject();
			
			shadowRenderable = context.createRenderable(shadowProgram);
			shadowRenderable.addVertexBuffer("position", microfacetDistribution.ulf.positionBuffer);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		// Make sure that everything is loaded onto the graphics card before announcing that loading is complete.
		this.draw();
		
		if (this.callback != null)
		{
			this.callback.loadingComplete();
		}
	}

	@Override
	public void update() 
	{
    	if (this.btfRequested)
    	{
    		try
    		{
				this.exportBTF();
			} 
    		catch (Exception e) 
    		{
				e.printStackTrace();
			}
    		this.btfRequested = false;
    		
    		if (callback != null)
    		{
    			callback.loadingComplete();
    		}
    	}
    	else
    	{
    		ulfRenderer.update(); // Resample requests handled here
    	}
	}
	
	
	private void setupForDraw()
	{
		this.setupForDraw(this.program);
	}
	
	public void setupForDraw(Program<ContextType> p)
	{
		if (microfacetDistribution.normalTexture == null)
		{
			p.setUniform("useNormalTexture", false);
			p.setTexture("normalMap", null);
		}
		else
		{
			p.setUniform("useNormalTexture", this.settings().areTexturesEnabled());
			p.setTexture("normalMap", microfacetDistribution.normalTexture);
		}
		
		if (microfacetDistribution.diffuseTexture == null)
		{
			p.setUniform("useDiffuseTexture", false);
			p.setTexture("diffuseMap", null);
		}
		else
		{
			p.setUniform("useDiffuseTexture", this.settings().areTexturesEnabled());
			p.setTexture("diffuseMap", microfacetDistribution.diffuseTexture);
		}
		
		if (microfacetDistribution.specularTexture == null)
		{
			p.setUniform("useSpecularTexture", false);
			p.setTexture("specularMap", null);
		}
		else
		{
			p.setUniform("useSpecularTexture", this.settings().areTexturesEnabled());
			p.setTexture("specularMap", microfacetDistribution.specularTexture);
		}
		
		if (microfacetDistribution.roughnessTexture == null)
		{
			p.setUniform("useRoughnessTexture", false);
			p.setTexture("roughnessMap", null);
		}
		else
		{
			p.setUniform("useRoughnessTexture", this.settings().areTexturesEnabled());
			p.setTexture("roughnessMap", microfacetDistribution.roughnessTexture);
		}
		
		if (ulfRenderer.getEnvironmentTexture() == null || !lightController.getEnvironmentMappingEnabled())
		{
			p.setUniform("useEnvironmentTexture", false);
			p.setTexture("environmentMap", null);
			ulfRenderer.setEnvironmentTextureEnabled(false);
		}
		else
		{
			p.setUniform("useEnvironmentTexture", true);
			p.setTexture("environmentMap", ulfRenderer.getEnvironmentTexture());
			p.setUniform("environmentMipMapLevel", Math.max(0, Math.min(ulfRenderer.getEnvironmentTexture().getMipmapLevelCount() - 2, 
					(int)Math.ceil(0.5 * 
						Math.log((double)ulfRenderer.getEnvironmentTexture().getWidth() * 
								(double)ulfRenderer.getEnvironmentTexture().getHeight() / (double)microfacetDistribution.ulf.viewSet.getCameraPoseCount() ) 
							/ Math.log(2.0)))));
			p.setUniform("diffuseEnvironmentMipMapLevel", ulfRenderer.getEnvironmentTexture().getMipmapLevelCount() - 2);
			p.setUniform("environmentMapGamma", 
					ulfRenderer.getEnvironmentTexture().isInternalFormatCompressed() || 
					ulfRenderer.getEnvironmentTexture().getInternalUncompressedColorFormat().dataType != DataType.FLOATING_POINT 
					? 2.2f : 1.0f);
			ulfRenderer.setEnvironmentTextureEnabled(true);
		}
		
		if (microfacetDistribution.mfdTexture == null)
		{
			p.setUniform("useMFDTexture", false);
			p.setTexture("mfdMap", null);
		}
		else
		{
			p.setUniform("useMFDTexture", true);
			p.setTexture("mfdMap", microfacetDistribution.mfdTexture);
		}
		
		if (microfacetDistribution.ulf.viewSet.getLuminanceMap() == null)
		{
			p.setUniform("useLuminanceMap", false);
			p.setTexture("luminanceMap", null);
		}
		else
		{
			p.setUniform("useLuminanceMap", true);
			p.setTexture("luminanceMap", microfacetDistribution.ulf.viewSet.getLuminanceMap());
		}
		
		if (microfacetDistribution.ulf.viewSet.getInverseLuminanceMap() == null)
		{
			p.setUniform("useInverseLuminanceMap", false);
			p.setTexture("inverseLuminanceMap", null);
		}
		else
		{
			p.setUniform("useInverseLuminanceMap", true);
			p.setTexture("inverseLuminanceMap", microfacetDistribution.ulf.viewSet.getInverseLuminanceMap());
		}
		
		float gamma = 2.2f;
		p.setUniform("ambientColor", lightController.getAmbientLightColor());
    	
    	Vector3 clearColor = new Vector3(
    			(float)Math.pow(lightController.getAmbientLightColor().x, 1.0 / gamma),
    			(float)Math.pow(lightController.getAmbientLightColor().y, 1.0 / gamma),
    			(float)Math.pow(lightController.getAmbientLightColor().z, 1.0 / gamma));
    	ulfRenderer.setClearColor(clearColor);
    	
		p.setUniform("infiniteLightSources", false);
		p.setTexture("shadowMaps", shadowMaps);
		
		if (microfacetDistribution.shadowMatrixBuffer == null || microfacetDistribution.shadowTextures == null)
		{
			p.setUniform("shadowTestingEnabled", false);
		}
		else
		{
			p.setUniform("shadowTestingEnabled", true);
			p.setUniformBuffer("ShadowMatrices", microfacetDistribution.shadowMatrixBuffer);
			p.setTexture("shadowImages", microfacetDistribution.shadowTextures);
		}
	}
	
	private float getScale()
	{
		 return new Vector3(microfacetDistribution.ulf.viewSet.getCameraPose(0)
				 .times(new Vector4(microfacetDistribution.ulf.proxy.getCentroid(), 1.0f))).length()
			 * this.ulfRenderer.getBoundingRadius() / this.ulfRenderer.getLightField().proxy.getBoundingRadius();
	}
	
	private Matrix4 getLightMatrix(int lightIndex)
	{
		float scale = getScale();
		return Matrix4.scale(scale)
			.times(lightController.getLightMatrix(lightIndex))
			.times(Matrix4.scale(1.0f / scale))
			.times(new Matrix4(new Matrix3(microfacetDistribution.ulf.viewSet.getCameraPose(0))))
			.times(Matrix4.translate(ulfRenderer.getCentroid().negated()));
	}
	
	private Matrix4 getLightProjection(int lightIndex)
	{
		Matrix4 lightMatrix = getLightMatrix(lightIndex);
		
		float lightDist = new Vector3(lightMatrix.times(new Vector4(ulfRenderer.getCentroid(), 1.0f))).length();
		
		float radius = (float)
			(new Matrix3(microfacetDistribution.ulf.viewSet.getCameraPose(0))
				.times(new Vector3(this.ulfRenderer.getBoundingRadius()))
				.length() / Math.sqrt(3));
		
		return Matrix4.perspective(2.0f * (float)Math.atan(radius / lightDist) /*1.5f*/, 1.0f, 
				lightDist - radius,
				lightDist + radius);
	}
	
	private void generateShadowMaps(int lightIndex)
	{
		shadowProgram.setUniform("projection", getLightProjection(lightIndex));
		
		shadowFramebuffer.setDepthAttachment(shadowMaps.getLayerAsFramebufferAttachment(lightIndex));
		shadowFramebuffer.clearDepthBuffer();
		
		for (Matrix4 m : this.ulfRenderer.getTransformationMatrices())
		{
			shadowProgram.setUniform("model_view", getLightMatrix(lightIndex).times(m));
			shadowRenderable.draw(PrimitiveMode.TRIANGLES, shadowFramebuffer);
		}
	}
	
	private Matrix4 setupLight(int lightIndex, int modelInstance)
	{
		Matrix4 lightMatrix = getLightMatrix(lightIndex).times(ulfRenderer.getTransformationMatrices().get(modelInstance));
		
		// lightMatrix can be hardcoded here
			//Matrix4.rotateY(-12 * Math.PI / 16).times(Matrix4.rotateX(0 * Math.PI / 16));
			//Matrix4.rotateY(3 * Math.PI / 4).times(Matrix4.rotateX(-Math.PI / 4))
			//	.times(new Matrix4(new Matrix3(microfacetField.ulf.viewSet.getCameraPose(0))));
		
		if (lightIndex == 0)
		{
			program.setUniform("envMapMatrix", lightMatrix);
		}
		Vector3 lightPos = new Vector3(lightMatrix.quickInverse(0.001f).times(new Vector4(0.0f, 0.0f, 0.0f, 1.0f)));
		
		program.setUniform("lightPosVirtual[" + lightIndex + "]", lightPos);
		
		Vector3 controllerLightIntensity = lightController.getLightColor(lightIndex);
		float lightDistance = new Vector3(getLightMatrix(lightIndex).times(new Vector4(ulfRenderer.getCentroid(), 1.0f))).length();

		float scale = new Vector3(microfacetDistribution.ulf.viewSet.getCameraPose(0)
				 .times(new Vector4(microfacetDistribution.ulf.proxy.getCentroid(), 1.0f))).length();
		
		program.setUniform("lightIntensityVirtual[" + lightIndex + "]", 
				controllerLightIntensity.times(lightDistance * lightDistance * microfacetDistribution.ulf.viewSet.getLightIntensity(0).y / (scale * scale)));
		program.setUniform("lightMatrixVirtual[" + lightIndex + "]", getLightProjection(lightIndex).times(lightMatrix));
		program.setUniform("virtualLightCount", Math.min(4, lightController.getLightCount()));
		
		return lightMatrix;
	}
	
	private Matrix4 setupLightForFidelity(Vector3 lightIntensity, Vector3 lightPos)
	{
		float lightDist = lightPos.distance(this.microfacetDistribution.ulf.proxy.getCentroid());
		
		float radius = (float)
			(new Matrix3(microfacetDistribution.ulf.viewSet.getCameraPose(0))
				.times(new Vector3(this.microfacetDistribution.ulf.proxy.getBoundingRadius()))
				.length() / Math.sqrt(3));
		
		Matrix4 lightProjection = Matrix4.perspective(2.0f * (float)Math.atan(radius / lightDist) /*1.5f*/, 1.0f, 
				lightDist - radius,
				lightDist + radius);
		
		Matrix4 lightMatrix = Matrix4.lookAt(lightPos, this.microfacetDistribution.ulf.proxy.getCentroid(), new Vector3(0.0f, 1.0f, 0.0f));
		
    	shadowProgram.setUniform("model_view", lightMatrix);
		shadowProgram.setUniform("projection", lightProjection);
		
		shadowFramebuffer.setDepthAttachment(shadowMaps.getLayerAsFramebufferAttachment(0));
		shadowFramebuffer.clearDepthBuffer();
		shadowRenderable.draw(PrimitiveMode.TRIANGLES, shadowFramebuffer);
		
		program.setUniform("lightPosVirtual[0]", lightPos);
		
		program.setUniform("lightIntensityVirtual[0]", lightIntensity);
		program.setUniform("lightMatrixVirtual[0]", lightProjection.times(lightMatrix));
		
		program.setUniform("virtualLightCount", 1);
		
		return lightMatrix;
	}

	@Override
	public void draw() 
	{
		try
		{
			setupForDraw();
			ulfRenderer.prepareForDefaultFBODraw();
			
			for (int modelInstance = 0; modelInstance < ulfRenderer.getModelInstanceCount(); modelInstance++)
			{
				Matrix4 envMapMatrix = null;
				
				for (int lightIndex = 0; lightIndex < lightController.getLightCount(); lightIndex++)
				{
					Matrix4 matrix = setupLight(lightIndex, modelInstance);
					
					if (lightIndex == 0)
					{
						envMapMatrix = matrix;
					}
				}
				
				ulfRenderer.setEnvironmentMatrix(envMapMatrix);
				ulfRenderer.drawInstance(modelInstance);
			}
			
			ulfRenderer.finishDefaultFBODraw();
			
			FramebufferSize windowSize = context.getDefaultFramebuffer().getSize();
			
			context.setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
			
			Matrix4 viewMatrix = ulfRenderer.getViewMatrix();
			
			if (this.ulfRenderer.settings().isRelightingEnabled() && this.ulfRenderer.settings().areVisibleLightsEnabled())
			{
				for (int i = 0; i < lightController.getLightCount(); i++)
				{
					if (lightController.getSelectedLightIndex() != i)
					{
						this.lightProgram.setUniform("color", lightController.getLightColor(i));
						
						Vector3 lightPosition = new Vector3(viewMatrix.times(this.getLightMatrix(i).quickInverse(0.001f)).getColumn(3));
						
						this.lightProgram.setUniform("model_view",
	
	//							modelView.times(this.getLightMatrix(i).quickInverse(0.001f)));
							Matrix4.translate(lightPosition)
								.times(Matrix4.scale((float)windowSize.height * -lightPosition.z / (16.0f * windowSize.width), -lightPosition.z / 16.0f, 1.0f)));
						this.lightProgram.setUniform("projection", ulfRenderer.getProjectionMatrix());
			    		this.lightProgram.setTexture("lightTexture", this.lightTexture);
						this.lightRenderable.draw(PrimitiveMode.TRIANGLE_FAN, context);
					}
				}
			}
			
			context.disableAlphaBlending();
		}
		catch(Exception e)
		{
			if (!suppressErrors)
			{
				e.printStackTrace();
				suppressErrors = true; // Prevent excessive errors
			}
		}
	}

	@Override
	public void cleanup() 
	{
		ulfRenderer.cleanup();
		if (microfacetDistribution != null)
		{
			microfacetDistribution.deleteOpenGLResources();
			microfacetDistribution = null;
		}
		
		if (shadowMaps != null)
		{
			shadowMaps.delete();
			shadowMaps = null;
		}
		
		if (shadowFramebuffer != null)
		{
			shadowFramebuffer.delete();
			shadowFramebuffer = null;
		}
		
		if (lightProgram != null)
		{
			lightProgram.delete();
		}
		
		if (lightVertices != null)
		{
			lightVertices.delete();
		}
		
		if (lightTexture != null)
		{
			lightTexture.delete();
		}
	}

	@Override
	public void setOnLoadCallback(IBRLoadingMonitor callback) 
	{
		this.callback = callback;
		ulfRenderer.setOnLoadCallback(callback);
	}
	
	@Override
	public VertexMesh getActiveProxy()
	{
		return this.microfacetDistribution.ulf.proxy;
	}
	
	@Override
	public ViewSet<ContextType> getActiveViewSet()
	{
		return this.microfacetDistribution.ulf.viewSet;
	}

	@Override
	public IBRSettings settings()
	{
		return ulfRenderer.settings();
	}

	@Override
	public boolean getHalfResolution() 
	{
		return ulfRenderer.getHalfResolution();
	}
	
	@Override
	public void setHalfResolution(boolean halfResEnabled) 
	{
		ulfRenderer.setHalfResolution(halfResEnabled);
	}

	@Override
	public boolean getMultisampling() 
	{
		return ulfRenderer.getMultisampling();
	}

	@Override
	public void setMultisampling(boolean multisamplingEnabled) 
	{
		ulfRenderer.setMultisampling(multisamplingEnabled);
	}

	@Override
	public void requestResample(int width, int height, File targetVSETFile, File exportPath) throws IOException 
	{
		ulfRenderer.requestResample(width, height, targetVSETFile, exportPath);
	}

	@Override
	public void requestFidelity(File exportPath) throws IOException 
	{
		ulfRenderer.requestFidelity(exportPath);
	}

	@Override
	public void requestBTF(int width, int height, File exportPath) throws IOException 
	{
		this.btfRequested = true;
		this.btfWidth = width;
		this.btfHeight = height;
		this.btfExportPath = exportPath;
	}
	
	private void exportBTF()
	{	
		try
        {
			Program<ContextType> btfProgram = context.getShaderProgramBuilder()
    				.addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
    				.addShader(ShaderType.FRAGMENT, new File("shaders/ibr/ibmfr.frag"))
    				.createProgram();
			
			FramebufferObject<ContextType> framebuffer = context.getFramebufferObjectBuilder(btfWidth, btfHeight)
					.addColorAttachment()
					.createFramebufferObject();
	    	
	    	Renderable<ContextType> renderable = context.createRenderable(btfProgram);
	    	renderable.addVertexBuffer("position", ulfRenderer.getLightField().positionBuffer);
	    	renderable.addVertexBuffer("texCoord", ulfRenderer.getLightField().texCoordBuffer);
	    	renderable.addVertexBuffer("normal", ulfRenderer.getLightField().normalBuffer);
	    	renderable.addVertexBuffer("tangent", ulfRenderer.getLightField().tangentBuffer);
	    	
	    	ulfRenderer.setupForDraw(btfProgram);
	    	this.setupForDraw(btfProgram);
	    	
	    	btfProgram.setUniform("useTSOverrides", true);
	    	
	    	////////////////////////////////
	    	
	    	// Backscattering
			for (int i = 1; i <= 179; i++)
			{
				double theta = i / 180.0f * Math.PI;
		    	btfProgram.setUniform("virtualLightCount", 1);
		    	btfProgram.setUniform("lightIntensityVirtual[0]", lightController.getLightColor(0));
		    	btfProgram.setUniform("lightDirTSOverride", new Vector3((float)Math.cos(theta), 0.0f, (float)Math.sin(theta)));
		    	btfProgram.setUniform("viewDirTSOverride", new Vector3((float)Math.cos(theta), 0.0f, (float)Math.sin(theta)));
	    	
//	    	// Joey's lab
//	    	for (int i = 1; i <= 90; i++)
//			{
//				double theta = i / 180.0f * Math.PI;
//		    	btfProgram.setUniform("virtualLightCount", 1);
//		    	btfProgram.setUniform("lightIntensityVirtual[0]", lightController.getLightColor(0));
////		    	btfProgram.setUniform("lightDirTSOverride", new Vector3(-(float)(Math.sin(theta)*Math.sqrt(0.5)), -(float)(Math.sin(theta)*Math.sqrt(0.5)), (float)Math.cos(theta)));
////		    	btfProgram.setUniform("viewDirTSOverride", new Vector3((float)(Math.cos(theta)*Math.sqrt(0.5)), (float)(Math.cos(theta)*Math.sqrt(0.5)), (float)Math.sin(theta)));
//		    	btfProgram.setUniform("lightDirTSOverride", new Vector3(-(float)Math.sin(theta), 0.0f, (float)Math.cos(theta)));
//		    	btfProgram.setUniform("viewDirTSOverride", new Vector3((float)Math.cos(theta), 0.0f, (float)Math.sin(theta)));
//		    	
	    	////////////////////////////////
				
		    	context.disableBackFaceCulling();
		    	
		    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
		    	
		    	File exportFile = new File(btfExportPath, String.format("%02d.png", i));
		    	exportFile.getParentFile().mkdirs();
		        framebuffer.saveColorBufferToFile(0, "PNG", exportFile);
		        
		        if (this.callback != null)
		        {
		        	this.callback.setProgress((double) i / (double) /*90*/180);
		        }
			}
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
	}

	@Override
	public void setProgram(Program<ContextType> program) 
	{
		this.program = program;
		ulfRenderer.setProgram(program);
		suppressErrors = false;
	}
	
	@Override
	public Texture2D<ContextType> getEnvironmentTexture()
	{
		return this.ulfRenderer.getEnvironmentTexture();
	}
	
	@Override
	public void setEnvironment(File environmentFile) throws IOException
	{
		this.ulfRenderer.setEnvironment(environmentFile);
	}
	
	@Override
	public String toString()
	{
		return this.ulfRenderer.toString();
	}

	@Override
	public void reloadHelperShaders() 
	{
		this.ulfRenderer.reloadHelperShaders();
		
		if (this.lightProgram != null)
    	{
			this.lightProgram.delete();
			this.lightProgram = null;
    	}
		
    	try
        {
    		this.lightProgram = context.getShaderProgramBuilder()
    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
    				.addShader(ShaderType.FRAGMENT, new File("shaders/ibr/light.frag"))
    				.createProgram();
    		this.lightRenderable = context.createRenderable(this.lightProgram);
    		this.lightRenderable.addVertexBuffer("position", lightVertices);
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
	}

	@Override
	public void setTransformationMatrices(List<Matrix4> matrices) 
	{
		this.ulfRenderer.setTransformationMatrices(matrices);
	}
}
