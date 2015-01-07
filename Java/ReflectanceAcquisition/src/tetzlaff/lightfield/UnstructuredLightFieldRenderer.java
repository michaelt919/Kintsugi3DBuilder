package tetzlaff.lightfield;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLDefaultFramebuffer;
import tetzlaff.gl.opengl.OpenGLFramebuffer;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;

public class UnstructuredLightFieldRenderer implements Drawable
{
    private static OpenGLProgram program;
    
	private OpenGLContext context;
    private UnstructuredLightField lightField;
    private OpenGLRenderable renderable;
    private Trackball trackball;
    
    private float gamma = 2.2f;
    private float weightExponent = 16.0f;
    private boolean occlusionEnabled = true;
    private float occlusionBias = 0.005f;
    
    static
    {
    	try
        {
        	program = new OpenGLProgram(new File("shaders/ulr.vert"), new File("shaders/ulr.frag"));
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
    }
    
    public UnstructuredLightFieldRenderer(OpenGLContext context, UnstructuredLightField lightField, Trackball trackball)
    {
    	this.context = context;
    	this.lightField = lightField;
    	this.trackball = trackball;
    }
 
    @Override
    public void initialize() 
    {
        this.context.enableDepthTest();
    	renderable = new OpenGLRenderable(program);
    	renderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
		renderable.program().setTexture("imageTextures", lightField.viewSet.getTextures());
    	renderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
    	renderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
    	renderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
    	renderable.program().setUniform("cameraPoseCount", lightField.viewSet.getCameraPoseCount());
    	renderable.program().setTexture("depthTextures", lightField.depthTextures);
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
    	
    	renderable.program().setUniform("gamma", this.gamma);
    	renderable.program().setUniform("weightExponent", this.weightExponent);
    	renderable.program().setUniform("occlusionEnabled", this.occlusionEnabled);
    	renderable.program().setUniform("occlusionBias", this.occlusionBias);
    	
        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
    }
    
    @Override
    public void cleanup()
    {
        lightField.deleteOpenGLResources();
    }

	public float getGamma() 
	{
		return this.gamma;
	}

	public void setGamma(float gamma) 
	{
		this.gamma = gamma;
	}

	public float getWeightExponent() 
	{
		return this.weightExponent;
	}

	public void setWeightExponent(float weightExponent) 
	{
		this.weightExponent = weightExponent;
	}

	public boolean isOcclusionEnabled() 
	{
		return this.occlusionEnabled;
	}

	public void setOcclusionEnabled(boolean occlusionEnabled) 
	{
		this.occlusionEnabled = occlusionEnabled;
	}

	public float getOcclusionBias() 
	{
		return this.occlusionBias;
	}

	public void setOcclusionBias(float occlusionBias) 
	{
		this.occlusionBias = occlusionBias;
	}
}
