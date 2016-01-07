package tetzlaff.gl.exceptions;

public class NoAvailableTextureUnitsException extends RuntimeException {

	private static final long serialVersionUID = -3881373761882575026L;

	public NoAvailableTextureUnitsException() {
	}

	public NoAvailableTextureUnitsException(String message) 
	{
		super(message);
	}

	public NoAvailableTextureUnitsException(Throwable cause) 
	{
		super(cause);
	}

	public NoAvailableTextureUnitsException(String message, Throwable cause) 
	{
		super(message, cause);
	}

	public NoAvailableTextureUnitsException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) 
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
