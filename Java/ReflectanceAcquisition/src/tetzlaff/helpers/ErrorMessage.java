package tetzlaff.helpers;

/**
 * Represents a "user-friendly" error message for when something goes wrong.
 * @author Michael Tetzlaff
 *
 */
public class ErrorMessage 
{
	/**
	 * The title of the error.
	 */
	public final String title;
	
	/**
	 * The message containing a detailed description of the error.
	 */
	public final String message;
	
	/**
	 * Creates a new error message.
	 * @param title The title of the error.
	 * @param message The message containing a detailed description of the error.
	 */
	public ErrorMessage(String title, String message)
	{
		this.title = title;
		this.message = message;
	}
}
