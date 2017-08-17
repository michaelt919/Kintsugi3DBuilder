package tetzlaff.gl.exceptions;

public class ProgramLinkFailureException extends RuntimeException 
{
    private static final long serialVersionUID = 8084771613401908824L;

    public ProgramLinkFailureException()
    {
    }

    public ProgramLinkFailureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ProgramLinkFailureException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ProgramLinkFailureException(String message)
    {
        super(message);
    }

    public ProgramLinkFailureException(Throwable cause)
    {
        super(cause);
    }
}
