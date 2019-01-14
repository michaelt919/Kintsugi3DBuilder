package tetzlaff.gl.exceptions;

public class NoSpecifiedVertexBuffersException extends RuntimeException {

    private static final long serialVersionUID = 6824841077784662947L;

    public NoSpecifiedVertexBuffersException()
    {
    }

    public NoSpecifiedVertexBuffersException(String message)
    {
        super(message);
    }

    public NoSpecifiedVertexBuffersException(Throwable cause)
    {
        super(cause);
    }

    public NoSpecifiedVertexBuffersException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public NoSpecifiedVertexBuffersException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}