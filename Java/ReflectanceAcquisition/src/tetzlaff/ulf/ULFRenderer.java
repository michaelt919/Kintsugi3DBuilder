package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLDefaultFramebuffer;
import tetzlaff.gl.opengl.OpenGLFramebuffer;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLResource;

public class ULFRenderer implements ULFDrawable
{
    private static OpenGLProgram program;
    
    private String vsetFile;
    private UnstructuredLightField lightField;
	private OpenGLContext context;
    private OpenGLRenderable renderable;
    private Trackball trackball;
    private ULFLoadingMonitor callback;
    private Iterable<OpenGLResource> vboResources;

    public ULFRenderer(OpenGLContext context, String vsetFile, Trackball trackball)
    {
    	this.context = context;
    	this.vsetFile = vsetFile;
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
    	if (ULFRenderer.program == null)
    	{
	    	try
	        {
	    		ULFRenderer.program = new OpenGLProgram(new File("shaders/ulr.vert"), new File("shaders/ulr.frag"));
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
    	}
    	
    	try 
    	{
			this.lightField = UnstructuredLightField.loadFromVSETFile(this.vsetFile);
			if (this.callback != null)
			{
				this.callback.loadingComplete();
			}
	    	
	    	this.renderable = new OpenGLRenderable(program);
	    	this.vboResources = this.renderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
		} 
    	catch (IOException e) 
    	{
			e.printStackTrace();
		}
    }

	@Override
	public void update()
	{
	}
    
    @Override
    public void draw() 
    {
    	OpenGLFramebuffer framebuffer = OpenGLDefaultFramebuffer.fromContext(context);
    	
    	FramebufferSize size = framebuffer.getSize();
    	
    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
    	framebuffer.clearDepthBuffer();

    	this.renderable.program().setTexture("imageTextures", lightField.viewSet.getTextures());
    	this.renderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
    	this.renderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
    	this.renderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
    	this.renderable.program().setUniform("cameraPoseCount", lightField.viewSet.getCameraPoseCount());
    	this.renderable.program().setTexture("depthTextures", lightField.depthTextures);
    	
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
    	renderable.program().setUniform("occlusionEnabled", this.lightField.settings.isOcclusionEnabled());
    	renderable.program().setUniform("occlusionBias", this.lightField.settings.getOcclusionBias());
    	
        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
    }
    
    @Override
    public void cleanup()
    {
    	for (OpenGLResource r : vboResources)
    	{
    		r.delete();
    	}
    	
        lightField.deleteOpenGLResources();
    }
    
    public String getVSETFileName()
    {
    	return this.vsetFile;
    }
    
    public UnstructuredLightField getLightField()
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
}
