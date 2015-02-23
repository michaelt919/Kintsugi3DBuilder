package tetzlaff.gl.opengl.exceptions;

public class OpenGLOutOfMemoryException extends OpenGLException 
{
	private static final long serialVersionUID = -1977989622291033976L;

	public OpenGLOutOfMemoryException() 
	{
		super("There is not enough memory left to execute the command.  The state of the GL is undefined.");
	}
}
