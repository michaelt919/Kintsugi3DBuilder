package tetzlaff.gl.exceptions;

/**
 * Thrown when an attempt is made to perform an operation that would cause an internal stack to overflow.
 * @author Michael Tetzlaff
 *
 */
public class GLStackOverflowException extends GLException 
{
	private static final long serialVersionUID = -6479066172793823172L;

	public GLStackOverflowException() 
	{
		super("An attempt has been made to perform an operation that would cause an internal stack to overflow.");
	}
}
