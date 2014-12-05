import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.glfw.GLFW.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import openGL.OpenGLHelper;
import openGL.helpers.FloatVertexList;
import openGL.wrappers.implementations.OpenGLFramebufferObject;
import openGL.wrappers.implementations.OpenGLProgram;
import openGL.wrappers.implementations.OpenGLRenderable;
import openGL.wrappers.implementations.OpenGLTexture2D;
import openGL.wrappers.implementations.OpenGLVertexArray;
import openGL.wrappers.implementations.OpenGLVertexBuffer;
import openGL.wrappers.interfaces.FramebufferObject;
import openGL.wrappers.interfaces.Program;
import openGL.wrappers.interfaces.Renderable;
import openGL.wrappers.interfaces.Texture;
import openGL.wrappers.interfaces.VertexArray;

import org.lwjgl.Sys;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.system.glfw.ErrorCallback;
import org.lwjgl.BufferUtils;

public class MainProgram 
{
	private long window;
    private Texture texture;
    private Program program;
    private FramebufferObject framebuffer;
    private FramebufferObject framebuffer2;
    private Renderable renderable;
    private OpenGLVertexBuffer positions;
 
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
        	program.use();
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
        
//        renderable = new OpenGLRenderable(program);
//        renderable.addVertexBuffer("position", positions = new OpenGLVertexBuffer(
//    		new FloatVertexList(3, 4, new float[] { 
//				-1.0f, -1.0f, 0.0f,
//				-1.0f,  1.0f, 0.0f, 
//				 1.0f,  1.0f, 0.0f, 
//				 1.0f, -1.0f, 0.0f,
//			})
//		));
//        renderable.addVertexBuffer("texCoord", new OpenGLVertexBuffer(
//    		new FloatVertexList(2, 4, new float[] { 
//				0.0f, 0.0f, 
//				0.0f, 1.0f, 
//				1.0f, 1.0f, 
//				1.0f, 0.0f 
//			})
//		));
    }
    
    private void draw() 
    {
    	framebuffer.bindForDraw();
    	glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        OpenGLHelper.openGLErrorCheck();
        
        //int name = program.getVertexAttribLocation("position");
        int bufferID = glGenBuffers();
        OpenGLHelper.openGLErrorCheck();
        FloatBuffer bufferAlt = FloatBuffer.wrap(new float[] {
    		-1.0f, -1.0f, 0.0f,
			-1.0f,  1.0f, 0.0f, 
			 1.0f,  1.0f, 0.0f, 
			 1.0f, -1.0f, 0.0f
        });
        ByteBuffer buffer = BufferUtils.createByteBuffer(48);//ByteBuffer.allocateDirect(48);
       // buffer.order(ByteOrder.nativeOrder());
        buffer.putFloat(-1.0f);
        buffer.putFloat(-1.0f);
        buffer.putFloat(0.0f);
        buffer.putFloat(-1.0f);
        buffer.putFloat(1.0f);
        buffer.putFloat(0.0f);
        buffer.putFloat(1.0f);
        buffer.putFloat(1.0f);
        buffer.putFloat(0.0f);
        buffer.putFloat(1.0f);
        buffer.putFloat(-1.0f);
        buffer.putFloat(0.0f);
        buffer.clear();
        float[] array = new float[] { 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };
        buffer.asFloatBuffer().get(array);
        glBindBuffer(GL_ARRAY_BUFFER, bufferID);
        OpenGLHelper.openGLErrorCheck();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        OpenGLHelper.openGLErrorCheck();
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableVertexAttribArray(program.getVertexAttribLocation("position"));
        OpenGLHelper.openGLErrorCheck();
        glBindBuffer(GL_ARRAY_BUFFER, bufferID);
        OpenGLHelper.openGLErrorCheck();
        glVertexAttribPointer(program.getVertexAttribLocation("position"), 3, GL_FLOAT, false, 0, 0);
        OpenGLHelper.openGLErrorCheck();
        ByteBuffer buffer2 = ByteBuffer.allocateDirect(48);
        glGetBufferSubData(GL_ARRAY_BUFFER, 0, buffer2);
        array = new float[] { 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };
        buffer2.asFloatBuffer().get(array);
        OpenGLHelper.openGLErrorCheck();
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        OpenGLHelper.openGLErrorCheck();
//
        //renderable.program().setTexture("texture0", texture);
        //renderable.draw(GL_TRIANGLE_FAN, framebuffer);
//        
//        int position = program.getVertexAttribLocation("position");
//        
//        glBegin(GL_TRIANGLE_FAN);
//	    	glVertexAttrib3f(position, -1.0f, -1.0f, 0.0f);
//	    	glVertexAttrib3f(position, -1.0f, 1.0f, 0.0f);
//	    	glVertexAttrib3f(position, 1.0f, 1.0f, 0.0f);
//	    	glVertexAttrib3f(position, 1.0f, -1.0f, 0.0f);
//	    glEnd();
        
        try 
        {
            framebuffer.saveToFile(0, "png", "output1.png");
        } 
        catch(IOException e)
        {
        	e.printStackTrace();
        }
        
        framebuffer2.bindForDraw();
    	glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        OpenGLHelper.openGLErrorCheck();

        //renderable.program().setTexture("texture0", framebuffer.getColorAttachmentTexture(0));
        //renderable.draw(GL_TRIANGLE_FAN, framebuffer2);
        
        try 
        {
            framebuffer2.saveToFile(0, "png", "output2.png");
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
