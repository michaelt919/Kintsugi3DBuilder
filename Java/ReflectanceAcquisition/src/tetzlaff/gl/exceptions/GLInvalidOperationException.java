package tetzlaff.gl.exceptions;

public class GLInvalidOperationException extends GLException 
{
	private static final long serialVersionUID = 4614535430818755323L;

	public GLInvalidOperationException() 
	{
		super("The specified operation is not allowed in the current state.");
	}
}
