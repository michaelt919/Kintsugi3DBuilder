package tetzlaff.reflacq;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.glfw.GLFW.*;

import java.io.File;
import java.io.IOException;

import org.lwjgl.Sys;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.system.glfw.ErrorCallback;

import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.gl.opengl.OpenGLFramebufferObject;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLTexture;
import tetzlaff.gl.opengl.OpenGLTexture2D;

public class MainProgram 
{
	private long window;
    private OpenGLTexture texture;
    private OpenGLProgram program;
    private OpenGLFramebufferObject framebuffer;
    private OpenGLFramebufferObject framebuffer2;
    VertexMesh mesh;
    private OpenGLRenderable renderable;
 
    public void execute() 
    {
    	try
    	{
	        init();
	        draw();
	        cleanup();
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
 
        System.out.println("Using LWJGL version " + Sys.getVersion());
        
        // Create an invisible 1x1 window just so that we have a valid OpenGL context
        glfwSetErrorCallback(ErrorCallback.Util.getDefault());
        if ( glfwInit() != GL_TRUE )
            throw new IllegalStateException("Unable to initialize GLFW");
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        window = glfwCreateWindow(1, 1, "", NULL, NULL);
        glfwMakeContextCurrent(window);
        
        GLContext.createFromCurrent();
        System.out.println("Using OpenGL version " + glGetString(GL_VERSION));
        
        glEnable(GL_DEPTH_TEST);

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
        
        try
        {
        	renderable = new OpenGLRenderable(program);
        	mesh = new VertexMesh("OBJ", "frog.obj");
        	renderable.addVertexMesh("position", "texCoord", "normal", mesh);
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
//        renderable = new OpenGLRenderable(program);
//        renderable.addVertexBuffer("position",
//    		new OpenGLVertexBuffer(
//	    		new FloatVertexList(3, 4, new float[] { 
//					-1.0f, -1.0f, -1.0f,
//					-1.0f,  1.0f, -1.0f, 
//					 1.0f,  1.0f, 1.0f, 
//					 1.0f, -1.0f, 1.0f,
//				})
//			),
//			new OpenGLIndexBuffer(
//				new int[] { 0, 1, 2, 0, 2, 3 }
//			)
//        );
//        renderable.addVertexBuffer("texCoord", 
//    		new OpenGLVertexBuffer(
//	    		new FloatVertexList(2, 4, new float[] { 
//					0.0f, 0.0f, 
//					0.0f, 1.0f, 
//					1.0f, 1.0f, 
//					1.0f, 0.0f 
//				})
//			),
//			new OpenGLIndexBuffer(
//				new int[] { 0, 1, 2, 0, 2, 3 }
//			)
//        );
    }
    
    private void draw() 
    {
    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
    	framebuffer.clearDepthBuffer();
    	
    	renderable.program().setUniform("model_view", 
			Matrix4.lookAt(
				new Vector3(2.0f, -3.0f, 3.0f)
					.plus(mesh.getCentroid()), 
				mesh.getCentroid(),
				new Vector3(0.0f, -1.0f, 0.0f)
			)
		);
//    	renderable.program().setUniform("model_view", 
//			Matrix4.lookAt(10.0f, 20.0f, 30.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
//		);
//    	renderable.program().setUniform("model_view", 
//			Matrix4.rotateZ(Math.PI / 2)
//				.times(Matrix4.rotateX(Math.PI / 8))
//				.times(Matrix4.rotateY(Math.PI / 8))
//				.times(Matrix4.translate(0.0f, 0.0f, 1.0f))
//				.times(new Matrix4(0.25f, 0.5f, 1.0f, 0.25f, -0.5f, -3.0f)));
    	//renderable.program().setUniform("projection", Matrix4.ortho(0.0f, 1.0f, -1.0f, 0.5f, 0.0f, 2.0f));
    	renderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 3, 1.0f, 0.01f, 100.0f));
    	//renderable.program().setUniform("projection", Matrix4.frustum(-1.0f, 1.0f, -1.0f, 1.0f, 2.0f, 4.0f));
        renderable.program().setTexture("texture0", texture);
        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
        
        try 
        {
            framebuffer.saveColorBufferToFile(0, "png", "output.png");
        } 
        catch(IOException e)
        {
        	e.printStackTrace();
        }
        
//        framebuffer2.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
//
//    	renderable.program().setUniform("model_view", new Matrix4(0.5f, 0.75f, 1.0f, 0.25f, 0.5f, 0.0f));
//    	renderable.program().setUniform("projection", Matrix4.ortho(0.0f, 1.0f, -1.0f, 0.0f));
//        renderable.program().setTexture("texture0", framebuffer.getColorAttachmentTexture(0));
//        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer2);
//        
//        try 
//        {
//            framebuffer2.saveColorBufferToFile(0, "png", "output2.png");
//        } 
//        catch(IOException e)
//        {
//        	e.printStackTrace();
//        }
    }
    
    private void cleanup()
    {
        program.delete();
        framebuffer.delete();
        framebuffer2.delete();
        texture.delete();
        glfwDestroyWindow(window);
    }
 
    public static void main(String[] args) 
    {
        new MainProgram().execute();
    }
}
