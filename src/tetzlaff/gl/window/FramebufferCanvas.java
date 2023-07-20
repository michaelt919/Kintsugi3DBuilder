/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.gl.window;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.DoubleFramebufferObject;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.SwapObservable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class FramebufferCanvas<ContextType extends Context<ContextType>>
    extends CanvasBase<ContextType>
    implements ControllableCanvas3D<ContextType>, PollableCanvas3D<ContextType>, SwapObservable<Framebuffer<ContextType>>
{
    private final DoubleFramebufferObject<ContextType> framebuffer;

    private CanvasSize canvasSize;
    private CanvasPosition canvasPosition;

    private final Map<Key, KeyState> keyStates = new EnumMap<>(Key.class);
    private ModifierKeys modifierKeys = ModifierKeys.NONE;

    private static final int BUTTON_COUNT = 3;
    private final MouseButtonState[] buttonStates = new MouseButtonState[BUTTON_COUNT]; // left, right, middle

    private CursorPosition cursorPosition = new CursorPosition(0, 0);

    private final EventCollector eventCollector = new EventCollector();

    private boolean terminating = false;

    public static <ContextType extends Context<ContextType>> FramebufferCanvas<ContextType>
        createUsingExistingFramebuffer(DoubleFramebufferObject<ContextType> framebuffer)
    {
        return new FramebufferCanvas<>(framebuffer);
    }

    private FramebufferCanvas(DoubleFramebufferObject<ContextType> framebuffer)
    {
        this.framebuffer = framebuffer;
        Arrays.fill(buttonStates, MouseButtonState.RELEASED);
    }

    @Override
    public void addSwapListener(Consumer<Framebuffer<ContextType>> listener)
    {
        this.framebuffer.addSwapListener(listener);
    }

    @Override
    protected WindowListenerManager getListenerManager()
    {
        return eventCollector.getListenerManager();
    }

    @Override
    public void pollEvents()
    {
        eventCollector.pollEvents();
    }

    @Override
    public boolean shouldTerminate()
    {
        return terminating;
    }

    public void requestTerminate()
    {
        this.terminating = true;
    }

    public void cancelTerminate()
    {
        this.terminating = false;
    }

    @Override
    public ContextType getContext()
    {
        return framebuffer.getContext();
    }

    @Override
    public boolean isHighDPI()
    {
        return false;
    }

    @Override
    public CanvasSize getSize()
    {
        return canvasSize;
    }

    @Override
    public CanvasPosition getPosition()
    {
        return canvasPosition;
    }

    @Override
    public MouseButtonState getMouseButtonState(int buttonIndex)
    {
        return buttonIndex < BUTTON_COUNT ? buttonStates[buttonIndex] : MouseButtonState.RELEASED;
    }

    @Override
    public KeyState getKeyState(Key key)
    {
        return keyStates.getOrDefault(key, KeyState.RELEASED);
    }

    @Override
    public CursorPosition getCursorPosition()
    {
        return cursorPosition;
    }

    @Override
    public ModifierKeys getModifierKeys()
    {
        return modifierKeys;
    }

    @Override
    public void pressMouseButton(int buttonIndex, CursorPosition position, ModifierKeys modifierKeys)
    {
        this.modifierKeys = modifierKeys;
        this.cursorPosition = position;

        if (buttonIndex < BUTTON_COUNT)
        {
            this.buttonStates[buttonIndex] = MouseButtonState.PRESSED;
        }

        eventCollector.mouseButtonPress(l -> l.mouseButtonPressed(this, buttonIndex, modifierKeys));
    }

    @Override
    public void releaseMouseButton(int buttonIndex, CursorPosition position, ModifierKeys modifierKeys)
    {
        this.modifierKeys = modifierKeys;
        this.cursorPosition = position;

        if (buttonIndex < BUTTON_COUNT)
        {
            this.buttonStates[buttonIndex] = MouseButtonState.RELEASED;
        }

        eventCollector.mouseButtonRelease(l -> l.mouseButtonReleased(this, buttonIndex, modifierKeys));
    }

    @Override
    public void moveCursor(CursorPosition position, ModifierKeys modifierKeys)
    {
        this.modifierKeys = modifierKeys;
        this.cursorPosition = position;
        eventCollector.cursorPos(l -> l.cursorMoved(this, position.x, position.y));
    }

    @Override
    public void cursorEnter(CursorPosition position, ModifierKeys modifierKeys)
    {
        this.modifierKeys = modifierKeys;
        this.cursorPosition = position;
        eventCollector.cursorEnter(l -> l.cursorEntered(this));
    }

    @Override
    public void cursorExit(CursorPosition position, ModifierKeys modifierKeys)
    {
        this.modifierKeys = modifierKeys;
        this.cursorPosition = position;
        eventCollector.cursorExit(l -> l.cursorExited(this));
    }

    @Override
    public void scroll(double xOffset, double yOffset)
    {
        eventCollector.scroll(l -> l.scroll(this, xOffset, yOffset));
    }

    @Override
    public void pressKey(Key key, ModifierKeys modifierKeys)
    {
        this.modifierKeys = modifierKeys;
        keyStates.put(key, KeyState.PRESSED);
        eventCollector.keyPress(l -> l.keyPressed(this, key, modifierKeys));
    }

    @Override
    public void releaseKey(Key key, ModifierKeys modifierKeys)
    {
        this.modifierKeys = modifierKeys;
        keyStates.put(key, KeyState.RELEASED);
        eventCollector.keyRelease(l -> l.keyReleased(this, key, modifierKeys));
    }

    @Override
    public void typeKey(Key key, ModifierKeys modifierKeys)
    {
        this.modifierKeys = modifierKeys;
        eventCollector.keyType(l -> l.keyTyped(this, key, modifierKeys));
    }

    @Override
    public void typeCharacter(char character, ModifierKeys modifierKeys)
    {
        eventCollector.character(l -> l.characterTyped(this, character));
        eventCollector.charMods(l -> l.characterTypedWithModifiers(this, character, modifierKeys));
    }

    @Override
    public void changeBounds(CanvasPosition position, CanvasSize size)
    {
        framebuffer.requestResize(size.width, size.height);
        eventCollector.canvasSize(l -> l.canvasResized(this, size.width, size.height));
        eventCollector.framebufferSize(l -> l.framebufferResized(this, size.width, size.height));
        canvasSize = size;

        if (canvasPosition == null || position.x != canvasPosition.x || position.y != canvasPosition.y)
        {
            eventCollector.canvasPos(l -> l.canvasMoved(this, position.x, position.y));
            canvasPosition = position;
        }
    }

    @Override
    public void gainFocus()
    {
        eventCollector.windowFocusGained(l -> l.windowFocusGained(this));
    }

    @Override
    public void loseFocus()
    {
        eventCollector.windowFocusLost(l -> l.windowFocusLost(this));
    }

    @Override
    public void iconify()
    {
        eventCollector.windowIconified(l1 -> l1.windowIconified(this));
    }

    @Override
    public void restore()
    {
        eventCollector.windowRestored(l1 -> l1.windowRestored(this));
    }

    @Override
    public void close()
    {
        eventCollector.windowClose(l -> l.windowClosing(this));
    }
}
