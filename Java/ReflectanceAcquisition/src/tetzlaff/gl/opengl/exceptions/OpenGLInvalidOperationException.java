package tetzlaff.gl.opengl.exceptions;

public class OpenGLInvalidOperationException extends OpenGLException 
{
	private static final long serialVersionUID = 4614535430818755323L;

	public OpenGLInvalidOperationException() 
	{
		super("The specified operation is not allowed in the current state.");
	}
}
