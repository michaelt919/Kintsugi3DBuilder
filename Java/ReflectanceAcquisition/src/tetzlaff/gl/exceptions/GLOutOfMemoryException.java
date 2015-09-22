package tetzlaff.gl.exceptions;

/**
 * Thrown when the GL implementation runs out of memory.
 * The state of the GL will be undefined if this is thrown 
 * (meaning that the context has no further use and should be be destroyed).
 * @author Michael Tetzlaff
 *
 */
public class GLOutOfMemoryException extends GLException 
{
	private static final long serialVersionUID = -1977989622291033976L;

	public GLOutOfMemoryException() 
	{
		super("There is not enough memory left to execute the command.  The state of the GL is undefined.");
	}
}
