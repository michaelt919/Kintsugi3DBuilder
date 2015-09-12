package tetzlaff.gl.exceptions;

public class GLInvalidFramebufferOperationException extends GLException 
{
	private static final long serialVersionUID = 5325152218446700770L;

	public GLInvalidFramebufferOperationException() 
	{
		super("The framebuffer object is not complete.");
	}
}
