package tetzlaff.gl.opengl.exceptions;

public class OpenGLInvalidOperationException extends OpenGLException 
{
	public OpenGLInvalidOperationException() 
	{
		super("The specified operation is not allowed in the current state.");
	}
}
