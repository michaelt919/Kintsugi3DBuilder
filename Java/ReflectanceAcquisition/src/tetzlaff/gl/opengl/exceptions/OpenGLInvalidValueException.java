package tetzlaff.gl.opengl.exceptions;

public class OpenGLInvalidValueException extends OpenGLException 
{
	private static final long serialVersionUID = 4250451180152106370L;

	public OpenGLInvalidValueException() 
	{
		super("A numeric argument is out of range.");
	}
}
