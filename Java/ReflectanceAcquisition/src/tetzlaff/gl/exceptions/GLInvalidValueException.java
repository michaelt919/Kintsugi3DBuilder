package tetzlaff.gl.exceptions;

/**
 * Thrown when a numeric argument is out of range.
 * One common cause of this is attempting to use an OpenGL object that has already been destroyed.
 * @author Michael Tetzlaff
 *
 */
public class GLInvalidValueException extends GLException 
{
	private static final long serialVersionUID = 4250451180152106370L;

	public GLInvalidValueException() 
	{
		super("A numeric argument is out of range.");
	}
}
