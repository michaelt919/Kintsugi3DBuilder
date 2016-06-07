package tetzlaff.imagebasedmicrofacet;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.LightController;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.ulf.ULFDrawable;
import tetzlaff.ulf.ULFLoadOptions;
import tetzlaff.ulf.ULFLoadingMonitor;
import tetzlaff.ulf.ULFRenderer;
import tetzlaff.ulf.ViewSet;

public class ImageBasedMicrofacetRenderer<ContextType extends Context<ContextType>> implements ULFDrawable<ContextType>
{
	private ContextType context;
	private Program<ContextType> program;
	private Program<ContextType> indexProgram;
	private Program<ContextType> shadowProgram;
	private ULFRenderer<ContextType> ulfRenderer;
	private SampledMicrofacetField<ContextType> microfacetField;
	private LightController lightController;
	private ULFLoadingMonitor callback;
	private boolean suppressErrors = false;
	
	Texture3D<ContextType> shadowMaps;
	FramebufferObject<ContextType> shadowFramebuffer;
	Renderable<ContextType> shadowRenderable;
	
	public ImageBasedMicrofacetRenderer(ContextType context, Program<ContextType> program, Program<ContextType> indexProgram, Program<ContextType> shadowProgram, 
			File xmlFile, File meshFile, ULFLoadOptions loadOptions, CameraController cameraController, LightController lightController)
    {
		this.context = context;
		
		this.program = program;
		this.indexProgram = indexProgram;
		this.shadowProgram = shadowProgram;

    	this.lightController = lightController;
    	
    	this.ulfRenderer = new ULFRenderer<ContextType>(context, program, indexProgram, xmlFile, meshFile, loadOptions, cameraController);
    }

	@Override
	public void initialize() 
	{
		ulfRenderer.initialize();
		
		ulfRenderer.setResampleSetupCallback((modelView) ->
		{
			setupForDraw();
			
			if (lightController instanceof OverrideableLightController)
			{
		    	float scale = new Vector3(microfacetField.ulf.viewSet.getCameraPose(0)
		    			.times(new Vector4(microfacetField.ulf.proxy.getCentroid(), 1.0f))).length();
				
				((OverrideableLightController)lightController).overrideCameraPose(
						Matrix4.scale(1.0f / scale)
							.times(modelView)
							.times(Matrix4.translate(microfacetField.ulf.proxy.getCentroid()))
			    			.times(new Matrix4(new Matrix3(microfacetField.ulf.viewSet.getCameraPose(0).transpose())))
							.times(Matrix4.scale(scale)));
			}
			
			for (int i = 0; i < lightController.getLightCount(); i++)
			{
				setupLight(i);
			}
		});
		
		ulfRenderer.setResampleCompleteCallback(() -> 
		{
			if (lightController instanceof OverrideableLightController)
			{
				((OverrideableLightController)lightController).removeCameraPoseOverride();
			}
		});
		
		try
		{
			microfacetField = new SampledMicrofacetField<ContextType>(
					ulfRenderer.getLightField(), 
					new File(new File(ulfRenderer.getGeometryFile().getParentFile(), "textures"), "diffuse.png"),
					new File(new File(ulfRenderer.getGeometryFile().getParentFile(), "textures"), "normal.png"),
					new File(new File(ulfRenderer.getGeometryFile().getParentFile(), "textures"), "specular.png"),
					new File(new File(ulfRenderer.getGeometryFile().getParentFile(), "textures"), "roughness.png"), 
					context);
			
			shadowMaps = context.get2DDepthTextureArrayBuilder(2048, 2048, lightController.getLightCount()).createTexture();

			shadowFramebuffer = context.getFramebufferObjectBuilder(2048, 2048)
				.addDepthAttachment()
				.createFramebufferObject();
			
			shadowRenderable = context.createRenderable(shadowProgram);
			shadowRenderable.addVertexBuffer("position", microfacetField.ulf.positionBuffer);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		if (callback != null)
		{
			callback.loadingComplete();
		}
	}

	@Override
	public void update() 
	{
		ulfRenderer.update(); // Resample requests handled here
		
		if (callback != null)
		{
			callback.loadingComplete();
		}
	}
	
	private void setupForDraw()
	{
		if (microfacetField.normalTexture == null)
		{
			program.setUniform("useNormalTexture", false);
			program.setTexture("normalMap", null);
		}
		else
		{
			program.setUniform("useNormalTexture", true);
			program.setTexture("normalMap", microfacetField.normalTexture);
		}
		
		if (microfacetField.diffuseTexture == null)
		{
			program.setUniform("useDiffuseTexture", false);
			program.setTexture("diffuseMap", null);
		}
		else
		{
			program.setUniform("useDiffuseTexture", true);
			program.setTexture("diffuseMap", microfacetField.diffuseTexture);
		}
		
		if (microfacetField.specularTexture == null)
		{
			program.setUniform("useSpecularTexture", false);
			program.setTexture("specularMap", null);
		}
		else
		{
			program.setUniform("useSpecularTexture", true);
			program.setTexture("specularMap", microfacetField.specularTexture);
		}
		
		if (microfacetField.roughnessTexture == null)
		{
			program.setUniform("useRoughnessTexture", false);
			program.setTexture("roughnessMap", null);
		}
		else
		{
			program.setUniform("useRoughnessTexture", true);
			program.setTexture("roughnessMap", microfacetField.roughnessTexture);
		}
		
		if (microfacetField.ulf.viewSet.getLuminanceMap() == null)
		{
			program.setUniform("useLuminanceMap", false);
			program.setTexture("luminanceMap", null);
		}
		else
		{
			program.setUniform("useLuminanceMap", true);
			program.setTexture("luminanceMap", microfacetField.ulf.viewSet.getLuminanceMap());
		}
		
		if (microfacetField.ulf.viewSet.getInverseLuminanceMap() == null)
		{
			program.setUniform("useInverseLuminanceMap", false);
			program.setTexture("inverseLuminanceMap", null);
		}
		else
		{
			program.setUniform("useInverseLuminanceMap", true);
			program.setTexture("inverseLuminanceMap", microfacetField.ulf.viewSet.getInverseLuminanceMap());
		}
		
		float gamma = 2.2f;
    	Vector3 ambientColor = new Vector3(0.0f, 0.0f, 0.0f);
		program.setUniform("ambientColor", ambientColor);
    	
    	Vector3 clearColor = new Vector3(
    			(float)Math.pow(ambientColor.x, 1.0 / gamma),
    			(float)Math.pow(ambientColor.y, 1.0 / gamma),
    			(float)Math.pow(ambientColor.z, 1.0 / gamma));
    	ulfRenderer.setClearColor(clearColor);
    	
		program.setUniform("infiniteLightSources", false);
		program.setTexture("shadowMaps", shadowMaps);
	}
	
	private void setupLight(int lightIndex)
	{
    	float scale = new Vector3(microfacetField.ulf.viewSet.getCameraPose(0).times(new Vector4(microfacetField.ulf.proxy.getCentroid(), 1.0f))).length();

		Matrix4 lightMatrix = Matrix4.scale(scale)
			.times(lightController.getLightMatrix(lightIndex))
			.times(Matrix4.scale(1.0f / scale))
			.times(new Matrix4(new Matrix3(microfacetField.ulf.viewSet.getCameraPose(0))))
			.times(Matrix4.translate(microfacetField.ulf.proxy.getCentroid().negated()));
		
		float lightDist = new Vector3(lightMatrix.times(new Vector4(this.microfacetField.ulf.proxy.getCentroid(), 1.0f))).length();
		
		float radius = this.microfacetField.ulf.proxy.getBoundingRadius();
		
		Matrix4 lightProjection = Matrix4.perspective(2.0f * (float)Math.atan(radius / lightDist), 1.0f, 
				lightDist - radius,
				lightDist + radius);
		
		shadowProgram.setUniform("model_view", lightMatrix);
		shadowProgram.setUniform("projection", lightProjection);
		
		shadowFramebuffer.setDepthAttachment(shadowMaps.getLayerAsFramebufferAttachment(lightIndex));
		shadowFramebuffer.clearDepthBuffer();
		shadowRenderable.draw(PrimitiveMode.TRIANGLES, shadowFramebuffer);
		
		Vector3 lightPos = new Vector3(lightMatrix.quickInverse(0.001f).times(new Vector4(0.0f, 0.0f, 0.0f, 1.0f)));
		
		program.setUniform("lightPosVirtual[" + lightIndex + "]", lightPos);
		
		Vector3 controllerLightIntensity = lightController.getLightColor(lightIndex);
		float lightDistance = new Vector3(lightMatrix.times(new Vector4(microfacetField.ulf.proxy.getCentroid(), 1.0f))).length();
		
		program.setUniform("lightIntensityVirtual[" + lightIndex + "]", 
				controllerLightIntensity.times(lightDistance * lightDistance * microfacetField.ulf.viewSet.getLightIntensity(0).y / (scale * scale)));
		program.setUniform("lightMatrixVirtual[" + lightIndex + "]", lightProjection.times(lightMatrix));
		program.setUniform("virtualLightCount", Math.min(4, lightController.getLightCount()));
		
		if (microfacetField.shadowMatrixBuffer == null || microfacetField.shadowTextures == null)
		{
			program.setUniform("shadowTestingEnabled", false);
		}
		else
		{
			program.setUniform("shadowTestingEnabled", true);
			program.setUniformBuffer("ShadowMatrices", microfacetField.shadowMatrixBuffer);
			program.setTexture("shadowImages", microfacetField.shadowTextures);
		}
	}

	@Override
	public void draw() 
	{
		try
		{
			setupForDraw();
			
			for (int i = 0; i < lightController.getLightCount(); i++)
			{
				setupLight(i);
			}
			
			if (indexProgram != null)
			{
				if (microfacetField.normalTexture == null)
				{
					indexProgram.setUniform("useNormalTexture", false);
				}
				else
				{
					indexProgram.setUniform("useNormalTexture", true);
					indexProgram.setTexture("normalMap", microfacetField.normalTexture);
				}
	
				// TODO multiple lights
//				indexProgram.setUniform("lightPos", 
//						new Vector3(trackballs.get(0).getTrackballMatrix().getColumn(2))
//							.times(trackballs.get(0).getScale() / trackballs.get(0).getScale())
//							.plus(ulfRenderer.getLightField().proxy.getCentroid()));
//				indexProgram.setUniform("lightIntensity", new Vector3(1.0f, 1.0f, 1.0f));
			}
			
			ulfRenderer.draw();
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
		if (microfacetField != null)
		{
			microfacetField.deleteOpenGLResources();
			microfacetField = null;
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
	}

	@Override
	public void setOnLoadCallback(ULFLoadingMonitor callback) 
	{
		this.callback = callback;
	}
	
	@Override
	public VertexMesh getActiveProxy()
	{
		return this.microfacetField.ulf.proxy;
	}
	
	@Override
	public ViewSet<ContextType> getActiveViewSet()
	{
		return this.microfacetField.ulf.viewSet;
	}

	@Override
	public float getGamma() 
	{
		return ulfRenderer.getGamma();
	}

	@Override
	public float getWeightExponent() 
	{
		return ulfRenderer.getWeightExponent();
	}

	@Override
	public boolean isOcclusionEnabled() 
	{
		return ulfRenderer.isOcclusionEnabled();
	}

	@Override
	public float getOcclusionBias() 
	{
		return ulfRenderer.getOcclusionBias();
	}

	@Override
	public boolean getHalfResolution() 
	{
		return ulfRenderer.getHalfResolution();
	}

	@Override
	public void setGamma(float gamma) 
	{
		ulfRenderer.setGamma(gamma);
	}

	@Override
	public void setWeightExponent(float weightExponent)
	{
		ulfRenderer.setWeightExponent(weightExponent);
	}

	@Override
	public void setOcclusionEnabled(boolean occlusionEnabled) 
	{
		ulfRenderer.setOcclusionEnabled(occlusionEnabled);
	}

	@Override
	public void setOcclusionBias(float occlusionBias) 
	{
		ulfRenderer.setOcclusionBias(occlusionBias);
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
	public boolean isViewIndexCacheEnabled() 
	{
		return ulfRenderer.isViewIndexCacheEnabled();
	}

	@Override
	public void setViewIndexCacheEnabled(boolean viewIndexCacheEnabled) 
	{
		ulfRenderer.setViewIndexCacheEnabled(viewIndexCacheEnabled);
	}

	@Override
	public void requestResample(int width, int height, File targetVSETFile, File exportPath) throws IOException 
	{
		ulfRenderer.requestResample(width, height, targetVSETFile, exportPath);
	}

	@Override
	public void setProgram(Program<ContextType> program) 
	{
		this.program = program;
		ulfRenderer.setProgram(program);
		suppressErrors = false;
	}

	@Override
	public void setIndexProgram(Program<ContextType> program) 
	{
		this.indexProgram = program;
		ulfRenderer.setIndexProgram(program);
		suppressErrors = false;
	}
}
