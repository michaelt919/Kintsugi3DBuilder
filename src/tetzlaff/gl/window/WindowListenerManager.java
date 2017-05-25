package tetzlaff.gl.window;

import tetzlaff.gl.window.listeners.CharacterListener;
import tetzlaff.gl.window.listeners.CharacterModifiersListener;
import tetzlaff.gl.window.listeners.CursorEnteredListener;
import tetzlaff.gl.window.listeners.CursorExitedListener;
import tetzlaff.gl.window.listeners.CursorPositionListener;
import tetzlaff.gl.window.listeners.FramebufferSizeListener;
import tetzlaff.gl.window.listeners.KeyPressListener;
import tetzlaff.gl.window.listeners.KeyReleaseListener;
import tetzlaff.gl.window.listeners.KeyRepeatListener;
import tetzlaff.gl.window.listeners.MouseButtonPressListener;
import tetzlaff.gl.window.listeners.MouseButtonReleaseListener;
import tetzlaff.gl.window.listeners.ScrollListener;
import tetzlaff.gl.window.listeners.WindowCloseListener;
import tetzlaff.gl.window.listeners.WindowFocusGainedListener;
import tetzlaff.gl.window.listeners.WindowFocusLostListener;
import tetzlaff.gl.window.listeners.WindowIconifiedListener;
import tetzlaff.gl.window.listeners.WindowPositionListener;
import tetzlaff.gl.window.listeners.WindowRefreshListener;
import tetzlaff.gl.window.listeners.WindowRestoredListener;
import tetzlaff.gl.window.listeners.WindowSizeListener;

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
