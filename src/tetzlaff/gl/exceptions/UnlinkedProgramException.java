package tetzlaff.gl.exceptions;

public class UnlinkedProgramException extends IllegalStateException 
{
    private static final long serialVersionUID = 7045695222299534322L;

    public UnlinkedProgramException()
    {
        super();
    }

    public UnlinkedProgramException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UnlinkedProgramException(String message)
    {
        super(message);
    }

    public UnlinkedProgramException(Throwable cause)
    {
        super(cause);
    }
}
