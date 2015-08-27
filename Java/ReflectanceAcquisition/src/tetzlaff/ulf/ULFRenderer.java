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
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;

public class ULFRenderer<ContextType extends Context<ContextType>> implements ULFDrawable
{
    private Program<ContextType> program;
    
    private File cameraFile;
    private File meshFile;
    private ULFLoadOptions loadOptions;
    private UnstructuredLightField<ContextType> lightField;
	private ContextType context;
    private Renderable<ContextType> renderable;
    private Trackball trackball;
    private ULFLoadingMonitor callback;

    private boolean halfResEnabled;
    private FramebufferObject<ContextType> halfResFBO;
    private Program<ContextType> simpleTexProgram;
    private Renderable<ContextType> simpleTexRenderable;

    private boolean resampleRequested;
    private int resampleWidth, resampleHeight;
    private File resampleVSETFile;
    private File resampleExportPath;

    public ULFRenderer(ContextType context, Program<ContextType> program, File vsetFile, ULFLoadOptions loadOptions, Trackball trackball)
    {
    	this.context = context;
    	this.program = program;
    	this.cameraFile = vsetFile;
    	this.loadOptions = loadOptions;
    	this.trackball = trackball;
    }

    public ULFRenderer(ContextType context, Program<ContextType> program, File xmlFile, File meshFile, ULFLoadOptions loadOptions, Trackball trackball)
    {
    	this.context = context;
    	this.program = program;
    	this.cameraFile = xmlFile;
    	this.meshFile = meshFile;
    	this.loadOptions = loadOptions;
    	this.trackball = trackball;
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
    			this.lightField = UnstructuredLightField.loadFromAgisoftXMLFile(this.cameraFile, this.meshFile, this.loadOptions, this.context);
    		}
    		else
    		{
    			this.lightField = UnstructuredLightField.loadFromVSETFile(this.cameraFile, this.loadOptions, this.context);
    		}
    		
			if (this.callback != null)
			{
				this.callback.loadingComplete();
			}
	    	
	    	this.renderable = context.createRenderable(program);
	    	this.renderable.addVertexBuffer("position", this.lightField.positionBuffer);
	    				
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
    			
    	this.renderable.program().setTexture("imageTextures", lightField.viewSet.getTextures());
    	this.renderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
    	this.renderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
    	this.renderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
    	this.renderable.program().setUniform("cameraPoseCount", lightField.viewSet.getCameraPoseCount());
    	if (lightField.depthTextures != null)
		{
    		this.renderable.program().setTexture("depthTextures", lightField.depthTextures);
		}
    	
    	renderable.program().setUniform("model_view", 
			Matrix4.lookAt(
				new Vector3(0.0f, 0.0f, 5.0f / trackball.getScale()), 
				new Vector3(0.0f, 0.0f, 0.0f),
				new Vector3(0.0f, 1.0f, 0.0f)
			) // View
			.times(trackball.getRotationMatrix())
			.times(Matrix4.translate(lightField.proxy.getCentroid().negated())) // Model
		);
    	
    	renderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 4, (float)size.width / (float)size.height, 0.01f, 100.0f));
    	
    	renderable.program().setUniform("gamma", this.lightField.settings.getGamma());
    	renderable.program().setUniform("weightExponent", this.lightField.settings.getWeightExponent());
    	renderable.program().setUniform("occlusionEnabled", this.lightField.depthTextures != null && this.lightField.settings.isOcclusionEnabled());
    	renderable.program().setUniform("occlusionBias", this.lightField.settings.getOcclusionBias());
    	
    	if(halfResEnabled) {
	    	// Do first pass at half resolution to off-screen buffer
			this.halfResFBO = context.getFramebufferObjectBuilder(size.width/2, size.height/2)
					.addColorAttachment(new ColorAttachmentSpec(ColorFormat.RGB8)
						.setLinearFilteringEnabled(true)
						) //.setMultisamples(4, true)) // TODO why doesn't this work?
					.addDepthAttachment(new DepthAttachmentSpec(16, false)
						) //.setMultisamples(4, true))
					.createFramebufferObject();
			
			halfResFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
	    	halfResFBO.clearDepthBuffer();
	        renderable.draw(PrimitiveMode.TRIANGLES, halfResFBO);
	        context.finish();
	        
	        // Second pass at full resolution to default framebuffer
	    	simpleTexRenderable.program().setTexture("tex", halfResFBO.getColorAttachmentTexture(0));    	

			framebuffer.clearDepthBuffer();
	    	simpleTexRenderable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);

	    	context.finish();
	    	halfResFBO.delete();
    	} else {
    		framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
    		framebuffer.clearDepthBuffer();
	        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);    		
    	}
    }
    
    @Override
    public void cleanup()
    {
    	lightField.deleteOpenGLResources();
    	if(halfResFBO != null)
    	{
    		halfResFBO.delete();
	    	halfResFBO = null;
    	}
    }
    
    public File getVSETFile()
    {
    	return this.cameraFile;
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
    	
    	this.renderable.program().setTexture("imageTextures", lightField.viewSet.getTextures());
    	this.renderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
    	this.renderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
    	this.renderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
    	this.renderable.program().setUniform("cameraPoseCount", lightField.viewSet.getCameraPoseCount());
    	if (lightField.depthTextures != null)
		{
    		this.renderable.program().setTexture("depthTextures", lightField.depthTextures);
		}
    	this.renderable.program().setUniform("gamma", this.lightField.settings.getGamma());
    	this.renderable.program().setUniform("weightExponent", this.lightField.settings.getWeightExponent());
    	this.renderable.program().setUniform("occlusionEnabled", this.lightField.depthTextures != null && this.lightField.settings.isOcclusionEnabled());
    	this.renderable.program().setUniform("occlusionBias", this.lightField.settings.getOcclusionBias());
    	
		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
		{
	    	renderable.program().setUniform("model_view", targetViewSet.getCameraPose(i));
	    	renderable.program().setUniform("projection", 
    			targetViewSet.getCameraProjection(targetViewSet.getCameraProjectionIndex(i))
    				.getProjectionMatrix(targetViewSet.getRecommendedNearPlane(), targetViewSet.getRecommendedFarPlane()));
	    	
	    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
	    	framebuffer.clearDepthBuffer();
	    	
	    	renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
	    	
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
}
