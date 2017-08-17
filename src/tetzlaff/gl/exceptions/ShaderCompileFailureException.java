package tetzlaff.gl.exceptions;

public class ShaderCompileFailureException extends RuntimeException 
{
    private static final long serialVersionUID = 7556469381337373536L;

    public ShaderCompileFailureException()
    {
    }

    public ShaderCompileFailureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ShaderCompileFailureException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ShaderCompileFailureException(String message)
    {
        super(message);
    }

    public ShaderCompileFailureException(Throwable cause)
    {
        super(cause);
    }
}
