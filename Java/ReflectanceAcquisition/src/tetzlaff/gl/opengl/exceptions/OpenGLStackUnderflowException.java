package tetzlaff.gl.opengl.exceptions;

public class OpenGLStackUnderflowException extends OpenGLException 
{
	private static final long serialVersionUID = -6641362706730794229L;

	public OpenGLStackUnderflowException() 
	{
		super("An attempt has been made to perform an operation that would cause an internal stack to underflow.");
	}
}
