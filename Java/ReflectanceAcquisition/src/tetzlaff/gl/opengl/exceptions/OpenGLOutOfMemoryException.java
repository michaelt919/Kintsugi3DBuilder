package tetzlaff.gl.opengl.exceptions;

public class OpenGLOutOfMemoryException extends OpenGLException 
{
	public OpenGLOutOfMemoryException() 
	{
		super("There is not enough memory left to execute the command.  The state of the GL is undefined.");
	}
}
