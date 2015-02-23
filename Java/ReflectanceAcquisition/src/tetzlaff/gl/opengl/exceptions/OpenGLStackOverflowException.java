package tetzlaff.gl.opengl.exceptions;

public class OpenGLStackOverflowException extends OpenGLException 
{
	private static final long serialVersionUID = -6479066172793823172L;

	public OpenGLStackOverflowException() 
	{
		super("An attempt has been made to perform an operation that would cause an internal stack to overflow.");
	}
}
