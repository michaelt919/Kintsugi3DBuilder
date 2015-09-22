package tetzlaff.gl.exceptions;

/**
 * Thrown when an operation is attempted which is not allowed in the current state.
 * This error type is essentially a catch-all and there are many varying reasons why this could occur. 
 * @author Michael Tetzlaff
 *
 */
public class GLInvalidOperationException extends GLException 
{
	private static final long serialVersionUID = 4614535430818755323L;

	public GLInvalidOperationException() 
	{
		super("The specified operation is not allowed in the current state.");
	}
}
