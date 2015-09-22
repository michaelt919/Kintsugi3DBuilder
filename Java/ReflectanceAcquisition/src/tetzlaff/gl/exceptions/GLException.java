package tetzlaff.gl.exceptions;

/**
 * Thrown when any GL-related error occurs (including non-fatal errors), and the superclass of several more specific exceptions.
 * @author Michael Tetzlaff
 *
 */
public class GLException extends RuntimeException 
{
	private static final long serialVersionUID = -8134219559617090676L;

	public GLException() 
	{
		super();
	}

	public GLException(String message) 
	{
		super(message);
	}

}
