package tetzlaff.gl.exceptions;

public class InvalidProgramException extends RuntimeException 
{
    private static final long serialVersionUID = 5646195535156170921L;

    public InvalidProgramException()
    {
        super();
    }

    public InvalidProgramException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InvalidProgramException(String message)
    {
        super(message);
    }

    public InvalidProgramException(Throwable cause)
    {
        super(cause);
    }
}
