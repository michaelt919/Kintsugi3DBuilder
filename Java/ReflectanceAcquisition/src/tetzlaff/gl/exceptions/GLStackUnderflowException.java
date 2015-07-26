package tetzlaff.gl.exceptions;

public class GLStackUnderflowException extends GLException 
{
	private static final long serialVersionUID = -6641362706730794229L;

	public GLStackUnderflowException() 
	{
		super("An attempt has been made to perform an operation that would cause an internal stack to underflow.");
	}
}
