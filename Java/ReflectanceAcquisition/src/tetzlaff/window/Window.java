package tetzlaff.window;

import tetzlaff.window.glfw.WindowListenerManager;

public interface Window extends WindowListenerManager
{
	void show();

	void hide();
	
	boolean isClosing();
	
	void requestClose();
	
	void cancelClose();

	WindowSize getWindowSize();

	WindowPosition getWindowPosition();

	void setWindowTitle(String title);

	void setWindowSize(int width, int height);

	void setWindowPosition(int x, int y);
	
	MouseButtonState getMouseButtonState(int buttonIndex);
	
	KeyState getKeyState(int keycode);
	
	CursorPosition getCursorPosition();
	
	ModifierKeys getModifierKeys();
}
