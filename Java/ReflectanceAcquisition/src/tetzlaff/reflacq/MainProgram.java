package tetzlaff.reflacq;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.glfw.GLFW.*;

import java.io.File;
import java.io.IOException;

import org.lwjgl.Sys;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.system.glfw.ErrorCallback;

import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.opengl.OpenGLFramebufferObject;
import tetzlaff.gl.opengl.OpenGLIndexBuffer;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLTexture;
import tetzlaff.gl.opengl.OpenGLTexture2D;
import tetzlaff.gl.opengl.OpenGLVertexBuffer;
import tetzlaff.gl.opengl.helpers.StaticHelpers;

public class MainProgram 
{
	private long window;
    private OpenGLTexture texture;
    private OpenGLProgram program;
    private OpenGLFramebufferObject framebuffer;
    private OpenGLFramebufferObject framebuffer2;
    private OpenGLRenderable renderable;
 
    public void execute() 
    {
    	try
    	{
	        System.out.println("Using LWJGL version " + Sys.getVersion());
	        
	        init();
	        
	        draw();
	        
	        program.delete();
	        framebuffer.delete();
	        glfwDestroyWindow(window);
	    } 
	    finally 
	    {
	        glfwTerminate();
	    }
    }
 
    private void init() 
    {
        int WIDTH = 300;
        int HEIGHT = 300;
 
        // Create an invisible 1x1 window just so that we have a valid OpenGL context
        glfwSetErrorCallback(ErrorCallback.Util.getDefault());
        if ( glfwInit() != GL_TRUE )
            throw new IllegalStateException("Unable to initialize GLFW");
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        window = glfwCreateWindow(1, 1, "", NULL, NULL);
        glfwMakeContextCurrent(window);
        
        GLContext.createFromCurrent();
        System.out.println("Using OpenGL version " + glGetString(GL_VERSION));

        framebuffer = new OpenGLFramebufferObject(WIDTH, HEIGHT);
        framebuffer2 = new OpenGLFramebufferObject(WIDTH, HEIGHT);
        
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
        	texture = new OpenGLTexture2D(GL_RGBA, "PNG", "checkerboard.png");
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
        renderable = new OpenGLRenderable(program);
        renderable.addVertexBuffer("position",
    		new OpenGLVertexBuffer(
	    		new FloatVertexList(3, 4, new float[] { 
					-1.0f, -1.0f, -1.0f,
					-1.0f,  1.0f, -1.0f, 
					 1.0f,  1.0f, 1.0f, 
					 1.0f, -1.0f, 1.0f,
				})
			),
			new OpenGLIndexBuffer(
				new int[] { 0, 1, 2, 0, 2, 3 }
			)
        );
        renderable.addVertexBuffer("texCoord", 
    		new OpenGLVertexBuffer(
	    		new FloatVertexList(2, 4, new float[] { 
					0.0f, 0.0f, 
					0.0f, 1.0f, 
					1.0f, 1.0f, 
					1.0f, 0.0f 
				})
			),
			new OpenGLIndexBuffer(
				new int[] { 0, 1, 2, 0, 2, 3 }
			)
        );
    }
    
    private void draw() 
    {
    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
 
    	renderable.program().setUniform("model_view", 
			Matrix4.rotateZ(Math.PI / 2)
				.times(Matrix4.rotateX(Math.PI / 8))
				.times(Matrix4.rotateY(Math.PI / 8))
				.times(Matrix4.translate(0.0f, 0.0f, 1.0f))
				.times(new Matrix4(0.25f, 0.5f, 1.0f, 0.25f, -0.5f, -3.0f)));
    	//renderable.program().setUniform("projection", Matrix4.ortho(0.0f, 1.0f, -1.0f, 0.5f, 0.0f, 2.0f));
    	renderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 4, 1.0f, 1.0f, 3.0f));
    	//renderable.program().setUniform("projection", Matrix4.frustum(-1.0f, 1.0f, -1.0f, 1.0f, 2.0f, 4.0f));
        renderable.program().setTexture("texture0", texture);
        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
        
        try 
        {
            framebuffer.saveColorBufferToFile(0, "png", "output1.png");
        } 
        catch(IOException e)
        {
        	e.printStackTrace();
        }
        
        framebuffer2.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

    	renderable.program().setUniform("model_view", new Matrix4(0.5f, 0.75f, 1.0f, 0.25f, 0.5f, 0.0f));
    	renderable.program().setUniform("projection", Matrix4.ortho(0.0f, 1.0f, -1.0f, 0.0f));
        renderable.program().setTexture("texture0", framebuffer.getColorAttachmentTexture(0));
        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer2);
        
        try 
        {
            framebuffer2.saveColorBufferToFile(0, "png", "output2.png");
        } 
        catch(IOException e)
        {
        	e.printStackTrace();
        }
    }
 
    public static void main(String[] args) 
    {
        new MainProgram().execute();
    }
}
