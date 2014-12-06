package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.glfw.GLFW.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;

import java.nio.IntBuffer;

public class OpenGLDefaultFramebuffer extends OpenGLFramebuffer
{
	private static OpenGLDefaultFramebuffer instance;
	
	private OpenGLDefaultFramebuffer() 
	{
	}
	
	@Override
	protected int getId()
	{
		return 0;
	}

	@Override
	public int getWidth() 
	{
		throw new UnsupportedOperationException("Retrieving the width or height of the default framebuffer is not currently supported.");
//		long window = glfwGetCurrentContext();
//		IntBuffer widthBuffer = IntBuffer.allocate(1);
//		glfwGetFramebufferSize(window, widthBuffer, null);
//		return widthBuffer.get();
	}

	@Override
	public int getHeight() 
	{
		throw new UnsupportedOperationException("Retrieving the width or height of the default framebuffer is not currently supported.");
//		long window = glfwGetCurrentContext();
//		IntBuffer heightBuffer = IntBuffer.allocate(1);
//		glfwGetFramebufferSize(window, null, heightBuffer);
//		return heightBuffer.get();
	}
	
	public static OpenGLDefaultFramebuffer getInstance()
	{
		return instance == null ? instance = new OpenGLDefaultFramebuffer() : instance;
	}

	@Override
	protected void selectColorSourceForRead(int index) 
	{
		if (index != 0)
		{
			throw new IllegalArgumentException("The default framebuffer does not have multiple color attachments.");
		}
		else
		{
			glReadBuffer(GL_BACK);
			openGLErrorCheck();
		}
	}
}
