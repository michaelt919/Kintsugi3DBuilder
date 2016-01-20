package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;

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
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;

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
    private CameraController cameraController;
    private ULFLoadingMonitor callback;

    private boolean viewIndexCacheEnabled;
    private boolean halfResEnabled;
    FramebufferObject<ContextType> indexFBO;
    private Texture3D<ContextType> viewIndexCacheTexturesFront;
    private Texture3D<ContextType> viewIndexCacheTexturesBack;
    private Program<ContextType> simpleTexProgram;
    private Renderable<ContextType> simpleTexRenderable;
    
    private float targetFPS;
    private long lastFrame = 0;
    private int offset=0, stride=0;

    private boolean resampleRequested;
    private int resampleWidth, resampleHeight;
    private File resampleVSETFile;
    private File resampleExportPath;
    
    private boolean multisamplingEnabled = true;

    public ULFRenderer(ContextType context, Program<ContextType> program, Program<ContextType> viewIndexProgram, File cameraFile, File meshFile, ULFLoadOptions loadOptions, CameraController cameraController)
    {
    	this.context = context;
    	this.program = program;
    	this.viewIndexProgram = program;
    	this.cameraFile = cameraFile;
    	this.geometryFile = meshFile;
    	this.loadOptions = loadOptions;
    	this.cameraController = cameraController;
    	this.viewIndexCacheEnabled = false;
    	this.targetFPS = 30.0f;
    }
	
	public void setCameraController(CameraController cameraController)
	{
		this.cameraController = cameraController;
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
	    				.addShader(ShaderType.VERTEX, new File("shaders/ibr/ulr.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/ibr/ulr.frag"))
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
	    	
	    	this.mainRenderable = context.createRenderable(program);
	    	this.mainRenderable.addVertexBuffer("position", this.lightField.positionBuffer);
	    	
	    	if (this.lightField.normalBuffer != null)
	    	{
	    		this.mainRenderable.addVertexBuffer("normal", this.lightField.normalBuffer);
	    	}
	    	
	    	if (this.lightField.texCoordBuffer != null)
	    	{
	    		this.mainRenderable.addVertexBuffer("texCoord", this.lightField.texCoordBuffer);
	    	}
	    	
	    	if(viewIndexProgram != null)
	    	{
		    	this.indexRenderable = context.createRenderable(viewIndexProgram);
		    	this.indexRenderable.addVertexBuffer("position", this.lightField.positionBuffer);
		    	
		    	if (this.lightField.normalBuffer != null)
		    	{
		    		this.indexRenderable.addVertexBuffer("normal", this.lightField.normalBuffer);
		    	}
		    	
		    	if (this.lightField.texCoordBuffer != null)
		    	{
		    		this.indexRenderable.addVertexBuffer("texCoord", this.lightField.texCoordBuffer);
		    	}
	    	}
	    				
	    	this.simpleTexRenderable = context.createRenderable(simpleTexProgram);
	    	this.simpleTexRenderable.addVertexBuffer("position", context.createRectangle());
	    	
        	indexFBO = context.getFramebufferObjectBuilder(this.lightField.depthTextures.getWidth(), this.lightField.depthTextures.getHeight())
					.addEmptyColorAttachments(2)
					.createFramebufferObject();
			
			viewIndexCacheTexturesFront = context.get2DColorTextureArrayBuilder(this.lightField.depthTextures.getWidth(), this.lightField.depthTextures.getHeight(), 2)
					.setInternalFormat(ColorFormat.RGBA16I)
					.setMipmapsEnabled(false)
					.setLinearFilteringEnabled(false)
					.setMultisamples(1, true)
					.createTexture();
			
			viewIndexCacheTexturesBack = context.get2DColorTextureArrayBuilder(this.lightField.depthTextures.getWidth(), this.lightField.depthTextures.getHeight(), 2)
					.setInternalFormat(ColorFormat.RGBA16I)
					.setMipmapsEnabled(false)
					.setLinearFilteringEnabled(false)
					.setMultisamples(1, true)
					.createTexture();
			
			indexFBO.setColorAttachment(0, viewIndexCacheTexturesFront.getLayerAsFramebufferAttachment(0));
			indexFBO.setColorAttachment(1, viewIndexCacheTexturesFront.getLayerAsFramebufferAttachment(1));
			indexFBO.clearIntegerColorBuffer(0, -1, -1, -1, -1);
			indexFBO.clearIntegerColorBuffer(1, -1, -1, -1, -1);
			context.flush();
    		
			if (this.callback != null)
			{
				this.callback.loadingComplete();
			}
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
    	if (offset == stride)
    	{
    		offset = 0;
    		long now = new Date().getTime();
    		
    		if (lastFrame > 0)
    		{
	    		long elapsedTime = now-lastFrame;
	    		float fps = (float)stride/(float)elapsedTime*1000;
	    		stride = Math.max(1, Math.min((int)Math.ceil(stride*targetFPS/fps), lightField.viewSet.getCameraPoseCount()));
	    		//System.out.println("fps="+fps);
	    		//System.out.println("new stride="+stride);
    		}
    		else
    		{
    			stride = lightField.viewSet.getCameraPoseCount();
    		}
    		
    		lastFrame = now;
    	}
    	
    	if(multisamplingEnabled)
		{
			context.enableMultisampling();
		}
		else
		{
			context.disableMultisampling();			
		}
    	
    	Framebuffer<ContextType> framebuffer = context.getDefaultFramebuffer();
    	FramebufferSize size = framebuffer.getSize();
    			
    	this.mainRenderable.program().setTexture("imageTextures", lightField.viewSet.getTextures());
    	this.mainRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
    	this.mainRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
    	this.mainRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
    	if (lightField.viewSet.getLightPositionBuffer() != null && lightField.viewSet.getLightIntensityBuffer() != null && lightField.viewSet.getLightIndexBuffer() != null)
    	{
	    	this.mainRenderable.program().setUniformBuffer("LightPositions", lightField.viewSet.getLightPositionBuffer());
	    	this.mainRenderable.program().setUniformBuffer("LightIntensities", lightField.viewSet.getLightIntensityBuffer());
	    	this.mainRenderable.program().setUniformBuffer("LightIndices", lightField.viewSet.getLightIndexBuffer());
    	}
    	this.mainRenderable.program().setUniform("cameraPoseCount", lightField.viewSet.getCameraPoseCount());
    	this.mainRenderable.program().setUniform("infiniteLightSources", true /* TODO */);
    	if (lightField.depthTextures != null)
		{
    		this.mainRenderable.program().setTexture("depthTextures", lightField.depthTextures);
		}
    	
    	mainRenderable.program().setUniform("model_view", 
			cameraController.getViewMatrix()
				.times(Matrix4.translate(lightField.proxy.getCentroid().negated())) // Model
		);
    	
    	mainRenderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 8, (float)size.width / (float)size.height, 0.01f, 100.0f));
    	
    	mainRenderable.program().setUniform("gamma", this.lightField.settings.getGamma());
    	mainRenderable.program().setUniform("weightExponent", this.lightField.settings.getWeightExponent());
    	mainRenderable.program().setUniform("occlusionEnabled", this.lightField.depthTextures != null && this.lightField.settings.isOcclusionEnabled());
    	mainRenderable.program().setUniform("occlusionBias", this.lightField.settings.getOcclusionBias());
    	
    	mainRenderable.program().setTexture("luminanceMap", this.lightField.viewSet.getLuminanceMap());
    	
        FramebufferObject<ContextType> offscreenFBO;
        
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
        	// Update view indices
			
        	this.indexRenderable.program().setUniform("offset", offset);
        	this.indexRenderable.program().setUniform("stride", stride);
        	
        	this.indexRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
        	this.indexRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
        	this.indexRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
        	this.indexRenderable.program().setUniformBuffer("LightPositions", lightField.viewSet.getLightPositionBuffer());
        	this.indexRenderable.program().setUniformBuffer("LightIntensities", lightField.viewSet.getLightIntensityBuffer());
        	this.indexRenderable.program().setUniformBuffer("LightIndices", lightField.viewSet.getLightIndexBuffer());
        	this.indexRenderable.program().setUniform("cameraPoseCount", lightField.viewSet.getCameraPoseCount());
        	
        	indexRenderable.program().setUniform("model_view", 
    			 cameraController.getViewMatrix() // View
    			 	.times(Matrix4.translate(lightField.proxy.getCentroid().negated())) // Model
    		);
        	indexRenderable.program().setUniform("weightExponent", this.lightField.settings.getWeightExponent());
        	
        	indexFBO.setColorAttachment(0, viewIndexCacheTexturesBack.getLayerAsFramebufferAttachment(0));
			indexFBO.setColorAttachment(1, viewIndexCacheTexturesBack.getLayerAsFramebufferAttachment(1));

			viewIndexProgram.setTexture("viewIndexTextures", viewIndexCacheTexturesFront);
			
			indexFBO.clearIntegerColorBuffer(0, -1, -1, -1, -1);
			indexFBO.clearIntegerColorBuffer(1, -1, -1, -1, -1);
			indexRenderable.draw(PrimitiveMode.TRIANGLES, indexFBO);
			context.flush();
				
    		Texture3D<ContextType> tmp = viewIndexCacheTexturesFront;
        	viewIndexCacheTexturesFront = viewIndexCacheTexturesBack;
        	viewIndexCacheTexturesBack = tmp;
        	
			program.setTexture("viewIndexTextures", viewIndexCacheTexturesFront);
		}
    	
    	if(halfResEnabled) 
    	{
			// Do first pass at half resolution to off-screen buffer
			offscreenFBO = context.getFramebufferObjectBuilder(fboWidth, fboHeight)
					.addColorAttachment(new ColorAttachmentSpec(ColorFormat.RGB8)
						.setLinearFilteringEnabled(true))
					.addDepthAttachment(new DepthAttachmentSpec(32, false))
					.createFramebufferObject();
			
			offscreenFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
	    	offscreenFBO.clearDepthBuffer();
	        mainRenderable.draw(PrimitiveMode.TRIANGLES, offscreenFBO);
	        context.flush();
	        
	        // Second pass at full resolution to default framebuffer
	    	simpleTexRenderable.program().setTexture("tex", offscreenFBO.getColorAttachmentTexture(0));    	

			framebuffer.clearDepthBuffer();
	    	simpleTexRenderable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);

	    	context.flush();
	    	offscreenFBO.delete();
    	} 
    	else 
    	{
    		framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
    		framebuffer.clearDepthBuffer();
	        mainRenderable.draw(PrimitiveMode.TRIANGLES, framebuffer);  
	        context.flush();
    	}
    	
    	offset++;
    }
    
    @Override
    public void cleanup()
    {
    	lightField.deleteOpenGLResources();
    	
		if (indexFBO != null)
		{
			indexFBO.delete();
			indexFBO = null;
		}
		
    	if (viewIndexCacheTexturesFront != null)
    	{
    		viewIndexCacheTexturesFront.delete();
    		viewIndexCacheTexturesFront = null;
    	}
		
		if (viewIndexCacheTexturesBack != null)
    	{
			viewIndexCacheTexturesBack.delete();
			viewIndexCacheTexturesBack = null;
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
		this.multisamplingEnabled = multisamplingEnabled;
	}

	@Override
	public void setProgram(Program<ContextType> program) 
	{
		this.program = program;
		
		this.mainRenderable = context.createRenderable(program);
    	this.mainRenderable.addVertexBuffer("position", this.lightField.positionBuffer);
    	
    	if (this.lightField.normalBuffer != null)
    	{
    		this.mainRenderable.addVertexBuffer("normal", this.lightField.normalBuffer);
    	}
    	
    	if (this.lightField.texCoordBuffer != null)
    	{
    		this.mainRenderable.addVertexBuffer("texCoord", this.lightField.texCoordBuffer);
    	}
	}
	
	@Override
	public void setIndexProgram(Program<ContextType> program)
	{
		this.viewIndexProgram = program;
		
		this.indexRenderable = context.createRenderable(program);
    	this.indexRenderable.addVertexBuffer("position", this.lightField.positionBuffer);
    	
    	if (this.lightField.normalBuffer != null)
    	{
    		this.indexRenderable.addVertexBuffer("normal", this.lightField.normalBuffer);
    	}
    	
    	if (this.lightField.texCoordBuffer != null)
    	{
    		this.indexRenderable.addVertexBuffer("texCoord", this.lightField.texCoordBuffer);
    	}
	}
}
