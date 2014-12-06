package tetzlaff.gl.opengl.exceptions;

public class OpenGLStackOverflowException extends OpenGLException 
{
	public OpenGLStackOverflowException() 
	{
		super("An attempt has been made to perform an operation that would cause an internal stack to overflow.");
	}
}
