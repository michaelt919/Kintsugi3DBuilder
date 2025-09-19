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

import kintsugi3d.gl.window.listeners.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

class EventCollector
{
    private static final Logger LOG = LoggerFactory.getLogger(EventCollector.class);

    private final Queue<Consumer<CanvasPositionListener>> canvasPos = new LinkedList<>();
    private final Queue<Consumer<CanvasSizeListener>> canvasSize = new LinkedList<>();
    private final Queue<Consumer<WindowCloseListener>> windowClose = new LinkedList<>();
    private final Queue<Consumer<CanvasRefreshListener>> canvasRefresh = new LinkedList<>();
    private final Queue<Consumer<WindowFocusLostListener>> windowFocusLost = new LinkedList<>();
    private final Queue<Consumer<WindowFocusGainedListener>> windowFocusGained = new LinkedList<>();
    private final Queue<Consumer<WindowIconifiedListener>> windowIconified = new LinkedList<>();
    private final Queue<Consumer<WindowRestoredListener>> windowRestored = new LinkedList<>();
    private final Queue<Consumer<FramebufferSizeListener>> framebufferSize = new LinkedList<>();
    private final Queue<Consumer<KeyPressListener>> keyPress = new LinkedList<>();
    private final Queue<Consumer<KeyReleaseListener>> keyRelease = new LinkedList<>();
    private final Queue<Consumer<KeyTypeListener>> keyType = new LinkedList<>();
    private final Queue<Consumer<CharacterListener>> character = new LinkedList<>();
    private final Queue<Consumer<CharacterModifiersListener>> charMods = new LinkedList<>();
    private final Queue<Consumer<MouseButtonPressListener>> mouseButtonPress = new LinkedList<>();
    private final Queue<Consumer<MouseButtonReleaseListener>> mouseButtonRelease = new LinkedList<>();
    private final Queue<Consumer<CursorPositionListener>> cursorPos = new LinkedList<>();
    private final Queue<Consumer<CursorEnteredListener>> cursorEnter = new LinkedList<>();
    private final Queue<Consumer<CursorExitedListener>> cursorExit = new LinkedList<>();
    private final Queue<Consumer<ScrollListener>> scroll = new LinkedList<>();

    private final WindowListenerManagerInstance listenerManager = new WindowListenerManagerInstance();

    WindowListenerManager getListenerManager()
    {
        return listenerManager;
    }

    synchronized void canvasPos(Consumer<CanvasPositionListener> event)
    {
        this.canvasPos.add(event);
    }

    synchronized void canvasSize(Consumer<CanvasSizeListener> event)
    {
        this.canvasSize.add(event);
    }

    synchronized void windowClose(Consumer<WindowCloseListener> event)
    {
        this.windowClose.add(event);
    }

    synchronized void canvasRefresh(Consumer<CanvasRefreshListener> event)
    {
        this.canvasRefresh.add(event);
    }

    synchronized void windowFocusLost(Consumer<WindowFocusLostListener> event)
    {
        this.windowFocusLost.add(event);
    }

    synchronized void windowFocusGained(Consumer<WindowFocusGainedListener> event)
    {
        this.windowFocusGained.add(event);
    }

    synchronized void windowIconified(Consumer<WindowIconifiedListener> event)
    {
        this.windowIconified.add(event);
    }

    synchronized void windowRestored(Consumer<WindowRestoredListener> event)
    {
        this.windowRestored.add(event);
    }

    synchronized void framebufferSize(Consumer<FramebufferSizeListener> event)
    {
        this.framebufferSize.add(event);
    }

    synchronized void keyPress(Consumer<KeyPressListener> event)
    {
        this.keyPress.add(event);
    }

    synchronized void keyRelease(Consumer<KeyReleaseListener> event)
    {
        this.keyRelease.add(event);
    }

    synchronized void keyType(Consumer<KeyTypeListener> event)
    {
        this.keyType.add(event);
    }

    synchronized void character(Consumer<CharacterListener> event)
    {
        this.character.add(event);
    }

    synchronized void charMods(Consumer<CharacterModifiersListener> event)
    {
        this.charMods.add(event);
    }

    synchronized void mouseButtonPress(Consumer<MouseButtonPressListener> event)
    {
        this.mouseButtonPress.add(event);
    }

    synchronized void mouseButtonRelease(Consumer<MouseButtonReleaseListener> event)
    {
        this.mouseButtonRelease.add(event);
    }

    synchronized void cursorPos(Consumer<CursorPositionListener> event)
    {
        this.cursorPos.add(event);
    }

    synchronized void cursorEnter(Consumer<CursorEnteredListener> event)
    {
        this.cursorEnter.add(event);
    }

    synchronized void cursorExit(Consumer<CursorExitedListener> event)
    {
        this.cursorExit.add(event);
    }

    synchronized void scroll(Consumer<ScrollListener> event)
    {
        this.scroll.add(event);
    }

    private synchronized <L> void pollEvents(Queue<Consumer<L>> eventQueue, Iterable<L> listeners)
    {
        while(!eventQueue.isEmpty())
        {
            Consumer<L> event = eventQueue.poll();
            for (L l : listeners)
            {
                if (event == null)
                {
                    LOG.warn("Event was null", new Throwable());
                }
                else
                {
                    try
                    {
                        event.accept(l);
                    }
                    catch(Exception e)
                    {
                        LOG.error("An error occurred while polling events", e);
                    }
                }
            }
        }
    }

    void pollEvents()
    {
        pollEvents(canvasPos, listenerManager.getCanvasPosListeners());
        pollEvents(canvasSize, listenerManager.getCanvasSizeListeners());
        pollEvents(windowClose, listenerManager.getWindowCloseListeners());
        pollEvents(canvasRefresh, listenerManager.getCanvasRefreshListeners());
        pollEvents(windowFocusLost, listenerManager.getWindowFocusLostListeners());
        pollEvents(windowFocusGained, listenerManager.getWindowFocusGainedListeners());
        pollEvents(windowIconified, listenerManager.getWindowIconifiedListeners());
        pollEvents(windowRestored, listenerManager.getWindowRestoredListeners());
        pollEvents(framebufferSize, listenerManager.getFramebufferSizeListeners());
        pollEvents(keyPress, listenerManager.getKeyPressListeners());
        pollEvents(keyRelease, listenerManager.getKeyReleaseListeners());
        pollEvents(keyType, listenerManager.getKeyTypeListeners());
        pollEvents(character, listenerManager.getCharacterListeners());
        pollEvents(charMods, listenerManager.getCharModsListeners());
        pollEvents(mouseButtonPress, listenerManager.getMouseButtonPressListeners());
        pollEvents(mouseButtonRelease, listenerManager.getMouseButtonReleaseListeners());
        pollEvents(cursorPos, listenerManager.getCursorPosListeners());
        pollEvents(cursorEnter, listenerManager.getCursorEnterListeners());
        pollEvents(cursorExit, listenerManager.getCursorExitListeners());
        pollEvents(scroll, listenerManager.getScrollListeners());
    }
}
