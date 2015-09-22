package tetzlaff.window;

/**
 * A class for representing the position of a window on the screen.
 * @author Michael Tetzlaff
 *
 */
public class WindowPosition 
{
	/**
	 * The x-coordinate of the window position, in logical pixels.
	 */
	public final int x;
	
	/**
	 * The y-coordinate of the window position, in logical pixels.
	 */
	public final int y;
	
	/**
	 * Creates a new object for representing a window position.
	 * @param x The x-coordinate of the window position.
	 * @param y The y-coordinate of the window position.
	 */
	public WindowPosition(int x, int y) 
	{
		this.x = x;
		this.y = y;
	}
}
