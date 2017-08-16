package tetzlaff.gl.exceptions;

public class GLOutOfMemoryException extends GLException 
{
    private static final long serialVersionUID = -1977989622291033976L;

    public GLOutOfMemoryException()
    {
        super("There is not enough memory left to execute the command.  The state of the GL is undefined.");
    }
}
