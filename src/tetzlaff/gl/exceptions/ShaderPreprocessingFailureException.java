package tetzlaff.gl.exceptions;

public class ShaderPreprocessingFailureException extends RuntimeException
{
    private static final long serialVersionUID = -6890750795431427594L;

    public ShaderPreprocessingFailureException()
    {
    }

    public ShaderPreprocessingFailureException(String message)
    {
        super(message);
    }

    public ShaderPreprocessingFailureException(Throwable cause)
    {
        super(cause);
    }

    public ShaderPreprocessingFailureException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ShaderPreprocessingFailureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
