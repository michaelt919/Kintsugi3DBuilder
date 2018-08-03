package tetzlaff.gl.window;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.window.listeners.*;

public abstract class WindowBase<ContextType extends Context<ContextType>>
    implements Window<ContextType>
{
    protected abstract WindowListenerManager getListenerManager();

    @Override
    public void addWindowPositionListener(WindowPositionListener listener)
    {
        getListenerManager().addWindowPositionListener(listener);
    }

    @Override
    public void addWindowSizeListener(WindowSizeListener listener)
    {
        getListenerManager().addWindowSizeListener(listener);
    }

    @Override
    public void addWindowCloseListener(WindowCloseListener listener)
    {
        getListenerManager().addWindowCloseListener(listener);
    }

    @Override
    public void addWindowRefreshListener(WindowRefreshListener listener)
    {
        getListenerManager().addWindowRefreshListener(listener);
    }

    @Override
    public void addWindowFocusLostListener(WindowFocusLostListener listener)
    {
        getListenerManager().addWindowFocusLostListener(listener);
    }

    @Override
    public void addWindowFocusGainedListener(WindowFocusGainedListener listener)
    {
        getListenerManager().addWindowFocusGainedListener(listener);
    }

    @Override
    public void addWindowIconifiedListener(WindowIconifiedListener listener)
    {
        getListenerManager().addWindowIconifiedListener(listener);
    }

    @Override
    public void addWindowRestoredListener(WindowRestoredListener listener)
    {
        getListenerManager().addWindowRestoredListener(listener);
    }

    @Override
    public void addFramebufferSizeListener(FramebufferSizeListener listener)
    {
        getListenerManager().addFramebufferSizeListener(listener);
    }

    @Override
    public void addKeyPressListener(KeyPressListener listener)
    {
        getListenerManager().addKeyPressListener(listener);
    }

    @Override
    public void addKeyReleaseListener(KeyReleaseListener listener)
    {
        getListenerManager().addKeyReleaseListener(listener);
    }

    @Override
    public void addKeyTypeListener(KeyTypeListener listener)
    {
        getListenerManager().addKeyTypeListener(listener);
    }

    @Override
    public void addCharacterListener(CharacterListener listener)
    {
        getListenerManager().addCharacterListener(listener);
    }

    @Override
    public void addCharacterModifiersListener(CharacterModifiersListener listener)
    {
        getListenerManager().addCharacterModifiersListener(listener);
    }

    @Override
    public void addMouseButtonPressListener(MouseButtonPressListener listener)
    {
        getListenerManager().addMouseButtonPressListener(listener);
    }

    @Override
    public void addMouseButtonReleaseListener(MouseButtonReleaseListener listener)
    {
        getListenerManager().addMouseButtonReleaseListener(listener);
    }

    @Override
    public void addCursorPositionListener(CursorPositionListener listener)
    {
        getListenerManager().addCursorPositionListener(listener);
    }

    @Override
    public void addCursorEnteredListener(CursorEnteredListener listener)
    {
        getListenerManager().addCursorEnteredListener(listener);
    }

    @Override
    public void addCursorExitedListener(CursorExitedListener listener)
    {
        getListenerManager().addCursorExitedListener(listener);
    }

    @Override
    public void addScrollListener(ScrollListener listener)
    {
        getListenerManager().addScrollListener(listener);
    }
}
