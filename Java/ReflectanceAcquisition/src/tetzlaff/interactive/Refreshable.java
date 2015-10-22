package tetzlaff.interactive;

import java.io.File;

/**
 * An interface for an object that needs to be regularly refreshed.
 * @author Michael Tetzlaff
 *
 */
public interface Refreshable 
{
	/**
	 * Initializes the object.  This method should only ever be called once in an object's lifetime.
	 */
	void initialize();
	
	/**
	 * Refreshes the object.  This method should be called as often as possible until the object is destroyed.
	 */
	void refresh();
	
	/**
	 * Terminates the object.  Attempting to use an object after terminating it will have undefined results.
	 */
	void terminate();
	
	/**
	 * Requests that the object save a description of its current state to a particular file for debugging purposes.
	 * This request may be either handled by writing to the specified file in the requested format, or may be ignored.
	 * It is recommended that interactive graphics applications implement this method by saving a screenshot of the current framebuffer.
	 * @param fileFormat The desired format of the debug file to be written.
	 * @param file The debug file to write to.
	 */
	void requestDebugDump(String fileFormat, File file);
	
	/**
	 * Determines whether or not an error has occurred.
	 * @return true if an error has occurred, false otherwise.
	 */
	boolean hasError();
	
	/**
	 * Gets an exception representing the most recent error that has occurred, if any.
	 * Calling this method will attempt to reset the error state of the object.
	 * @return An exception representing the most error that has occurred, or null if no error has occurred.
	 */
	Exception getError();
}
