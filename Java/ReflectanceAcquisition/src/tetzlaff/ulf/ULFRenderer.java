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
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;

/**
 * An implementation of a renderer for a single unstructured light field.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the context that will be used for rendering.
 */
public class ULFRenderer<ContextType extends Context<ContextType>> implements ULFDrawable<ContextType>
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

    private boolean kNeighborsEnabled;
    private int kNeighborCount;
    
    private boolean halfResEnabled;
    private FramebufferObject<ContextType> halfResFBO;
    private Program<ContextType> simpleTexProgram;
    private Renderable<ContextType> simpleTexRenderable;

    private boolean cameraVisEnabled;
    Program<ContextType> cameraVisProgram;
    private Renderable<ContextType> cameraVisRenderable;
    
    private VertexBuffer<ContextType> rectangleVBO;

    private boolean resampleRequested;
    private int resampleWidth, resampleHeight;
    private File resampleVSETFile;
    private File resampleExportPath;

    private Vector4 backgroundColor;
    
    private Exception initError;
    	
    /**
     * Creates a new unstructured light field renderer for rendering a light field defined by a VSET file.
     * @param context The GL context in which to perform the rendering.
     * @param program The program to use for rendering.
     * @param vsetFile The view set file defining the light field to be rendered.
     * @param loadOptions The options to use when loading the light field.
     * @param trackball The trackball controlling the movement of the virtual camera.
     */
    public ULFRenderer(ContextType context, Program<ContextType> program, File vsetFile, ULFLoadOptions loadOptions, Trackball trackball)
    {
    	this.context = context;
    	this.program = program;
    	this.cameraFile = vsetFile;
    	this.loadOptions = loadOptions;
    	this.trackball = trackball;
    	this.initError = null;
    	this.backgroundColor = new Vector4(0.30f, 0.30f, 0.30f, 1.0f);
    	this.kNeighborCount = 5;
    }

    /**
     * Creates a new unstructured light field renderer for rendering a light field from Agisoft PhotoScan.
     * @param context The GL context in which to perform the rendering.
     * @param program The program to use for rendering.
     * @param xmlFile The Agisoft PhotoScan XML camera file defining the views to load.
     * @param meshFile The mesh exported from Agisoft PhotoScan to be used as proxy geometry.
     * @param loadOptions The options to use when loading the light field.
     * @param trackball The trackball controlling the movement of the virtual camera.
     */
    public ULFRenderer(ContextType context, Program<ContextType> program, File xmlFile, File meshFile, ULFLoadOptions loadOptions, Trackball trackball)
    {
    	this.context = context;
    	this.program = program;
    	this.cameraFile = xmlFile;
    	this.meshFile = meshFile;
    	this.loadOptions = loadOptions;
    	this.trackball = trackball;
    	this.initError = null;
    	this.backgroundColor = new Vector4(0.30f, 0.30f, 0.30f, 1.0f);
    	this.kNeighborCount = 5;
    }
    
    @Override
    public void setOnLoadCallback(ULFLoadingMonitor callback)
    {
    	this.callback = callback;
    }
 
    @Override
    public void initialize() 
    {
    	// Initialize shaders
    	if (this.program == null)
    	{
	    	try
	        {
	    		this.program = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File(UnstructuredLightField.SHADER_RESOURCE_DIRECTORY, "ulr.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File(UnstructuredLightField.SHADER_RESOURCE_DIRECTORY, "ulr.frag"))
	    				.createProgram();
	        }
	        catch (IOException e)
	        {
	        	this.initError = e;
	        }
    	}

    	if (this.simpleTexProgram == null)
    	{
	    	try
	        {
	    		this.simpleTexProgram = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File(UnstructuredLightField.SHADER_RESOURCE_DIRECTORY, "texturerect.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File(UnstructuredLightField.SHADER_RESOURCE_DIRECTORY, "simpletexture.frag"))
	    				.createProgram();
	        }
	        catch (IOException e)
	        {
	        	this.initError = e;
	        }
    	}

    	if (this.cameraVisProgram == null)
    	{
	    	try
	        {
	    		this.cameraVisProgram = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File(UnstructuredLightField.SHADER_RESOURCE_DIRECTORY, "uniform3D.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File(UnstructuredLightField.SHADER_RESOURCE_DIRECTORY, "uniform.frag"))
	    				.createProgram();
	        }
	        catch (IOException e)
	        {
	        	this.initError = e;
	        }
    	}
    	
    	try 
    	{
    		// Read main cam def function and vicariously, all view image files
			if (this.callback != null)
			{
				this.callback.startLoading();
			}

    		if (this.cameraFile.getName().toUpperCase().endsWith(".XML"))
    		{
    			this.lightField = UnstructuredLightField.loadFromAgisoftXMLFile(this.cameraFile, this.meshFile, this.loadOptions, this.context, callback);
    		}
    		else
    		{
    			this.lightField = UnstructuredLightField.loadFromVSETFile(this.cameraFile, this.loadOptions, this.context, callback);
    		}
    			    	
	    	this.renderable = context.createRenderable(program);
	    	this.renderable.addVertexBuffer("position", this.lightField.positionBuffer);
	    	
	    	this.rectangleVBO = context.createRectangle();
	    				
	    	this.simpleTexRenderable = context.createRenderable(simpleTexProgram);
	    	this.simpleTexRenderable.addVertexBuffer("position", rectangleVBO);
	    	
	        this.cameraVisRenderable = context.createRenderable(cameraVisProgram);
	        this.cameraVisRenderable.addVertexBuffer("position", rectangleVBO);
		} 
    	catch (IOException e) 
    	{
        	this.initError = e;
		}
    	finally
    	{
			if (this.callback != null)
			{
				this.callback.loadingComplete();
			}    		
    	}
    }

	@Override
	public Exception getInitializeError()
	{
		return initError;
	}
		
	@Override
	public boolean hasInitializeError()
	{
		return (initError != null);
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
    public boolean draw()
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
    	renderable.program().setUniform("buehlerEnabled", kNeighborsEnabled);
    	renderable.program().setUniform("sampleCount", kNeighborCount);

    	if(halfResEnabled) {
	    	// Do first pass at half resolution to off-screen buffer
			this.halfResFBO = context.getFramebufferObjectBuilder(size.width/2, size.height/2)
					.addColorAttachment(new ColorAttachmentSpec(ColorFormat.RGB8)
						.setLinearFilteringEnabled(true)
						) //.setMultisamples(4, true)) // TODO why doesn't this work?
					.addDepthAttachment(new DepthAttachmentSpec(16, false)
						) //.setMultisamples(4, true))
					.createFramebufferObject();
			
			halfResFBO.clearColorBuffer(0, backgroundColor.x, backgroundColor.y, backgroundColor.z, backgroundColor.w);
	    	halfResFBO.clearDepthBuffer();
	        renderable.draw(PrimitiveMode.TRIANGLES, halfResFBO);
	        
	        if (cameraVisEnabled)
	        {
	        	drawCameras(halfResFBO, size);
	        }

	        context.finish();
	        
	        // Second pass at full resolution to default framebuffer
	    	simpleTexRenderable.program().setTexture("tex", halfResFBO.getColorAttachmentTexture(0));    	

			framebuffer.clearDepthBuffer();
	    	simpleTexRenderable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);

	    	context.finish();
	    	halfResFBO.delete();
    	} else {
    		framebuffer.clearColorBuffer(0, backgroundColor.x, backgroundColor.y, backgroundColor.z, backgroundColor.w);
    		framebuffer.clearDepthBuffer();
	        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
	        
	        if (cameraVisEnabled)
	        {
	        	drawCameras(framebuffer, size);
	        }
    	}
    	
    	return true;
    }
    
    private void drawCameras(Framebuffer<ContextType> framebuffer, FramebufferSize size)
    {
    	Matrix4 persp = Matrix4.perspective((float)Math.PI / 4, (float)size.width / (float)size.height, 0.01f, 100.0f);
    	for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
        {
        	cameraVisProgram.setUniform("projection", persp);
        	cameraVisProgram.setUniform("model_view", 
    			Matrix4.lookAt(
					new Vector3(0.0f, 0.0f, 5.0f / trackball.getScale()), 
					new Vector3(0.0f, 0.0f, 0.0f),
					new Vector3(0.0f, 1.0f, 0.0f)
				) // View
				.times(trackball.getRotationMatrix())
				.times(Matrix4.translate(lightField.proxy.getCentroid().negated()))
				.times(lightField.viewSet.getCameraPoseInverse(i))
				.times(Matrix4.scale(0.25f))
			); // Model
        	
        	if (i == 0)
        	{
        		cameraVisProgram.setUniform("color", new Vector4(1.0f, 0.5f, 0.75f, 1.0f));
        	}
        	else
        	{
        		cameraVisProgram.setUniform("color", new Vector4(0.5f, 0.75f, 1.0f, 1.0f));
        	}
        	
        	cameraVisRenderable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
        }
    }
    
	@Override
	public void saveToFile(String fileFormat, File file)
	{
    	Framebuffer<ContextType> framebuffer = context.getDefaultFramebuffer();
    	try {
			framebuffer.saveColorBufferToFile(0, fileFormat, file);
		} catch (IOException e) {
			System.err.println("Error saving to file " + file.getPath());
			e.printStackTrace(System.err);
		}
	}

    @Override
    public void cleanup()
    {
    	if(lightField != null)
    	{
    		lightField.deleteOpenGLResources();
    	}
    	if(halfResFBO != null)
    	{
    		halfResFBO.delete();
	    	halfResFBO = null;
    	}
    	if (rectangleVBO != null)
    	{
    		rectangleVBO.delete();
    		rectangleVBO = null;
    	}
    	if (simpleTexProgram != null)
    	{
    		simpleTexProgram.delete();
    		simpleTexProgram = null;
    	}
    	if (cameraVisProgram != null)
    	{
    		cameraVisProgram.delete();
    		cameraVisProgram = null;
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
		if(this.lightField == null) return "Error";
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
		ViewSet<ContextType> targetViewSet = ViewSet.loadFromVSETFile(resampleVSETFile, new ViewSetImageOptions(null, false, false, false), context, null);
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
    	this.renderable.program().setUniform("buehlerEnabled", false);

    	// Reset the callback
		if(callback != null) {
			callback.startLoading();
			callback.setMaximum(targetViewSet.getCameraPoseCount());
		}
		
		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
		{
	    	renderable.program().setUniform("model_view", targetViewSet.getCameraPose(i));
	    	renderable.program().setUniform("projection", 
    			targetViewSet.getCameraProjection(targetViewSet.getCameraProjectionIndex(i))
    				.getProjectionMatrix(targetViewSet.getRecommendedNearPlane(), targetViewSet.getRecommendedFarPlane()));
	    	
	    	framebuffer.clearColorBuffer(0, backgroundColor.x, backgroundColor.y, backgroundColor.z, backgroundColor.w);
	    	framebuffer.clearDepthBuffer();
	    	
	    	renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
	    	
	    	File exportFile = new File(resampleExportPath, targetViewSet.getImageFileName(i));
	    	exportFile.getParentFile().mkdirs();
	        framebuffer.saveColorBufferToFile(0, "PNG", exportFile);
	        
	        if (this.callback != null)
	        {
	        	this.callback.setProgress(i);
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

	@Override
	public void setVisualizeCameras(boolean camerasEnabled) {
		cameraVisEnabled = camerasEnabled;
	}

	@Override
	public void setBackgroundColor(Vector4 RGBA) {
		backgroundColor = RGBA;
	}

	@Override
	public Vector4 getBackgroundColor() {
		return backgroundColor;
	}

	@Override
	public boolean isKNeighborsEnabled() {
		return kNeighborsEnabled;
	}

	@Override
	public void setKNeighborsEnabled(boolean kNeighborsEnabled) {
		this.kNeighborsEnabled = kNeighborsEnabled;
	}

	@Override
	public int getKNeighborCount() {
		return kNeighborCount;
	}

	@Override
	public void setKNeighborCount(int kNeighborCount) {
		this.kNeighborCount = kNeighborCount;
	}

	@Override
	public void setProgram(Program<ContextType> program) 
	{
		this.program = program;
		
		this.renderable = context.createRenderable(program);
    	this.renderable.addVertexBuffer("position", this.lightField.positionBuffer);
    	
    	if (this.lightField.normalBuffer != null)
    	{
    		this.renderable.addVertexBuffer("normal", this.lightField.normalBuffer);
    	}
    	
    	if (this.lightField.texCoordBuffer != null)
    	{
    		this.renderable.addVertexBuffer("texCoord", this.lightField.texCoordBuffer);
    	}
	}
}
