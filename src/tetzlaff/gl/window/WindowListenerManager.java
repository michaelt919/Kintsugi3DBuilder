package tetzlaff.gl.window;

import tetzlaff.gl.window.listeners.*;

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
