package tetzlaff.gl.exceptions;

public class UncompiledShaderException extends IllegalStateException 
{
    private static final long serialVersionUID = 323471236897721345L;

    public UncompiledShaderException()
    {
        super();
    }

    public UncompiledShaderException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UncompiledShaderException(String message)
    {
        super(message);
    }

    public UncompiledShaderException(Throwable cause)
    {
        super(cause);
    }
}
