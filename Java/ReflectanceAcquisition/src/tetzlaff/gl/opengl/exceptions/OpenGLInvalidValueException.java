package tetzlaff.gl.opengl.exceptions;

public class OpenGLInvalidValueException extends OpenGLException 
{
	public OpenGLInvalidValueException() 
	{
		super("A numeric argument is out of range.");
	}
}
