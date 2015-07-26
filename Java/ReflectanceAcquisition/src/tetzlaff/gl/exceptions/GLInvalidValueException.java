package tetzlaff.gl.exceptions;

public class GLInvalidValueException extends GLException 
{
	private static final long serialVersionUID = 4250451180152106370L;

	public GLInvalidValueException() 
	{
		super("A numeric argument is out of range.");
	}
}
