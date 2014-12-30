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
import tetzlaff.window.glfw.GLFWWindow;

public class MainProgram implements Drawable
{
	private GLFWWindow window;
    private OpenGLTexture texture;
    private OpenGLProgram program;
    VertexMesh mesh;
    private OpenGLRenderable renderable;
    private Trackball trackball;
    
    public MainProgram(GLFWWindow window)
    {
    	this.window = window;
    }
 
    @Override
    public void initialize() 
    {
        this.trackball = new Trackball(2.0f);
        this.trackball.addAsWindowListener(this.window);
        
        this.window.enableDepthTest();
        
        try
        {
        	program = new OpenGLProgram(new File("shaders/test.vert"), new File("shaders/test.frag"));
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
        try
        {
        	texture = new OpenGLTexture2D("PNG", "checkerboard.png");
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
        try
        {
        	renderable = new OpenGLRenderable(program);
        	mesh = new VertexMesh("OBJ", "cube.obj");
        	renderable.addVertexMesh("position", "texCoord", "normal", mesh);
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
    	OpenGLFramebuffer framebuffer = OpenGLDefaultFramebuffer.fromContext(window);
    	
    	FramebufferSize size = framebuffer.getSize();
    	int dim = Math.min(size.width, size.height);
    	int x = (size.width - dim) / 2;
    	int y = (size.height - dim) / 2;
    	
    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
    	framebuffer.clearDepthBuffer();
    	
    	renderable.program().setUniform("model_view", 
			Matrix4.lookAt(
				new Vector3(0.0f, 0.0f, 5.0f).plus(mesh.getCentroid()), 
				mesh.getCentroid(),
				new Vector3(0.0f, 1.0f, 0.0f)
			) // View
			.times(trackball.getRotationMatrix()) // Model
		);
    	
    	renderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 3, 1.0f, 0.01f, 100.0f));
        renderable.program().setTexture("texture0", texture);
        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer, x, y, dim, dim);
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
    	GLFWWindow window = new GLFWWindow(300, 300, "Light Field Renderer", true);
        InteractiveApplication app = InteractiveGraphics.createApplication(window, window, new MainProgram(window));
        window.show();
		app.run();
        GLFWWindow.closeAllWindows();
    }
}
