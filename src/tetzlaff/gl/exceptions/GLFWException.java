package tetzlaff.gl.exceptions;

public class GLFWException extends RuntimeException 
{
    private static final long serialVersionUID = -9045950594133572264L;

    public GLFWException()
    {
    }

    public GLFWException(String message)
    {
        super(message);
    }

    public GLFWException(Throwable cause)
    {
        super(cause);
    }

    public GLFWException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public GLFWException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
