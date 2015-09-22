package tetzlaff.gl.exceptions;

/**
 * Thrown when an attempt is made to use a shader program that has not been linked (or failed to link).
 * @author Michael Tetzlaff
 *
 */
public class UnlinkedProgramException extends IllegalStateException 
{
	private static final long serialVersionUID = 7045695222299534322L;

	public UnlinkedProgramException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public UnlinkedProgramException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public UnlinkedProgramException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public UnlinkedProgramException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
