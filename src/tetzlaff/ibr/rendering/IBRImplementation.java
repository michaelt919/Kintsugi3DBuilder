package tetzlaff.ibr.rendering;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.AlphaBlendingFunction;
import tetzlaff.gl.AlphaBlendingFunction.Weight;
import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.ColorFormat.DataType;
import tetzlaff.gl.Context;
import tetzlaff.gl.Cubemap;
import tetzlaff.gl.CubemapFace;
import tetzlaff.gl.Drawable;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.TextureWrapMode;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.IBRSettings;
import tetzlaff.ibr.LoadingMonitor;
import tetzlaff.ibr.ViewSet;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.ReadonlyLightModel;
import tetzlaff.util.EnvironmentMap;

public class IBRImplementation<ContextType extends Context<ContextType>> implements IBRRenderable<ContextType>
{
	private ContextType context;
	private Program<ContextType> program;
	private Program<ContextType> shadowProgram;
	private ReadonlyLightModel lightModel;
	private LoadingMonitor callback;
	private boolean suppressErrors = false;
	private IBRSettings settings;

	private IBRResources.Builder<ContextType> resourceBuilder;
	private IBRResources<ContextType> resources;
	
	private Texture3D<ContextType> shadowMaps;
	private FramebufferObject<ContextType> shadowFramebuffer;
	private Drawable<ContextType> shadowDrawable;

	private Program<ContextType> lightProgram;
	private VertexBuffer<ContextType> lightVertices;
	private Texture2D<ContextType> lightTexture;
	private Drawable<ContextType> lightDrawable;
    
    private String id;
    private Drawable<ContextType> mainDrawable;
    private ReadonlyCameraModel cameraModel;

    private Vector3 clearColor;
    private boolean halfResEnabled;
    private Program<ContextType> simpleTexProgram;
    private Drawable<ContextType> simpleTexDrawable;
	private FramebufferObject<ContextType> offscreenFBO = null;
    
    private File newEnvironmentFile = null;
    private boolean environmentMapEnabled;
    private Cubemap<ContextType> environmentMap;
    private Matrix4 envMapMatrix = null;

    private Program<ContextType> environmentBackgroundProgram;
    private Drawable<ContextType> environmentBackgroundDrawable;
    
    private boolean multisamplingEnabled = false;
    
    private List<Matrix4> transformationMatrices;
    private Vector3 centroid;
    private float boundingRadius;
    
    private VertexGeometry referenceScene = null;
    private boolean referenceSceneChanged = false;
    private VertexBuffer<ContextType> refScenePositions = null;
    private VertexBuffer<ContextType> refSceneTexCoords = null;
    private VertexBuffer<ContextType> refSceneNormals = null;
    private Texture2D<ContextType> refSceneTexture = null;
	
	IBRImplementation(String id, ContextType context, Program<ContextType> program, 
			ReadonlyCameraModel cameraModel, ReadonlyLightModel lightModel,
			IBRResources.Builder<ContextType> resourceBuilder)
    {
    	this.id = id;
		this.context = context;
		this.program = program;
    	this.resourceBuilder = resourceBuilder;
    	this.cameraModel = cameraModel;
    	this.lightModel = lightModel;
    	
    	this.clearColor = new Vector3(0.0f);
    	this.transformationMatrices = new ArrayList<Matrix4>();
    	this.transformationMatrices.add(Matrix4.IDENTITY);
    	this.settings = new IBRSettings();
    }
	
	@Override
	public IBRResources<ContextType> getResources()
	{
		return this.resources;
	}

	@Override
	public void initialize() 
	{
		if (this.program == null)
    	{
	    	try
	        {
	    		this.program = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/relight/relight.frag"))
	    				.createProgram();
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
    	}

    	if (this.simpleTexProgram == null)
    	{
	    	try
	        {
	    		this.simpleTexProgram = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/common/texture.frag"))
	    				.createProgram();
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
    	}
		
    	if (this.environmentBackgroundProgram == null)
    	{
	    	try
	        {
	    		this.environmentBackgroundProgram = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/common/envbackgroundtexture.frag"))
	    				.createProgram();
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
    	}
    	
    	try 
    	{
    		this.resources = resourceBuilder.create();
    		
	    	this.mainDrawable = context.createDrawable(program);
	    	this.mainDrawable.addVertexBuffer("position", this.resources.positionBuffer);
	    	
	    	if (this.resources.normalBuffer != null)
	    	{
	    		this.mainDrawable.addVertexBuffer("normal", this.resources.normalBuffer);
	    	}
	    	
	    	if (this.resources.texCoordBuffer != null)
	    	{
	    		this.mainDrawable.addVertexBuffer("texCoord", this.resources.texCoordBuffer);
	    	}
	    	
	    	if (this.resources.tangentBuffer != null)
	    	{
	    		this.mainDrawable.addVertexBuffer("tangent", this.resources.tangentBuffer);
	    	}
	    				
	    	this.simpleTexDrawable = context.createDrawable(simpleTexProgram);
	    	this.simpleTexDrawable.addVertexBuffer("position", context.createRectangle());
			
	    	this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
	    	this.environmentBackgroundDrawable.addVertexBuffer("position", context.createRectangle());
	    	
			context.flush();

			if (this.callback != null)
			{
				this.callback.setMaximum(0.0); // make indeterminate
			}
		} 
    	catch (IOException e) 
    	{
			e.printStackTrace();
		}
	
		if (this.shadowProgram == null)
		{
	        try
	        {
	    		shadowProgram = context.getShaderProgramBuilder()
		    			.addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
		    			.addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"))
	    				.createProgram();
	    		
	    		shadowDrawable = context.createDrawable(shadowProgram);
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        	throw new IllegalStateException("The shader program could not be initialized.", e);
	        }
		}
		
		if (this.lightProgram == null)
    	{
	    	try
	        {
	    		this.lightProgram = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/relight/light.frag"))
	    				.createProgram();
	    		this.lightVertices = context.createRectangle();
	    		this.lightDrawable = context.createDrawable(this.lightProgram);
	    		this.lightDrawable.addVertexBuffer("position", lightVertices);
	    		
	    		NativeVectorBuffer lightTextureData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 1, 4096);
	    		
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
	    		
	    		this.lightTexture = context.build2DColorTextureFromBuffer(64, 64, lightTextureData)
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
		
		shadowDrawable.addVertexBuffer("position", resources.positionBuffer);

		shadowMaps = context.build2DDepthTextureArray(2048, 2048, lightModel.getLightCount()).createTexture();
		shadowFramebuffer = context.buildFramebufferObject(2048, 2048)
			.addDepthAttachment()
			.createFramebufferObject();
		
		this.updateCentroidAndRadius();
		
		// Make sure that everything is loaded onto the graphics card before announcing that loading is complete.
		this.draw(context.getDefaultFramebuffer());
		
		if (this.callback != null)
		{
			this.callback.loadingComplete();
		}
	}

	@Override
	public void update() 
	{
		this.updateCentroidAndRadius();
		
		if (this.newEnvironmentFile != null)
		{
			File environmentFile = this.newEnvironmentFile;
			this.newEnvironmentFile = null;
			
			try
			{
				System.out.println("Loading new environment texture.");
				
				// Use Michael Ludwig's code to convert to a cube map (supports either cross or panorama input)
				EnvironmentMap envMap = EnvironmentMap.createFromHDRFile(environmentFile);
				float[][] sides = envMap.getData();
				
//					// Uncomment to save the panorama as an image (i.e. for a figure in a paper)
//					float[] pixels = EnvironmentMap.toPanorama(envMap.getData(), envMap.getSide(), envMap.getSide() * 4, envMap.getSide() * 2);
//					BufferedImage img = new BufferedImage(envMap.getSide() * 4, envMap.getSide() * 2, BufferedImage.TYPE_3BYTE_BGR);
//					int k = 0;
//					
//					for (int j = 0; j < envMap.getSide() * 2; j++)
//					{
//						for (int i = 0; i < envMap.getSide() * 4; i++)
//						{
//							img.setRGB(i,  j, ((int)(Math.pow(pixels[3 * k + 0], 1.0 / 2.2) * 255) << 16)
//									| ((int)(Math.pow(pixels[3 * k + 1], 1.0 / 2.2) * 255) << 8) 
//									| (int)(Math.pow(pixels[3 * k + 2], 1.0 / 2.2) * 255));
//							k++;
//						}
//					}
//					ImageIO.write(img, "PNG", new File(environmentFile.getParentFile(), environmentFile.getName().replace("_zvc.hdr", "_IBRelight_pan.hdr")));
				
				Cubemap<ContextType> newEnvironmentTexture = 
						context.buildColorCubemap(envMap.getSide())
							.setInternalFormat(ColorFormat.RGB32F)
							.loadFace(CubemapFace.POSITIVE_X, NativeVectorBufferFactory.getInstance().createFromFloatArray(3, sides[EnvironmentMap.PX].length / 3, sides[EnvironmentMap.PX]))
							.loadFace(CubemapFace.NEGATIVE_X, NativeVectorBufferFactory.getInstance().createFromFloatArray(3, sides[EnvironmentMap.NX].length / 3, sides[EnvironmentMap.NX]))
							.loadFace(CubemapFace.POSITIVE_Y, NativeVectorBufferFactory.getInstance().createFromFloatArray(3, sides[EnvironmentMap.PY].length / 3, sides[EnvironmentMap.PY]))
							.loadFace(CubemapFace.NEGATIVE_Y, NativeVectorBufferFactory.getInstance().createFromFloatArray(3, sides[EnvironmentMap.NY].length / 3, sides[EnvironmentMap.NY]))
							.loadFace(CubemapFace.POSITIVE_Z, NativeVectorBufferFactory.getInstance().createFromFloatArray(3, sides[EnvironmentMap.PZ].length / 3, sides[EnvironmentMap.PZ]))
							.loadFace(CubemapFace.NEGATIVE_Z, NativeVectorBufferFactory.getInstance().createFromFloatArray(3, sides[EnvironmentMap.NZ].length / 3, sides[EnvironmentMap.NZ]))
							.setMipmapsEnabled(true)
							.setLinearFilteringEnabled(true)
							.createTexture();
				
				newEnvironmentTexture.setTextureWrap(TextureWrapMode.Repeat, TextureWrapMode.None);
	
				if (this.environmentMap != null)
				{
					this.environmentMap.close();
				}
				
				this.environmentMap = newEnvironmentTexture;
			}
			catch (Exception e) 
    		{
				e.printStackTrace();
			}
		}
		
		if (this.referenceSceneChanged && this.referenceScene != null)
		{
			this.referenceSceneChanged = false;
			
			try
			{
				System.out.println("Using new reference scene.");
				
				if (this.refScenePositions != null)
				{
					this.refScenePositions.close();
					this.refScenePositions = null;
				}
				
				if (this.refSceneTexCoords != null)
				{
					this.refSceneTexCoords.close();
					this.refSceneTexCoords = null;
				}
				
				if (this.refSceneNormals != null)
				{
					this.refSceneNormals.close();
					this.refSceneNormals = null;
				}
				
				if (this.refSceneTexture != null)
				{
					this.refSceneTexture.close();
					this.refSceneTexture = null;
				}
				
				this.refScenePositions = context.createVertexBuffer().setData(referenceScene.getVertices());
				this.refSceneTexCoords = context.createVertexBuffer().setData(referenceScene.getTexCoords());
				this.refSceneNormals = context.createVertexBuffer().setData(referenceScene.getNormals());
				this.refSceneTexture = context.build2DColorTextureFromFile(
						new File(referenceScene.getFilename().getParentFile(), referenceScene.getMaterial().getDiffuseMap().getMapName()), true)
					.setMipmapsEnabled(true)
					.setLinearFilteringEnabled(true)
					.createTexture();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void setupForDraw()
	{
		this.setupForDraw(this.mainDrawable.program());
	}
	
	private void setupForDraw(Program<ContextType> program)
	{
		this.resources.setupShaderProgram(program, this.settings().areTexturesEnabled());
    	
    	program.setUniform("renderGamma", this.settings.getGamma());
    	program.setUniform("weightExponent", this.settings.getWeightExponent());
    	program.setUniform("isotropyFactor", this.settings.getIsotropyFactor());
    	program.setUniform("occlusionEnabled", this.resources.depthTextures != null && this.settings.isOcclusionEnabled());
    	program.setUniform("occlusionBias", this.settings.getOcclusionBias());
    	program.setUniform("imageBasedRenderingEnabled", this.settings.isIBREnabled());
    	program.setUniform("relightingEnabled", this.settings.isRelightingEnabled());
    	program.setUniform("pbrGeometricAttenuationEnabled", this.settings.isPBRGeometricAttenuationEnabled());
    	program.setUniform("fresnelEnabled", this.settings.isFresnelEnabled());
    	program.setUniform("shadowsEnabled", this.settings.areShadowsEnabled());
    	
		program.setTexture("shadowMaps", shadowMaps);
		
		if (this.environmentMap == null || !lightModel.getEnvironmentMappingEnabled())
		{
			program.setUniform("useEnvironmentTexture", false);
			program.setTexture("environmentMap", null);
			this.environmentMapEnabled = false;
		}
		else
		{
			program.setUniform("useEnvironmentMap", true);
			program.setTexture("environmentMap", this.environmentMap);
			program.setUniform("environmentMipMapLevel", Math.max(0, Math.min(this.environmentMap.getMipmapLevelCount() - 2, 
					(int)Math.ceil(0.5 * 
						Math.log(6 * (double)this.environmentMap.getFaceSize() * 
								(double)this.environmentMap.getFaceSize() / (double)resources.viewSet.getCameraPoseCount() ) 
							/ Math.log(2.0)))));
			program.setUniform("diffuseEnvironmentMipMapLevel", this.environmentMap.getMipmapLevelCount() - 1);
			this.environmentMapEnabled = true;
		}
		
		program.setUniform("ambientColor", lightModel.getAmbientLightColor());
    	
    	this.clearColor = new Vector3(
    			(float)Math.pow(lightModel.getAmbientLightColor().x, 1.0 / this.settings().getGamma()),
    			(float)Math.pow(lightModel.getAmbientLightColor().y, 1.0 / this.settings().getGamma()),
    			(float)Math.pow(lightModel.getAmbientLightColor().z, 1.0 / this.settings().getGamma()));
	}
	
	private void updateCentroidAndRadius()
	{
		Vector4 sumPositions = new Vector4(0.0f);
    	this.boundingRadius = resources.geometry.getBoundingRadius();
    	
    	
    	this.centroid = resources.geometry.getCentroid();
    	
    	if (transformationMatrices != null)
    	{
    		for (Matrix4 m : transformationMatrices)
    		{
    			Vector4 position = m.times(resources.geometry.getCentroid().asPosition());
    			sumPositions = sumPositions.plus(position);
    		}
    		
    		this.centroid = sumPositions.getXYZ().dividedBy(sumPositions.w);
    		
    		for(Matrix4 m : transformationMatrices)
    		{
    			float distance = m.times(resources.geometry.getCentroid().asPosition()).getXYZ().distance(this.centroid);
    			this.boundingRadius = Math.max(this.boundingRadius, distance + resources.geometry.getBoundingRadius());
    		}
    	}
	}
	
	private float getScale()
	{
		 return resources.viewSet.getCameraPose(0)
				 .times(resources.geometry.getCentroid().asPosition())
			 .getXYZ().length()
			 * this.boundingRadius / this.resources.geometry.getBoundingRadius();
	}
	
	private Matrix4 getLightMatrix(int lightIndex)
	{
		float scale = getScale();
		return Matrix4.scale(scale)
			.times(lightModel.getLightMatrix(lightIndex))
			.times(Matrix4.scale(1.0f / scale))
			.times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().asMatrix4())
			.times(Matrix4.translate(this.centroid.negated()));
	}
	
	private Matrix4 getLightProjection(int lightIndex)
	{
		Matrix4 lightMatrix = getLightMatrix(lightIndex);
		
		float lightDist = lightMatrix.times(this.centroid.asPosition()).getXYZ().length();
		
		float radius = (float)
			(resources.viewSet.getCameraPose(0).getUpperLeft3x3()
				.times(new Vector3(this.boundingRadius))
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
		
		for (Matrix4 m : this.transformationMatrices)
		{
			shadowProgram.setUniform("model_view", getLightMatrix(lightIndex).times(m));
			shadowDrawable.draw(PrimitiveMode.TRIANGLES, shadowFramebuffer);
		}
	}
	
	private Matrix4 setupLight(int lightIndex, int modelInstance)
	{
		Matrix4 lightMatrix = 
			getLightMatrix(lightIndex).times(this.transformationMatrices.get(modelInstance));
		
		// lightMatrix can be hardcoded here (comment out previous line)
			
			// Contemporary gallery and stonewall
			//Matrix4.rotateY(16 * Math.PI / 16).times(Matrix4.rotateX(0 * Math.PI / 16))
				
			// Color studio 2:
			//Matrix4.rotateY(6 * Math.PI / 16).times(Matrix4.rotateX(0 * Math.PI / 16))
			
			// For the synthetic falcon example?
			//Matrix4.rotateY(5 * Math.PI / 4).times(Matrix4.rotateX(-Math.PI / 4))
				
			// Always end with this when hardcoding:
			//	.times(new Matrix4(new Matrix3(resources.viewSet.getCameraPose(0))));
		
		if (lightIndex == 0)
		{
			program.setUniform("envMapMatrix", lightMatrix);
		}
		Vector3 lightPos = lightMatrix.quickInverse(0.001f).times(Vector4.ORIGIN).getXYZ();
		
		program.setUniform("lightPosVirtual[" + lightIndex + "]", lightPos);
		
		Vector3 controllerLightIntensity = lightModel.getLightColor(lightIndex);
		float lightDistance = getLightMatrix(lightIndex).times(this.centroid.asPosition()).getXYZ().length();

		float scale = resources.viewSet.areLightSourcesInfinite() ? 1.0f :
				resources.viewSet.getCameraPose(0)
						.times(resources.geometry.getCentroid().asPosition())
					.getXYZ().length();
		
		program.setUniform("lightIntensityVirtual[" + lightIndex + "]", 
				controllerLightIntensity.times(lightDistance * lightDistance * resources.viewSet.getLightIntensity(0).y / (scale * scale)));
		program.setUniform("lightMatrixVirtual[" + lightIndex + "]", getLightProjection(lightIndex).times(lightMatrix));
		program.setUniform("virtualLightCount", Math.min(4, lightModel.getLightCount()));
		
		return lightMatrix;
	}
	
	private Matrix4 getViewMatrix()
	{
    	float scale = resources.viewSet.getCameraPose(0).times(resources.geometry.getCentroid().asPosition())
    			.getXYZ().length() 
    			* this.boundingRadius / resources.geometry.getBoundingRadius();
		
		return Matrix4.scale(scale)
    			.times(cameraModel.getLookMatrix())
    			.times(Matrix4.scale(1.0f / scale))
    			.times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().asMatrix4())
    			.times(Matrix4.translate(this.centroid.negated()));
	}
	
	private Matrix4 getModelViewMatrix(Matrix4 viewMatrix, int modelInstance)
	{
		return viewMatrix.times(transformationMatrices.get(modelInstance));
	}
	
	private Matrix4 getProjectionMatrix()
	{
    	FramebufferSize size = context.getDefaultFramebuffer().getSize();
		float scale = resources.viewSet.getCameraPose(0)
				.times(resources.geometry.getCentroid().asPosition())
			.getXYZ().length()
			* this.boundingRadius / resources.geometry.getBoundingRadius();
		
		return Matrix4.perspective(
    			//(float)(1.0),
    			resources.viewSet.getCameraProjection(
    					resources.viewSet.getCameraProjectionIndex(resources.viewSet.getPrimaryViewIndex()))
					.getVerticalFieldOfView(), 
    			(float)size.width / (float)size.height, 
    			0.01f * scale, 100.0f * scale);
	}

	private void drawReferenceScene(Program<ContextType> program)
	{
    	if (referenceScene != null && refScenePositions != null && refSceneNormals != null)
    	{
			Drawable<ContextType> drawable = context.createDrawable(program);
			drawable.addVertexBuffer("position", refScenePositions);
			drawable.addVertexBuffer("normal", refSceneNormals);
			
			if (refSceneTexture != null && refSceneTexCoords != null)
			{
				drawable.addVertexBuffer("texCoord", refSceneTexCoords);
				program.setTexture("diffuseMap", refSceneTexture);
				program.setUniform("useDiffuseTexture", true);
			}
			else
			{
				program.setUniform("useDiffuseTexture", false);
			}
			
			Matrix4 view = getViewMatrix();
    		program.setUniform("model_view", view);
			program.setUniform("viewPos", view.quickInverse(0.01f).getColumn(3).getXYZ());
        	
        	if(halfResEnabled) 
        	{
    			// Do first pass at half resolution to off-screen buffer
        		drawable.draw(PrimitiveMode.TRIANGLES, offscreenFBO);
        	} 
        	else 
        	{
        		drawable.draw(PrimitiveMode.TRIANGLES, context.getDefaultFramebuffer());  
        	}
    	}
	}
	
	@Override
	public void draw(Framebuffer<ContextType> framebuffer, Matrix4 view, Matrix4 projection) 
	{
		boolean customViewMatrix = (view != null);
    	
		try
		{
			if(multisamplingEnabled)
			{
				context.getState().enableMultisampling();
			}
			else
			{
				context.getState().disableMultisampling();			
			}
	    	
	    	context.getState().enableBackFaceCulling();
	    	
	    	if (!customViewMatrix)
	    	{
	    		view = this.getViewMatrix();
	    	}
	    	else
	    	{
	    		if (lightModel instanceof CameraBasedLightModel)
				{
			    	float scale = resources.viewSet.getCameraPose(0)
			    			.times(resources.geometry.getCentroid().asPosition())
		    			.getXYZ().length();
					
					((CameraBasedLightModel)lightModel).overrideCameraPose(
							Matrix4.scale(1.0f / scale)
								.times(view)
								.times(Matrix4.translate(resources.geometry.getCentroid()))
				    			.times(resources.viewSet.getCameraPose(0).transpose().getUpperLeft3x3().asMatrix4())
								.times(Matrix4.scale(scale)));
				}
	    	}
	    	
	    	if (projection == null)
	    	{
	    		projection = this.getProjectionMatrix();
	    	}
	    	
	    	FramebufferSize size = framebuffer.getSize();
	    	
	    	this.setupForDraw();
	    	
	    	mainDrawable.program().setUniform("projection", projection);
	    	
	    	if (environmentMap != null && environmentMapEnabled)
			{
				environmentBackgroundProgram.setUniform("useEnvironmentTexture", true);
				environmentBackgroundProgram.setTexture("env", environmentMap);
				environmentBackgroundProgram.setUniform("model_view", this.getViewMatrix());
				environmentBackgroundProgram.setUniform("projection", projection);
				environmentBackgroundProgram.setUniform("envMapMatrix", envMapMatrix == null ? Matrix4.IDENTITY : envMapMatrix);
				environmentBackgroundProgram.setUniform("envMapIntensity", this.clearColor);

				environmentBackgroundProgram.setUniform("gamma", 
						environmentMap.isInternalFormatCompressed() || 
						environmentMap.getInternalUncompressedColorFormat().dataType != DataType.FLOATING_POINT 
						? 1.0f : 2.2f);
			}
	    	
	        this.offscreenFBO = null;
	        
	        try
	        {
		        int fboWidth, fboHeight;
				if (halfResEnabled)
				{
					fboWidth = size.width / 2;
					fboHeight = size.height / 2;
					
					offscreenFBO = context.buildFramebufferObject(fboWidth, fboHeight)
							.addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB8)
								.setLinearFilteringEnabled(true))
							.addDepthAttachment(DepthAttachmentSpec.createFixedPointWithPrecision(32))
							.createFramebufferObject();
					
					offscreenFBO.clearColorBuffer(0, clearColor.x, clearColor.y, clearColor.z, 1.0f);
			    	offscreenFBO.clearDepthBuffer();
		    		
		    		if (environmentMap != null && environmentMapEnabled)
		    		{
		    			context.getState().disableDepthTest();
		    			this.environmentBackgroundDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
		    			context.getState().enableDepthTest();
		    		}
				}
				else
				{
					fboWidth = size.width;
					fboHeight = size.height;
					
					framebuffer.clearColorBuffer(0, clearColor.x, clearColor.y, clearColor.z, 1.0f);
		    		framebuffer.clearDepthBuffer();
		    		
		    		if (environmentMap != null && environmentMapEnabled)
		    		{
		    			context.getState().disableDepthTest();
		    			this.environmentBackgroundDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
		    			context.getState().enableDepthTest();
		    		}
				}
				
				for (int lightIndex = 0; lightIndex < lightModel.getLightCount(); lightIndex++)
				{
					generateShadowMaps(lightIndex);
				}
	
				this.program.setUniform("imageBasedRenderingEnabled", false);
				this.drawReferenceScene(this.program);
				this.program.setUniform("imageBasedRenderingEnabled", this.settings.isIBREnabled());
				setupForDraw(); // changed anything changed when drawing the reference scene.
				
				for (int modelInstance = 0; modelInstance < transformationMatrices.size(); modelInstance++)
				{
					this.envMapMatrix = null;
					
					for (int lightIndex = 0; lightIndex < lightModel.getLightCount(); lightIndex++)
					{
						Matrix4 matrix = setupLight(lightIndex, modelInstance);
						
						if (lightIndex == 0)
						{
							this.envMapMatrix = matrix;
						}
					}
					
					// Draw instance
					Matrix4 modelView = getModelViewMatrix(view, modelInstance);
					this.program.setUniform("model_view", modelView);
					this.program.setUniform("viewPos", modelView.quickInverse(0.01f).getColumn(3).getXYZ());
			    	
			    	if(halfResEnabled) 
			    	{
						// Do first pass at half resolution to off-screen buffer
				        mainDrawable.draw(PrimitiveMode.TRIANGLES, offscreenFBO);
			    	} 
			    	else 
			    	{
				        mainDrawable.draw(PrimitiveMode.TRIANGLES, context.getDefaultFramebuffer());  
			    	}
				}
				
				// Finish drawing
				if (halfResEnabled)
				{
		    		context.flush();
			        
			        // Second pass at full resolution to default framebuffer
			    	simpleTexDrawable.program().setTexture("tex", offscreenFBO.getColorAttachmentTexture(0));    	
	
		    		framebuffer.clearDepthBuffer();
			    	simpleTexDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
			    	
			    	context.flush();
		    		offscreenFBO.close();
				}
				else
				{
			    	context.flush();
				}
			}
	        finally
	        {
	        	if (this.offscreenFBO != null)
	        	{
	        		this.offscreenFBO.close();
	        		this.offscreenFBO = null;
	        	}
	        }
			
			FramebufferSize windowSize = context.getDefaultFramebuffer().getSize();
			
			context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
			
			Matrix4 viewMatrix = this.getViewMatrix();
			
			if (this.settings().isRelightingEnabled() && this.settings().areVisibleLightsEnabled())
			{
				for (int i = 0; i < lightModel.getLightCount(); i++)
				{
					if (lightModel.isLightVisualizationEnabled(i))
					{
						this.lightProgram.setUniform("color", lightModel.getLightColor(i));
						
						Vector3 lightPosition = viewMatrix.times(this.getLightMatrix(i).quickInverse(0.001f)).getColumn(3).getXYZ();
						
						this.lightProgram.setUniform("model_view",
	
	//							modelView.times(this.getLightMatrix(i).quickInverse(0.001f)));
							Matrix4.translate(lightPosition)
								.times(Matrix4.scale((float)windowSize.height * -lightPosition.z / (16.0f * windowSize.width), -lightPosition.z / 16.0f, 1.0f)));
						this.lightProgram.setUniform("projection", this.getProjectionMatrix());
			    		this.lightProgram.setTexture("lightTexture", this.lightTexture);
						this.lightDrawable.draw(PrimitiveMode.TRIANGLE_FAN, context);
					}
				}
			}
			
			context.getState().disableAlphaBlending();
		}
		catch(Exception e)
		{
			if (!suppressErrors)
			{
				e.printStackTrace();
				suppressErrors = true; // Prevent excessive errors
			}
		}
		finally
		{
			if (customViewMatrix && lightModel instanceof CameraBasedLightModel)
			{
				((CameraBasedLightModel)lightModel).removeCameraPoseOverride();
			}
		}
	}

	@Override
	public void close() 
	{
		resources.close();
    	
		if (this.refScenePositions != null)
		{
			this.refScenePositions.close();
			this.refScenePositions = null;
		}
		
		if (this.refSceneTexCoords != null)
		{
			this.refSceneTexCoords.close();
			this.refSceneTexCoords = null;
		}
		
		if (this.refSceneNormals != null)
		{
			this.refSceneNormals.close();
			this.refSceneNormals = null;
		}
		
		if (this.refSceneTexture != null)
		{
			this.refSceneTexture.close();
			this.refSceneTexture = null;
		}
		
		if (this.environmentBackgroundProgram != null)
		{
			this.environmentBackgroundProgram.close();
			this.environmentBackgroundProgram = null;
		}
		
		if (this.environmentMap != null)
		{
			this.environmentMap.close();
			this.environmentMap = null;
		}
		
		// New code
		if (resources != null)
		{
			resources.close();
			resources = null;
		}
		
		if (shadowMaps != null)
		{
			shadowMaps.close();
			shadowMaps = null;
		}
		
		if (shadowFramebuffer != null)
		{
			shadowFramebuffer.close();
			shadowFramebuffer = null;
		}
		
		if (lightProgram != null)
		{
			lightProgram.close();
		}
		
		if (lightVertices != null)
		{
			lightVertices.close();
		}
		
		if (lightTexture != null)
		{
			lightTexture.close();
		}
	}
	
	@Override
	public void setOnLoadCallback(LoadingMonitor callback) 
	{
		this.callback = callback;
	}
	
	@Override
	public VertexGeometry getActiveProxy()
	{
		return this.resources.geometry;
	}
	
	@Override
	public ViewSet getActiveViewSet()
	{
		return this.resources.viewSet;
	}

	@Override
	public IBRSettings settings()
	{
		return this.settings;
	}

	@Override
	public boolean getHalfResolution()
	{
		return this.halfResEnabled;
	}
	
	@Override
	public void setHalfResolution(boolean halfResEnabled) 
	{
		this.halfResEnabled = halfResEnabled;
	}

	@Override
	public boolean getMultisampling() 
	{
		return this.multisamplingEnabled;
	}

	@Override
	public void setMultisampling(boolean multisamplingEnabled) 
	{
		this.multisamplingEnabled = multisamplingEnabled;
	}

	
	
	@Override
	public void setProgram(Program<ContextType> program) 
	{
		this.program = program;
		
		this.mainDrawable = context.createDrawable(program);
    	this.mainDrawable.addVertexBuffer("position", this.resources.positionBuffer);
    	
    	if (this.resources.normalBuffer != null)
    	{
    		this.mainDrawable.addVertexBuffer("normal", this.resources.normalBuffer);
    	}
    	
    	if (this.resources.texCoordBuffer != null)
    	{
    		this.mainDrawable.addVertexBuffer("texCoord", this.resources.texCoordBuffer);
    	}
    	
    	if (this.resources.texCoordBuffer != null)
    	{
    		this.mainDrawable.addVertexBuffer("tangent", this.resources.tangentBuffer);
    	}
    	
		suppressErrors = false;
	}
	
	@Override
	public void setEnvironment(File environmentFile)
	{
		if (environmentFile != null && environmentFile.exists())
		{
			this.newEnvironmentFile = environmentFile;
		}
	}
	
	@Override
	public String toString()
	{
		return this.id.length() > 32 
				? "..." + this.id.substring(this.id.length()-31, this.id.length()) 
				: this.id;
	}

	@Override
	public void reloadHelperShaders() 
	{
		try
        {
    		Program<ContextType> newProgram = context.getShaderProgramBuilder()
    				.addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
    				.addShader(ShaderType.FRAGMENT, new File("shaders/common/envbackgroundtexture.frag"))
    				.createProgram();

    		if (this.environmentBackgroundProgram != null)
    		{
    			this.environmentBackgroundProgram.close();
    		}
    		
    		this.environmentBackgroundProgram = newProgram;
	    	this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
	    	this.environmentBackgroundDrawable.addVertexBuffer("position", context.createRectangle());

	    	
	    	newProgram = context.getShaderProgramBuilder()
    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
    				.addShader(ShaderType.FRAGMENT, new File("shaders/relight/light.frag"))
    				.createProgram();
	    	
			if (this.lightProgram != null)
	    	{
				this.lightProgram.close();
	    	}
    		
    		this.lightProgram = newProgram;
    		this.lightDrawable = context.createDrawable(this.lightProgram);
    		this.lightDrawable.addVertexBuffer("position", lightVertices);
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
	}

	@Override
	public void setTransformationMatrices(List<Matrix4> matrices) 
	{
		if (matrices != null)
		{
			this.transformationMatrices = matrices;
		}
	}
	
	@Override
	public void setReferenceScene(VertexGeometry scene)
	{
		this.referenceScene = scene;
		this.referenceSceneChanged = true;
	}
}
