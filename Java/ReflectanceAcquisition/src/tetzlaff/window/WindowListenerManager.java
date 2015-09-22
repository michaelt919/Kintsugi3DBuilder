package tetzlaff.window;

import tetzlaff.window.listeners.CharacterListener;
import tetzlaff.window.listeners.CharacterModifiersListener;
import tetzlaff.window.listeners.CursorEnteredListener;
import tetzlaff.window.listeners.CursorExitedListener;
import tetzlaff.window.listeners.CursorPositionListener;
import tetzlaff.window.listeners.FramebufferSizeListener;
import tetzlaff.window.listeners.KeyPressListener;
import tetzlaff.window.listeners.KeyReleaseListener;
import tetzlaff.window.listeners.KeyRepeatListener;
import tetzlaff.window.listeners.MouseButtonPressListener;
import tetzlaff.window.listeners.MouseButtonReleaseListener;
import tetzlaff.window.listeners.ScrollListener;
import tetzlaff.window.listeners.WindowCloseListener;
import tetzlaff.window.listeners.WindowFocusGainedListener;
import tetzlaff.window.listeners.WindowFocusLostListener;
import tetzlaff.window.listeners.WindowIconifiedListener;
import tetzlaff.window.listeners.WindowPositionListener;
import tetzlaff.window.listeners.WindowRefreshListener;
import tetzlaff.window.listeners.WindowRestoredListener;
import tetzlaff.window.listeners.WindowSizeListener;

/**
 * An interface for an object which can register listeners for window events.
 * @author Michael Tetzlaff
 *
 */
public interface WindowListenerManager 
{
	/**
	 * Adds a listener for when the window is moved.
	 * @param listener The listener to add.
	 */
	void addWindowPositionListener(WindowPositionListener listener);

	/**
	 * Adds a listener for when the window is resized.
	 * @param listener The listener to add.
	 */
	void addWindowSizeListener(WindowSizeListener listener);

	/**
	 * Adds a listener for when the window is closed.
	 * @param listener The listener to add.
	 */
	void addWindowCloseListener(WindowCloseListener listener);

	/**
	 * Adds a listener for when the window is refreshed.
	 * @param listener The listener to add.
	 */
	void addWindowRefreshListener(WindowRefreshListener listener);

	/**
	 * Adds a listener for when the window loses focus.
	 * @param listener The listener to add.
	 */
	void addWindowFocusLostListener(WindowFocusLostListener listener);

	/**
	 * Adds a listener for when the window gains focus.
	 * @param listener The listener to add.
	 */
	void addWindowFocusGainedListener(WindowFocusGainedListener listener);

	/**
	 * Adds a listener for when the window is "iconified" or "minimized."
	 * @param listener The listener to add.
	 */
	void addWindowIconifiedListener(WindowIconifiedListener listener);
	
	/**
	 * Adds a listener for when the window is restored from an iconified state.
	 * @param listener The listener to add.
	 */
	void addWindowRestoredListener(WindowRestoredListener listener);

	/**
	 * Adds a listener for when the size of the default framebuffer changes.
	 * @param listener The listener to add.
	 */
	void addFramebufferSizeListener(FramebufferSizeListener listener);

	/**
	 * Adds a listener for when a key is pressed.
	 * @param listener The listener to add.
	 */
	void addKeyPressListener(KeyPressListener listener);

	/**
	 * Adds a listener for when a key is released.
	 * @param listener The listener to add.
	 */
	void addKeyReleaseListener(KeyReleaseListener listener);

	/**
	 * Adds a listener for when a key is repeated.
	 * @param listener The listener to add.
	 */
	void addKeyRepeatListener(KeyRepeatListener listener);

	/**
	 * Adds a listener for when a character key is typed.
	 * @param listener The listener to add.
	 */
	void addCharacterListener(CharacterListener listener);

	/**
	 * Adds a listener for when a character key is typed with modifiers.
	 * @param listener The listener to add.
	 */
	void addCharacterModifiersListener(CharacterModifiersListener listener);

	/**
	 * Adds a listener for when a mouse button is pressed.
	 * @param listener The listener to add.
	 */
	void addMouseButtonPressListener(MouseButtonPressListener listener);

	/**
	 * Adds a listener for when a mouse button is released.
	 * @param listener The listener to add.
	 */
	void addMouseButtonReleaseListener(MouseButtonReleaseListener listener);

	/**
	 * Adds a listener for when the cursor moves.
	 * @param listener The listener to add.
	 */
	void addCursorPositionListener(CursorPositionListener listener);

	/**
	 * Adds a listener for when the cursor enters the window area.
	 * @param listener The listener to add.
	 */
	void addCursorEnteredListener(CursorEnteredListener listener);

	/**
	 * Adds a listener for when the cursor exits the window area.
	 * @param listener The listener to add.
	 */
	void addCursorExitedListener(CursorExitedListener listener);

	/**
	 * Adds a listener for when the scroll wheel is turned.
	 * @param listener The listener to add.
	 */
	void addScrollListener(ScrollListener listener);
}
