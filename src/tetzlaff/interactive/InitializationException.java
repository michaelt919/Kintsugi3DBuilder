package tetzlaff.interactive;

public class InitializationException extends Exception
{
    private static final long serialVersionUID = -1576852861606321385L;

    public InitializationException()
    {
    }

    public InitializationException(String message)
    {
        super(message);
    }

    public InitializationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InitializationException(Throwable cause)
    {
        super(cause);
    }
}
