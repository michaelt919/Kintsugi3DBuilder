package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector2;
import tetzlaff.gl.helpers.Vector3;

public class ULFRenderer<ContextType extends Context<ContextType>> implements ULFDrawable<ContextType>
{
    private Program<ContextType> program;
    private Program<ContextType> viewIndexProgram;
    
    private File cameraFile;
    private File geometryFile;
    private ULFLoadOptions loadOptions;
    private UnstructuredLightField<ContextType> lightField;
	private ContextType context;
    private Renderable<ContextType> mainRenderable;
    private Renderable<ContextType> indexRenderable;
    private Trackball trackball;
    private ULFLoadingMonitor callback;

    private boolean viewIndexCacheEnabled;
    private boolean halfResEnabled;
    private Texture3D<ContextType> viewIndexCacheTextures;
    private Program<ContextType> simpleTexProgram;
    private Renderable<ContextType> simpleTexRenderable;

    private boolean resampleRequested;
    private int resampleWidth, resampleHeight;
    private File resampleVSETFile;
    private File resampleExportPath;

    public ULFRenderer(ContextType context, Program<ContextType> program, Program<ContextType> viewIndexProgram, File cameraFile, File meshFile, ULFLoadOptions loadOptions, Trackball trackball)
    {
    	this.context = context;
    	this.program = program;
    	this.viewIndexProgram = program;
    	this.cameraFile = cameraFile;
    	this.geometryFile = meshFile;
    	this.loadOptions = loadOptions;
    	this.trackball = trackball;
    	this.viewIndexCacheEnabled = true;
    }
    
    @Override
    public void setOnLoadCallback(ULFLoadingMonitor callback)
    {
    	this.callback = callback;
    }
 
    @Override
    public void initialize() 
    {
    	if (this.program == null)
    	{
	    	try
	        {
	    		this.program = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File("shaders/ulr.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/ulr.frag"))
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
	    				.addShader(ShaderType.VERTEX, new File("shaders/texturerect.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/simpletexture.frag"))
	    				.createProgram();
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
    	}
    	
    	try 
    	{
    		if (this.cameraFile.getName().toUpperCase().endsWith(".XML"))
    		{
    			this.lightField = UnstructuredLightField.loadFromAgisoftXMLFile(this.cameraFile, this.geometryFile, this.loadOptions, this.context);
    		}
    		else
    		{
    			this.lightField = UnstructuredLightField.loadFromVSETFile(this.cameraFile, this.loadOptions, this.context);
    			if (this.geometryFile == null)
				{
    				this.geometryFile = lightField.viewSet.getGeometryFile();
				}
    		}
    		
			if (this.callback != null)
			{
				this.callback.loadingComplete();
			}
	    	
	    	this.mainRenderable = context.createRenderable(program);
	    	this.mainRenderable.addVertexBuffer("position", this.lightField.positionBuffer);
	    	this.mainRenderable.addVertexBuffer("normal", this.lightField.normalBuffer);
	    	this.mainRenderable.addVertexBuffer("texCoord", this.lightField.texCoordBuffer);
	    	
	    	if(viewIndexProgram != null)
	    	{
		    	this.indexRenderable = context.createRenderable(viewIndexProgram);
		    	this.indexRenderable.addVertexBuffer("position", this.lightField.positionBuffer);
		    	this.indexRenderable.addVertexBuffer("normal", this.lightField.normalBuffer);
		    	this.indexRenderable.addVertexBuffer("texCoord", this.lightField.texCoordBuffer);
	    	}
	    				
	    	this.simpleTexRenderable = context.createRenderable(simpleTexProgram);
	    	this.simpleTexRenderable.addVertexBuffer("position", context.createRectangle());
		} 
    	catch (IOException e) 
    	{
			e.printStackTrace();
		}
    }

	@Override
	public void update()
	{
    	if (this.resampleRequested)
    	{
    		try
    		{
				this.resample();
			} 
    		catch (Exception e) 
    		{
				e.printStackTrace();
			}
    		this.resampleRequested = false;
    		if (this.callback != null)
			{
				this.callback.loadingComplete();
			}
    	}
	}
    
    @Override
    public void draw()
    {
    	Framebuffer<ContextType> framebuffer = context.getDefaultFramebuffer();
    	FramebufferSize size = framebuffer.getSize();
    			
    	this.mainRenderable.program().setTexture("imageTextures", lightField.viewSet.getTextures());
    	this.mainRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
    	this.mainRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
    	this.mainRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
    	this.mainRenderable.program().setUniformBuffer("LightPositions", lightField.viewSet.getLightPositionBuffer());
    	this.mainRenderable.program().setUniformBuffer("LightIntensities", lightField.viewSet.getLightIntensityBuffer());
    	this.mainRenderable.program().setUniformBuffer("LightIndices", lightField.viewSet.getLightIndexBuffer());
    	this.mainRenderable.program().setUniform("cameraPoseCount", lightField.viewSet.getCameraPoseCount());
    	if (lightField.depthTextures != null)
		{
    		this.mainRenderable.program().setTexture("depthTextures", lightField.depthTextures);
		}
    	
    	mainRenderable.program().setUniform("model_view", 
			Matrix4.lookAt(
				new Vector3(0.0f, 0.0f, 5.0f / trackball.getScale()), 
				new Vector3(0.0f, 0.0f, 0.0f),
				new Vector3(0.0f, 1.0f, 0.0f)
			) // View
			.times(trackball.getRotationMatrix())
			.times(Matrix4.translate(lightField.proxy.getCentroid().negated())) // Model
		);
    	
    	mainRenderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 4, (float)size.width / (float)size.height, 0.01f, 100.0f));
    	
    	mainRenderable.program().setUniform("gamma", this.lightField.settings.getGamma());
    	mainRenderable.program().setUniform("weightExponent", this.lightField.settings.getWeightExponent());
    	mainRenderable.program().setUniform("occlusionEnabled", this.lightField.depthTextures != null && this.lightField.settings.isOcclusionEnabled());
    	mainRenderable.program().setUniform("occlusionBias", this.lightField.settings.getOcclusionBias());
    	
        FramebufferObject<ContextType> offscreenFBO;
        Texture3D<ContextType> nextViewIndexCacheTextures = null;
        
        int fboWidth, fboHeight;
		if (halfResEnabled)
		{
			fboWidth = size.width / 2;
			fboHeight = size.height / 2;
		}
		else
		{
			fboWidth = size.width;
			fboHeight = size.height;
		}
		
        if (viewIndexCacheEnabled && viewIndexProgram != null)
		{
        	this.indexRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
        	this.indexRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
        	this.indexRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
        	this.indexRenderable.program().setUniformBuffer("LightPositions", lightField.viewSet.getLightPositionBuffer());
        	this.indexRenderable.program().setUniformBuffer("LightIntensities", lightField.viewSet.getLightIntensityBuffer());
        	this.indexRenderable.program().setUniformBuffer("LightIndices", lightField.viewSet.getLightIndexBuffer());
        	this.indexRenderable.program().setUniform("cameraPoseCount", lightField.viewSet.getCameraPoseCount());
        	
        	indexRenderable.program().setUniform("model_view", 
    			Matrix4.lookAt(
    				new Vector3(0.0f, 0.0f, 5.0f / trackball.getScale()), 
    				new Vector3(0.0f, 0.0f, 0.0f),
    				new Vector3(0.0f, 1.0f, 0.0f)
    			) // View
    			.times(trackball.getRotationMatrix())
    			.times(Matrix4.translate(lightField.proxy.getCentroid().negated())) // Model
    		);
        	indexRenderable.program().setUniform("weightExponent", this.lightField.settings.getWeightExponent());
        	
        	FramebufferObject<ContextType> indexFBO = null;
        	try
        	{
	        	// Update view indices
	        	indexFBO = context.getFramebufferObjectBuilder(fboWidth, fboHeight)
						.addEmptyColorAttachments(2)
						.createFramebufferObject();
				
				nextViewIndexCacheTextures = context.get2DColorTextureArrayBuilder(fboWidth, fboHeight, 2)
						.setInternalFormat(ColorFormat.RGBA16I)
						.setMipmapsEnabled(false)
						.setLinearFilteringEnabled(false)
						.setMultisamples(1, true)
						.createTexture();
				
				
				if (viewIndexCacheTextures == null)
				{
					viewIndexCacheTextures = context.get2DColorTextureArrayBuilder(fboWidth, fboHeight, 2)
							.setInternalFormat(ColorFormat.RGBA16I)
							.setMipmapsEnabled(false)
							.setLinearFilteringEnabled(false)
							.setMultisamples(1, true)
							.createTexture();
					
					indexFBO.setColorAttachment(0, viewIndexCacheTextures.getLayerAsFramebufferAttachment(0));
					indexFBO.setColorAttachment(1, viewIndexCacheTextures.getLayerAsFramebufferAttachment(1));
					indexFBO.clearIntegerColorBuffer(0, -1, -1, -1, -1);
					indexFBO.clearIntegerColorBuffer(1, -1, -1, -1, -1);
					context.finish();
				}
				
				indexFBO.setColorAttachment(0, nextViewIndexCacheTextures.getLayerAsFramebufferAttachment(0));
				indexFBO.setColorAttachment(1, nextViewIndexCacheTextures.getLayerAsFramebufferAttachment(1));

				viewIndexProgram.setTexture("viewIndexTextures", viewIndexCacheTextures);
				
				indexFBO.clearIntegerColorBuffer(0, -1, -1, -1, -1);
				indexFBO.clearIntegerColorBuffer(1, -1, -1, -1, -1);
				indexRenderable.draw(PrimitiveMode.TRIANGLES, indexFBO);
				context.finish();
        	}
        	finally
        	{
        		if (indexFBO != null)
        		{
        			indexFBO.delete();
        		}
        		
        		if (viewIndexCacheTextures != null)
            	{
            		viewIndexCacheTextures.delete();
            	}
            	viewIndexCacheTextures = nextViewIndexCacheTextures;
        	}
        	
			program.setTexture("viewIndexTextures", viewIndexCacheTextures);
		}
    	
    	if(halfResEnabled) 
    	{
			// Do first pass at half resolution to off-screen buffer
			offscreenFBO = context.getFramebufferObjectBuilder(fboWidth, fboHeight)
					.addColorAttachment(new ColorAttachmentSpec(ColorFormat.RGB8)
						.setLinearFilteringEnabled(true)
						) //.setMultisamples(4, true)) // TODO why doesn't this work?
					.addDepthAttachment(new DepthAttachmentSpec(16, false)
						) //.setMultisamples(4, true))
					.createFramebufferObject();
			
			offscreenFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
	    	offscreenFBO.clearDepthBuffer();
	        mainRenderable.draw(PrimitiveMode.TRIANGLES, offscreenFBO);
	        context.finish();
	        
	        // Second pass at full resolution to default framebuffer
	    	simpleTexRenderable.program().setTexture("tex", offscreenFBO.getColorAttachmentTexture(0));    	

			framebuffer.clearDepthBuffer();
	    	simpleTexRenderable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);

	    	context.finish();
	    	offscreenFBO.delete();
    	} 
    	else 
    	{
    		framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
    		framebuffer.clearDepthBuffer();
	        mainRenderable.draw(PrimitiveMode.TRIANGLES, framebuffer);  
	        context.finish();
    	}
    }
    
    @Override
    public void cleanup()
    {
    	lightField.deleteOpenGLResources();
    	if (viewIndexCacheTextures != null)
    	{
    		viewIndexCacheTextures.delete();
    		viewIndexCacheTextures = null;
    	}
    }
    
    public File getVSETFile()
    {
    	return this.cameraFile;
    }
    
    public File getGeometryFile()
    {
    	return this.geometryFile;
    }
    
    public UnstructuredLightField<ContextType> getLightField()
    {
    	return this.lightField;
    }

	@Override
	public float getGamma() 
	{
		return this.lightField.settings.getGamma();
	}

	@Override
	public float getWeightExponent() 
	{
		return this.lightField.settings.getWeightExponent();
	}
	
	@Override
	public boolean isOcclusionEnabled() 
	{
		return this.lightField.settings.isOcclusionEnabled();
	}

	@Override
	public float getOcclusionBias() 
	{
		return this.lightField.settings.getOcclusionBias();
	}

	@Override
	public void setGamma(float gamma) 
	{
		this.lightField.settings.setGamma(gamma);
	}

	@Override
	public void setWeightExponent(float weightExponent) 
	{
		this.lightField.settings.setWeightExponent(weightExponent);
	}

	@Override
	public void setOcclusionEnabled(boolean occlusionEnabled) 
	{
		this.lightField.settings.setOcclusionEnabled(occlusionEnabled);
	}

	@Override
	public void setOcclusionBias(float occlusionBias) 
	{
		this.lightField.settings.setOcclusionBias(occlusionBias);
	}
	
	@Override
	public String toString()
	{
		return this.lightField.toString();
	}
	
	@Override
	public void requestResample(int width, int height, File targetVSETFile, File exportPath) throws IOException
	{
		this.resampleRequested = true;
		this.resampleWidth = width;
		this.resampleHeight = height;
		this.resampleVSETFile = targetVSETFile;
		this.resampleExportPath = exportPath;
	}
	
	private void resample() throws IOException
	{
		ViewSet<ContextType> targetViewSet = ViewSet.loadFromVSETFile(resampleVSETFile, new ViewSetImageOptions(null, false, false, false), context);
		FramebufferObject<ContextType> framebuffer = context.getFramebufferObjectBuilder(resampleWidth, resampleHeight).addColorAttachment().createFramebufferObject();
    	
    	this.mainRenderable.program().setTexture("imageTextures", lightField.viewSet.getTextures());
    	this.mainRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
    	this.mainRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
    	this.mainRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
    	this.mainRenderable.program().setUniform("cameraPoseCount", lightField.viewSet.getCameraPoseCount());
    	if (lightField.depthTextures != null)
		{
    		this.mainRenderable.program().setTexture("depthTextures", lightField.depthTextures);
		}
    	this.mainRenderable.program().setUniform("gamma", this.lightField.settings.getGamma());
    	this.mainRenderable.program().setUniform("weightExponent", this.lightField.settings.getWeightExponent());
    	this.mainRenderable.program().setUniform("occlusionEnabled", this.lightField.depthTextures != null && this.lightField.settings.isOcclusionEnabled());
    	this.mainRenderable.program().setUniform("occlusionBias", this.lightField.settings.getOcclusionBias());
    	
		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
		{
	    	mainRenderable.program().setUniform("model_view", targetViewSet.getCameraPose(i));
	    	mainRenderable.program().setUniform("projection", 
    			targetViewSet.getCameraProjection(targetViewSet.getCameraProjectionIndex(i))
    				.getProjectionMatrix(targetViewSet.getRecommendedNearPlane(), targetViewSet.getRecommendedFarPlane()));
	    	
	    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
	    	framebuffer.clearDepthBuffer();
	    	
	    	mainRenderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
	    	
	    	File exportFile = new File(resampleExportPath, targetViewSet.getImageFileName(i));
	    	exportFile.getParentFile().mkdirs();
	        framebuffer.saveColorBufferToFile(0, "PNG", exportFile);
	        
	        if (this.callback != null)
	        {
	        	this.callback.setProgress((double) i / (double) targetViewSet.getCameraPoseCount());
	        }
		}
		
		Files.copy(resampleVSETFile.toPath(), 
			new File(resampleExportPath, resampleVSETFile.getName()).toPath());
		Files.copy(lightField.viewSet.getGeometryFile().toPath(), 
			new File(resampleExportPath, lightField.viewSet.getGeometryFile().getName()).toPath());
	}

	@Override
	public void setHalfResolution(boolean halfResEnabled)
	{
		this.halfResEnabled = halfResEnabled;
	}

	@Override
	public boolean getHalfResolution()
	{
		return this.halfResEnabled;
	}

	@Override
	public void setViewIndexCacheEnabled(boolean viewIndexCacheEnabled)
	{
		this.viewIndexCacheEnabled = viewIndexCacheEnabled;
	}

	@Override
	public boolean isViewIndexCacheEnabled()
	{
		return this.viewIndexCacheEnabled;
	}

	@Override
	public void setMultisampling(boolean multisamplingEnabled)
	{
		context.makeContextCurrent();
		if(multisamplingEnabled)
		{
			context.enableMultisampling();
		}
		else
		{
			context.disableMultisampling();			
		}
	}

	@Override
	public void setProgram(Program<ContextType> program) 
	{
		this.program = program;
		
		this.mainRenderable = context.createRenderable(program);
    	this.mainRenderable.addVertexBuffer("position", this.lightField.positionBuffer);
    	this.mainRenderable.addVertexBuffer("normal", this.lightField.normalBuffer);
    	this.mainRenderable.addVertexBuffer("texCoord", this.lightField.texCoordBuffer);
	}
	
	@Override
	public void setIndexProgram(Program<ContextType> program)
	{
		this.viewIndexProgram = program;
		
		this.indexRenderable = context.createRenderable(program);
    	this.indexRenderable.addVertexBuffer("position", this.lightField.positionBuffer);
    	this.indexRenderable.addVertexBuffer("normal", this.lightField.normalBuffer);
    	this.indexRenderable.addVertexBuffer("texCoord", this.lightField.texCoordBuffer);
	}
}
