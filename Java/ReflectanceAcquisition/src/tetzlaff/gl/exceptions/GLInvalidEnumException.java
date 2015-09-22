package tetzlaff.gl.exceptions;

/**
 * Thrown when an unacceptable value is specified for an enumerated argument.
 * This is most likely the result of the GL implementation not supporting a particular enumerated option.
 * @author Michael Tetzlaff
 *
 */
public class GLInvalidEnumException extends GLException 
{
	private static final long serialVersionUID = 1901610618342735977L;

	public GLInvalidEnumException() 
	{
		super("An unacceptable value is specified for an enumerated argument.");
	}
}
