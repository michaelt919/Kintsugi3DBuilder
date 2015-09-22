package tetzlaff.window;

/**
 * A class for representing the states of all modifier keys.
 * @author Michael Tetzlaff
 *
 */
public interface ModifierKeys
{
	/**
	 * Gets whether the shift modifier is active.
	 * @return true if the shift modifier is active, false otherwise.
	 */
	boolean getShiftModifier();
	
	/**
	 * Gets whether the control/ctrl modifier is active.
	 * @return true if the control modifier is active, false otherwise.
	 */
	boolean getControlModifier();
	
	/**
	 * Gets whether the alt modifier is active.
	 * @return true if the alt modifier is active, false otherwise.
	 */
	boolean getAltModifier();
	
	/**
	 * Gets whether the "super" modifier is active.
	 * @return true if the super modifier is active, false otherwise.
	 */
	boolean getSuperModifier();
}
