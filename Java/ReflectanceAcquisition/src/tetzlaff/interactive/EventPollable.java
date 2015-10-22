package tetzlaff.interactive;

/**
 * An interface for an object that can be polled for events.
 * This can be used within a program loop to fire the handlers for any events that occurred.
 * @author Michael Tetzlaff
 *
 */
public interface EventPollable 
{
	/**
	 * Polls for events.
	 * This will cause the handlers to be fired for any events that have occurred since the last polling.
	 */
	void pollEvents();
	
	/**
	 * Gets whether or not the event loop should terminate.
	 * @return true if the event loop should terminate, false otherwise.
	 */
	boolean shouldTerminate();
}
