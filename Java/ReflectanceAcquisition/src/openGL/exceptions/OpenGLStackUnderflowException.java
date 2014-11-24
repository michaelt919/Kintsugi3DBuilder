package openGL.exceptions;

public class OpenGLStackUnderflowException extends OpenGLException 
{
	public OpenGLStackUnderflowException() 
	{
		super("An attempt has been made to perform an operation that would cause an internal stack to underflow.");
	}
}
