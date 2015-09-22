package tetzlaff.window;

/**
 * Enumerates the possible states of a key.
 * @author Michael Tetzlaff
 *
 */
public enum KeyState 
{
	/**
	 * The state of the key is unknown.
	 */
	Unknown, 
	
	/**
	 * The key is being pressed down.
	 */
	Pressed,
	
	/**
	 * The key is not being pressed down.
	 */
	Released
}
