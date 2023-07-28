/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.window;

import java.util.ArrayList;
import java.util.List;

import kintsugi3d.gl.window.listeners.*;

public class WindowListenerManagerInstance implements WindowListenerManager
{
    private final List<CanvasPositionListener> canvasPosListeners;
    private final List<CanvasSizeListener> canvasSizeListeners;
    private final List<WindowCloseListener> windowCloseListeners;
    private final List<CanvasRefreshListener> canvasRefreshListeners;
    private final List<WindowFocusLostListener> windowFocusLostListeners;
    private final List<WindowFocusGainedListener> windowFocusGainedListeners;
    private final List<WindowIconifiedListener> windowIconifiedListeners;
    private final List<WindowRestoredListener> windowRestoredListeners;
    private final List<FramebufferSizeListener> framebufferSizeListeners;
    private final List<KeyPressListener> keyPressListeners;
    private final List<KeyReleaseListener> keyReleaseListeners;
    private final List<KeyTypeListener> keyTypeListeners;
    private final List<CharacterListener> characterListeners;
    private final List<CharacterModifiersListener> charModsListeners;
    private final List<MouseButtonPressListener> mouseButtonPressListeners;
    private final List<MouseButtonReleaseListener> mouseButtonReleaseListeners;
    private final List<CursorPositionListener> cursorPosListeners;
    private final List<CursorEnteredListener> cursorEnterListeners;
    private final List<CursorExitedListener> cursorExitListeners;
    private final List<ScrollListener> scrollListeners;

    public WindowListenerManagerInstance()
    {
        windowCloseListeners = new ArrayList<>();
        charModsListeners = new ArrayList<>();
        cursorExitListeners = new ArrayList<>();
        windowFocusGainedListeners = new ArrayList<>();
        framebufferSizeListeners = new ArrayList<>();
        scrollListeners = new ArrayList<>();
        mouseButtonPressListeners = new ArrayList<>();
        mouseButtonReleaseListeners = new ArrayList<>();
        characterListeners = new ArrayList<>();
        canvasPosListeners = new ArrayList<>();
        cursorPosListeners = new ArrayList<>();
        cursorEnterListeners = new ArrayList<>();
        windowIconifiedListeners = new ArrayList<>();
        keyReleaseListeners = new ArrayList<>();
        keyTypeListeners = new ArrayList<>();
        canvasRefreshListeners = new ArrayList<>();
        keyPressListeners = new ArrayList<>();
        windowRestoredListeners = new ArrayList<>();
        canvasSizeListeners = new ArrayList<>();
        windowFocusLostListeners = new ArrayList<>();
    }

    @Override
    public void addCanvasPositionListener(CanvasPositionListener listener)
    {
        canvasPosListeners.add(listener);
    }

    @Override
    public void addCanvasSizeListener(CanvasSizeListener listener)
    {
        canvasSizeListeners.add(listener);
    }

    @Override
    public void addWindowCloseListener(WindowCloseListener listener)
    {
        windowCloseListeners.add(listener);
    }

    @Override
    public void addCanvasRefreshListener(CanvasRefreshListener listener)
    {
        canvasRefreshListeners.add(listener);
    }

    @Override
    public void addWindowFocusLostListener(WindowFocusLostListener listener)
    {
        windowFocusLostListeners.add(listener);
    }

    @Override
    public void addWindowFocusGainedListener(WindowFocusGainedListener listener)
    {
        windowFocusGainedListeners.add(listener);
    }

    @Override
    public void addWindowIconifiedListener(WindowIconifiedListener listener)
    {
        windowIconifiedListeners.add(listener);
    }

    @Override
    public void addWindowRestoredListener(WindowRestoredListener listener)
    {
        windowRestoredListeners.add(listener);
    }

    @Override
    public void addFramebufferSizeListener(FramebufferSizeListener listener)
    {
        framebufferSizeListeners.add(listener);
    }

    @Override
    public void addKeyPressListener(KeyPressListener listener)
    {
        keyPressListeners.add(listener);
    }

    @Override
    public void addKeyReleaseListener(KeyReleaseListener listener)
    {
        keyReleaseListeners.add(listener);
    }

    @Override
    public void addKeyTypeListener(KeyTypeListener listener)
    {
        keyTypeListeners.add(listener);
    }

    @Override
    public void addCharacterListener(CharacterListener listener)
    {
        characterListeners.add(listener);
    }

    @Override
    public void addCharacterModifiersListener(CharacterModifiersListener listener)
    {
        charModsListeners.add(listener);
    }

    @Override
    public void addMouseButtonPressListener(MouseButtonPressListener listener)
    {
        mouseButtonPressListeners.add(listener);
    }

    @Override
    public void addMouseButtonReleaseListener(MouseButtonReleaseListener listener)
    {
        mouseButtonReleaseListeners.add(listener);
    }

    @Override
    public void addCursorPositionListener(CursorPositionListener listener)
    {
        cursorPosListeners.add(listener);
    }

    @Override
    public void addCursorEnteredListener(CursorEnteredListener listener)
    {
        cursorEnterListeners.add(listener);
    }

    @Override
    public void addCursorExitedListener(CursorExitedListener listener)
    {
        cursorExitListeners.add(listener);
    }

    @Override
    public void addScrollListener(ScrollListener listener)
    {
        scrollListeners.add(listener);
    }

    public List<CanvasPositionListener> getCanvasPosListeners()
    {
        return canvasPosListeners;
    }

    public List<CanvasSizeListener> getCanvasSizeListeners()
    {
        return canvasSizeListeners;
    }

    public List<WindowCloseListener> getWindowCloseListeners()
    {
        return windowCloseListeners;
    }

    public List<CanvasRefreshListener> getCanvasRefreshListeners()
    {
        return canvasRefreshListeners;
    }

    public List<WindowFocusLostListener> getWindowFocusLostListeners()
    {
        return windowFocusLostListeners;
    }

    public List<WindowFocusGainedListener> getWindowFocusGainedListeners()
    {
        return windowFocusGainedListeners;
    }

    public List<WindowIconifiedListener> getWindowIconifiedListeners()
    {
        return windowIconifiedListeners;
    }

    public List<WindowRestoredListener> getWindowRestoredListeners()
    {
        return windowRestoredListeners;
    }

    public List<FramebufferSizeListener> getFramebufferSizeListeners()
    {
        return framebufferSizeListeners;
    }

    public List<KeyPressListener> getKeyPressListeners()
    {
        return keyPressListeners;
    }

    public List<KeyReleaseListener> getKeyReleaseListeners()
    {
        return keyReleaseListeners;
    }

    public List<KeyTypeListener> getKeyTypeListeners()
    {
        return keyTypeListeners;
    }

    public List<CharacterListener> getCharacterListeners()
    {
        return characterListeners;
    }

    public List<CharacterModifiersListener> getCharModsListeners()
    {
        return charModsListeners;
    }

    public List<MouseButtonPressListener> getMouseButtonPressListeners()
    {
        return mouseButtonPressListeners;
    }

    public List<MouseButtonReleaseListener> getMouseButtonReleaseListeners()
    {
        return mouseButtonReleaseListeners;
    }

    public List<CursorPositionListener> getCursorPosListeners()
    {
        return cursorPosListeners;
    }

    public List<CursorEnteredListener> getCursorEnterListeners()
    {
        return cursorEnterListeners;
    }

    public List<CursorExitedListener> getCursorExitListeners()
    {
        return cursorExitListeners;
    }

    public List<ScrollListener> getScrollListeners()
    {
        return scrollListeners;
    }
}
