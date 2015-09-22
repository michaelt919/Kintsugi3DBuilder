package tetzlaff.gl.exceptions;

/**
 * Thrown when an attempt is made to use more texture units than are available in the GL implementation.
 * @author Michael Tetzlaff
 *
 */
public class NoAvailableTextureUnitsException extends RuntimeException {

	private static final long serialVersionUID = -3881373761882575026L;

	public NoAvailableTextureUnitsException() {
		// TODO Auto-generated constructor stub
	}

	public NoAvailableTextureUnitsException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public NoAvailableTextureUnitsException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public NoAvailableTextureUnitsException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public NoAvailableTextureUnitsException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
