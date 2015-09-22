package tetzlaff.window;

/**
 * A class for representing the position of a window.
 * @author Michael Tetzlaff
 *
 */
public class WindowSize 
{
	/**
	 * The width of the window, in logical pixels.
	 */
	public final int width;
	
	/**
	 * The height of the window, in logical pixels.
	 */
	public final int height;
	
	/**
	 * Creates a new object for representing the size of a window.
	 * @param width The width of the window.
	 * @param height The height of the window.
	 */
	public WindowSize(int width, int height) 
	{
		this.width = width;
		this.height = height;
	}
}
