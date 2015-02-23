package tetzlaff.gl.opengl.exceptions;

public class OpenGLInvalidFramebufferOperationException extends OpenGLException 
{
	private static final long serialVersionUID = 5325152218446700770L;

	public OpenGLInvalidFramebufferOperationException() 
	{
		super("The framebuffer object is not complete.");
	}
}
