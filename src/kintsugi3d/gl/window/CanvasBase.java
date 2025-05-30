/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.window;

import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.window.listeners.*;

public abstract class CanvasBase<ContextType extends Context<ContextType>>
    implements Canvas3D<ContextType>
{
    protected abstract WindowListenerManager getListenerManager();

    @Override
    public void addCanvasPositionListener(CanvasPositionListener listener)
    {
        getListenerManager().addCanvasPositionListener(listener);
    }

    @Override
    public void addCanvasSizeListener(CanvasSizeListener listener)
    {
        getListenerManager().addCanvasSizeListener(listener);
    }

    @Override
    public void addWindowCloseListener(WindowCloseListener listener)
    {
        getListenerManager().addWindowCloseListener(listener);
    }

    @Override
    public void addCanvasRefreshListener(CanvasRefreshListener listener)
    {
        getListenerManager().addCanvasRefreshListener(listener);
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
