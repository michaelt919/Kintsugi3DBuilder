package tetzlaff.gl.window;

import tetzlaff.gl.Context;

public interface Window<ContextType extends Context<ContextType>> extends WindowListenerManager, AutoCloseable
{
    ContextType getContext();

    void show();

    void hide();

    void focus();

    boolean isHighDPI();

    boolean isWindowClosing();

    void requestWindowClose();

    void cancelWindowClose();

    boolean isResourceClosed();

    @Override
    void close();

    WindowSize getWindowSize();

    WindowPosition getWindowPosition();

    void setWindowTitle(String title);

    void setWindowSize(int width, int height);

    void setWindowPosition(int x, int y);

    MouseButtonState getMouseButtonState(int buttonIndex);

    KeyState getKeyState(int keycode);

    CursorPosition getCursorPosition();

    ModifierKeys getModifierKeys();

    boolean isFocused();
}
