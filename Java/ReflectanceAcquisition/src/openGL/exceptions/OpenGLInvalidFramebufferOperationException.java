package openGL.exceptions;

public class OpenGLInvalidFramebufferOperationException extends OpenGLException 
{
	public OpenGLInvalidFramebufferOperationException() 
	{
		super("The framebuffer object is not complete.");
	}
}
