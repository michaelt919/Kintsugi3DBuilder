package tetzlaff.reflacq;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.InteractiveGraphics;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.gl.opengl.OpenGLDefaultFramebuffer;
import tetzlaff.gl.opengl.OpenGLFramebuffer;
import tetzlaff.gl.opengl.OpenGLFramebufferObject;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLTexture;
import tetzlaff.gl.opengl.OpenGLTexture2D;
import tetzlaff.interactive.InteractiveApplication;
import tetzlaff.lightfield.ViewSet;
import tetzlaff.window.glfw.GLFWWindow;

public class MainProgram implements Drawable
{
	private GLFWWindow window;
    private OpenGLTexture texture;
    private OpenGLProgram program;
    private VertexMesh mesh;
    private ViewSet viewSet;
    private OpenGLRenderable renderable;
    private Trackball trackball;
    
    public MainProgram(GLFWWindow window)
    {
    	this.window = window;
    }
 
    @Override
    public void initialize() 
    {
        this.trackball = new Trackball(1.0f);
        this.trackball.addAsWindowListener(this.window);
        
        this.window.enableDepthTest();
        
        try
        {
        	program = new OpenGLProgram(new File("shaders/ulr.vert"), new File("shaders/ulr.frag"));
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
        try
        {
        	texture = new OpenGLTexture2D("checkerboard.png", true, true);
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
        try
        {
        	renderable = new OpenGLRenderable(program);
        	mesh = new VertexMesh("OBJ", "pumpkin-warts/manifold.obj");
        	renderable.addVertexMesh("position", "texCoord", "normal", mesh);
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
        try
        {
        	viewSet = ViewSet.loadFromVSETFile("pumpkin-warts/default.vset");
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
		renderable.program().setTexture("imageTextures", viewSet.getTextures());
    	renderable.program().setUniformBuffer("CameraPoses", viewSet.getCameraPoseBuffer());
    	renderable.program().setUniformBuffer("CameraProjections", viewSet.getCameraProjectionBuffer());
    	renderable.program().setUniformBuffer("CameraProjectionIndices", viewSet.getCameraProjectionIndexBuffer());
    	renderable.program().setUniform("cameraPoseCount", viewSet.getCameraPoseCount());

        window.show();
    }

	@Override
	public void update()
	{
	}
    
    @Override
    public void draw() 
    {
    	OpenGLFramebuffer framebuffer = OpenGLDefaultFramebuffer.fromContext(window);
    	
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
			.times(Matrix4.translate(mesh.getCentroid().negated())) // Model
		);
    	
    	renderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 4, (float)size.width / (float)size.height, 0.01f, 100.0f));
    	
    	renderable.program().setUniform("gamma", 2.2f);
    	renderable.program().setUniform("weightExponent", 16.0f);
    	
        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer, 0, 0, size.width, size.height);
    }
    
    @Override
    public void cleanup()
    {
    	// Print the last frame to a file
    	OpenGLFramebuffer framebuffer = OpenGLDefaultFramebuffer.fromContext(window);
    	FramebufferSize size = framebuffer.getSize();
    	int dim = Math.min(size.width, size.height);
    	
    	OpenGLFramebufferObject framebuffer2 = new OpenGLFramebufferObject(dim, dim);
    	framebuffer2.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
    	framebuffer2.clearDepthBuffer();
    	renderable.draw(PrimitiveMode.TRIANGLES, framebuffer2);
    	
		try 
		{
		    framebuffer2.saveColorBufferToFile(0, "png", "output.png");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
        framebuffer2.delete();
    	
        program.delete();
        texture.delete();
    }
 
    public static void main(String[] args) 
    {
    	GLFWWindow window = new GLFWWindow(300, 300, "Light Field Renderer", true, 4);
        InteractiveApplication app = InteractiveGraphics.createApplication(window, window, new MainProgram(window));
		app.run();
        GLFWWindow.closeAllWindows();
    }
}
