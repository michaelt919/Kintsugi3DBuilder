package tetzlaff.gl.exceptions;

public class GLInvalidEnumException extends GLException 
{
    private static final long serialVersionUID = 1901610618342735977L;

    public GLInvalidEnumException()
    {
        super("An unacceptable value is specified for an enumerated argument.");
    }
}
