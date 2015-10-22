package tetzlaff.helpers;

/**
 * An interface for translating an exception that was thrown by an application into a user-friendly error message.
 * @author Michael Tetzlaff
 *
 */
public interface ExceptionTranslator 
{
	/**
	 * Translates an exception into a user-friendly error message.
	 * @param e The exception to be translated.
	 * @return The user-friendly error message.
	 */
	ErrorMessage translate(Exception e);
}
