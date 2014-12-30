package tetzlaff.window.glfw;

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
import tetzlaff.window.listeners.ScrollListener;
import tetzlaff.window.listeners.MouseButtonReleaseListener;
import tetzlaff.window.listeners.WindowCloseListener;
import tetzlaff.window.listeners.WindowFocusGainedListener;
import tetzlaff.window.listeners.WindowFocusLostListener;
import tetzlaff.window.listeners.WindowIconifiedListener;
import tetzlaff.window.listeners.WindowPositionListener;
import tetzlaff.window.listeners.WindowRefreshListener;
import tetzlaff.window.listeners.WindowRestoredListener;
import tetzlaff.window.listeners.WindowSizeListener;

public interface WindowListenerManager 
{
	void addWindowPositionListener(WindowPositionListener listener);

	void addWindowSizeListener(WindowSizeListener listener);

	void addWindowCloseListener(WindowCloseListener listener);

	void addWindowRefreshListener(WindowRefreshListener listener);

	void addWindowFocusLostListener(WindowFocusLostListener listener);

	void addWindowFocusGainedListener(WindowFocusGainedListener listener);

	void addWindowIconifiedListener(WindowIconifiedListener listener);
	
	void addWindowRestoredListener(WindowRestoredListener listener);

	void addFramebufferSizeListener(FramebufferSizeListener listener);

	void addKeyPressListener(KeyPressListener listener);

	void addKeyReleaseListener(KeyReleaseListener listener);

	void addKeyRepeatListener(KeyRepeatListener listener);

	void addCharacterListener(CharacterListener listener);

	void addCharacterModifiersListener(CharacterModifiersListener listener);

	void addMouseButtonPressListener(MouseButtonPressListener listener);

	void addMouseButtonReleaseListener(MouseButtonReleaseListener listener);

	void addCursorPositionListener(CursorPositionListener listener);

	void addCursorEnteredListener(CursorEnteredListener listener);

	void addCursorExitedListener(CursorExitedListener listener);

	void addScrollListener(ScrollListener listener);
}
