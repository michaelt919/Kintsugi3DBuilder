package tetzlaff.window;

/**
 * A class for representing the position of the cursor within the window
 * @author Michael Tetzlaff
 *
 */
public class CursorPosition 
{
	/**
	 * The x-coordinate of the cursor position, in screen coordinates.
	 */
	public final double x;
	
	/**
	 * The y-coordinate of the cursor position, in screen coordinates.
	 */
	public final double y;

	/**
	 * Creates a new object for representing the cursor position.
	 * @param x The x-coordinate of the cursor position, in screen coordinates.
	 * @param y The y-coordinate of the cursor position, in screen coordinates.
	 */
	public CursorPosition(double x, double y) 
	{
		this.x = x;
		this.y = y;
	}
}
