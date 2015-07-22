package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;
import tetzlaff.gl.Context;
import tetzlaff.gl.FramebufferSize;

public class OpenGLDefaultFramebuffer extends OpenGLFramebuffer
{
	private Context context;
	
	private OpenGLDefaultFramebuffer(Context context) 
	{
		this.context = context;
	}
	
	public static OpenGLDefaultFramebuffer fromContext(Context context)
	{
		return new OpenGLDefaultFramebuffer(context);
	}
	
	@Override
	protected int getId()
	{
		return 0;
	}

	@Override
	public FramebufferSize getSize() 
	{
		return context.getFramebufferSize();
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
