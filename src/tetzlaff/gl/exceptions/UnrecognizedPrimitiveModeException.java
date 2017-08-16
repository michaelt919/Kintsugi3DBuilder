package tetzlaff.gl.exceptions;

public class UnrecognizedPrimitiveModeException extends RuntimeException
{

    private static final long serialVersionUID = -6087408994505500419L;

    public UnrecognizedPrimitiveModeException()
    {
    }

    public UnrecognizedPrimitiveModeException(String message)
    {
        super(message);
    }

    public UnrecognizedPrimitiveModeException(Throwable cause)
    {
        super(cause);
    }

    public UnrecognizedPrimitiveModeException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UnrecognizedPrimitiveModeException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
