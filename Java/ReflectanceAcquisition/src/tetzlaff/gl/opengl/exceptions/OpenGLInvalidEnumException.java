package tetzlaff.gl.opengl.exceptions;

public class OpenGLInvalidEnumException extends OpenGLException 
{
	private static final long serialVersionUID = 1901610618342735977L;

	public OpenGLInvalidEnumException() 
	{
		super("An unacceptable value is specified for an enumerated argument.");
	}
}
