package openGL.wrappers.implementations;

import static openGL.OpenGLHelper.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

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
