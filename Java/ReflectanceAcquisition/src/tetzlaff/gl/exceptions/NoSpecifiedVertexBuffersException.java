package tetzlaff.gl.exceptions;

/**
 * Thrown when an attempt is made to draw something without any vertex buffers specified.
 * @author Michael Tetzlaff
 *
 */
public class NoSpecifiedVertexBuffersException extends RuntimeException {

	private static final long serialVersionUID = 6824841077784662947L;

	public NoSpecifiedVertexBuffersException() {
		// TODO Auto-generated constructor stub
	}

	public NoSpecifiedVertexBuffersException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public NoSpecifiedVertexBuffersException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public NoSpecifiedVertexBuffersException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public NoSpecifiedVertexBuffersException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
