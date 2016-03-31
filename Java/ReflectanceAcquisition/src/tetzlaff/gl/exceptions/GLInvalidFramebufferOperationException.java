package tetzlaff.gl.exceptions;

/**
 * Thrown when an attempt is made to use a framebuffer object when it is not "complete."
 * @author Michael Tetzlaff
 *
 */
public class GLInvalidFramebufferOperationException extends GLException 
{
	private static final long serialVersionUID = 5325152218446700770L;

	public GLInvalidFramebufferOperationException(String framebufferStatusString) 
	{
		super(framebufferStatusString);
	}
}
