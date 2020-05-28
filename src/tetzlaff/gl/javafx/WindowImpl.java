/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.javafx;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.converter.CharacterStringConverter;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.DoubleFramebufferObject;
import tetzlaff.gl.core.FramebufferSize;
import tetzlaff.gl.window.*;

public class WindowImpl<ContextType extends Context<ContextType>>
    extends WindowBase<ContextType> implements PollableWindow<ContextType>
{
    private final ContextType context;

    private final Stage stage;
    private final Pane root;
    private final ImageView imageView;
    private final ChangeListener<Boolean> iconify;
    private WritableImage image;

    private boolean windowClosing = false;

    private WindowSize windowSize;
    private WindowPosition windowPosition;
    private boolean focused;

    private final Map<Key, KeyState> keyStates = new EnumMap<>(Key.class);
    private ModifierKeys modifierKeys = ModifierKeys.NONE;
    private MouseButtonState lmb = MouseButtonState.RELEASED;
    private MouseButtonState mmb = MouseButtonState.RELEASED;
    private MouseButtonState rmb = MouseButtonState.RELEASED;

    private CursorPosition cursorPosition = new CursorPosition(0, 0);

    private final EventCollector eventCollector = new EventCollector();

    private static class ModifierKeysInstance extends ModifierKeysBase
    {
        private final boolean shiftModifier;
        private final boolean controlModifier;
        private final boolean altModifier;
        private final boolean superModifier;

        ModifierKeysInstance(boolean shiftModifier, boolean controlModifier, boolean altModifier, boolean superModifier)
        {
            this.shiftModifier = shiftModifier;
            this.controlModifier = controlModifier;
            this.altModifier = altModifier;
            this.superModifier = superModifier;
        }

        @Override
        public boolean getShiftModifier()
        {
            return shiftModifier;
        }

        @Override
        public boolean getControlModifier()
        {
            return controlModifier;
        }

        @Override
        public boolean getAltModifier()
        {
            return altModifier;
        }

        @Override
        public boolean getSuperModifier()
        {
            return superModifier;
        }
    }

    WindowImpl(Stage primaryStage, ContextType context, DoubleFramebufferObject<ContextType> framebuffer, WindowSpecification windowSpec)
    {
        this.context = context;

        stage = new Stage();
        stage.initOwner(primaryStage);
        stage.setTitle(windowSpec.getTitle());
        stage.setResizable(windowSpec.isResizable());

        if (windowSpec.getX() >= 0)
        {
            stage.setX(windowSpec.getX());
        }

        if (windowSpec.getY() >= 0)
        {
            stage.setY(windowSpec.getY());
        }

        image = new WritableImage(windowSpec.getWidth(), windowSpec.getHeight());
        imageView = new ImageView(image);

        root = new StackPane(imageView);
        root.setPrefWidth(windowSpec.getWidth());
        root.setPrefHeight(windowSpec.getHeight());

        Scene scene = new Scene(root);
        stage.setScene(scene);

        framebuffer.addSwapListener(frontFBO ->
        {
            FramebufferSize size = frontFBO.getSize();
            int[] data = frontFBO.readColorBufferARGB(0);

            Platform.runLater(() ->
            {
                //noinspection FloatingPointEquality
                if (size.width != image.getWidth() || size.height != image.getHeight())
                {
                    image = new WritableImage(size.width, size.height);
                }

                image.getPixelWriter().setPixels(0, 0, size.width, size.height,
                    PixelFormat.getIntArgbInstance(), data, size.width * (size.height - 1), -size.width);

                imageView.setImage(image);
            });
        });

        ChangeListener<? super Number> windowSizeListener = (event, oldValue, newValue) ->
        {
            int width = (int) Math.round(root.getWidth());
            int height = (int) Math.round(root.getHeight());

            framebuffer.requestResize(width, height);
            eventCollector.windowSize(l -> l.windowResized(this, width, height));
            eventCollector.framebufferSize(l -> l.framebufferResized(this, width, height));
            windowSize = new WindowSize(width, height);
        };

        root.widthProperty().addListener(windowSizeListener);
        root.heightProperty().addListener(windowSizeListener);

        ChangeListener<? super Number> windowPosListener = (event, oldValue, newValue) ->
            eventCollector.windowPos(l ->
            {
                int width = (int) Math.round(stage.getX());
                int height = (int) Math.round(stage.getY());
                l.windowMoved(this, width, height);
                windowPosition = new WindowPosition(width, height);
            });

        stage.xProperty().addListener(windowPosListener);
        stage.yProperty().addListener(windowPosListener);

        stage.focusedProperty().addListener((event, oldValue, newValue) ->
        {
            if (oldValue && !newValue)
            {
                eventCollector.windowFocusLost(l -> l.windowFocusLost(this));
            }
            else if (newValue && !oldValue)
            {
                eventCollector.windowFocusGained(l -> l.windowFocusGained(this));
            }

            focused = stage.isFocused();
        });

        iconify = (event, oldValue, newValue) ->
        {
            if (oldValue && !newValue)
            {
                eventCollector.windowRestored(l -> l.windowRestored(this));
            }
            else if (newValue && !oldValue)
            {
                eventCollector.windowIconified(l -> l.windowIconified(this));
            }
        };

        primaryStage.iconifiedProperty().addListener(iconify);

        stage.setOnCloseRequest(event ->
        {
            eventCollector.windowClose(l -> l.windowClosing(this));
            event.consume();
        });

        scene.setOnKeyPressed(event ->
        {
            modifierKeys = new ModifierKeysInstance(
                event.isShiftDown(),
                event.isControlDown(),
                event.isAltDown(),
                event.isMetaDown());

            keyStates.put(KeyCodeMaps.codeToKey(event.getCode()), KeyState.PRESSED);

            eventCollector.keyPress(l -> l.keyPressed(this, KeyCodeMaps.codeToKey(event.getCode()), modifierKeys));
        });

        scene.setOnKeyReleased(event ->
        {
            modifierKeys = new ModifierKeysInstance(
                event.isShiftDown(),
                event.isControlDown(),
                event.isAltDown(),
                event.isMetaDown());

            keyStates.put(KeyCodeMaps.codeToKey(event.getCode()), KeyState.RELEASED);

            eventCollector.keyRelease(l -> l.keyReleased(this, KeyCodeMaps.codeToKey(event.getCode()), modifierKeys));
        });

        scene.setOnKeyTyped(event ->
        {
            modifierKeys = new ModifierKeysInstance(
                event.isShiftDown(),
                event.isControlDown(),
                event.isAltDown(),
                event.isMetaDown());

            eventCollector.keyType(l -> l.keyTyped(this,
                KeyCodeMaps.codeToKey(event.getCode()), modifierKeys));

            eventCollector.character(l -> l.characterTyped(this,
                new CharacterStringConverter().fromString(event.getCharacter())));

            eventCollector.charMods(l -> l.characterTypedWithModifiers(this,
                new CharacterStringConverter().fromString(event.getCharacter()), modifierKeys));
        });

        root.setOnMousePressed(event ->
        {
            handleMouseEvent(event);
            getButtonIndex(event.getButton()).ifPresent(
                index -> eventCollector.mouseButtonPress(l -> l.mouseButtonPressed(this, index, modifierKeys)));
        });

        root.setOnMouseReleased(event ->
        {
            handleMouseEvent(event);
            getButtonIndex(event.getButton()).ifPresent(
                index -> eventCollector.mouseButtonRelease(l -> l.mouseButtonReleased(this, index, modifierKeys)));
        });

        EventHandler<? super MouseEvent> mouseCursor = event ->
        {
            handleMouseEvent(event);
            eventCollector.cursorPos(l -> l.cursorMoved(this, event.getSceneX(), event.getSceneY()));
        };

        root.setOnMouseMoved(mouseCursor);
        root.setOnMouseDragged(mouseCursor);

        EventHandler<? super MouseEvent> mouseEnter = event ->
        {
            handleMouseEvent(event);
            eventCollector.cursorEnter(l -> l.cursorEntered(this));
        };

        root.setOnMouseEntered(mouseEnter);
        root.setOnMouseDragEntered(mouseEnter);

        EventHandler<? super MouseEvent> mouseExit = event ->
        {
            handleMouseEvent(event);
            eventCollector.cursorExit(l -> l.cursorExited(this));
        };

        root.setOnMouseExited(mouseExit);
        root.setOnMouseDragExited(mouseExit);

        root.setOnScroll(event -> eventCollector.scroll(
            l -> l.scroll(this, event.getDeltaX(), event.getDeltaY())));

        windowSize = new WindowSize((int)Math.round(root.getWidth()), (int)Math.round(root.getHeight()));
        windowPosition = new WindowPosition((int)Math.round(stage.getX()), (int)Math.round(stage.getY()));
        focused = stage.isFocused();
    }

    private static OptionalInt getButtonIndex(MouseButton button)
    {
        switch(button)
        {
            case PRIMARY: return OptionalInt.of(0);
            case SECONDARY: return OptionalInt.of(1);
            case MIDDLE: return OptionalInt.of(2);
            default: return OptionalInt.empty();
        }
    }

    private void handleMouseEvent(MouseEvent event)
    {
        modifierKeys = new ModifierKeysInstance(
            event.isShiftDown(),
            event.isControlDown(),
            event.isAltDown(),
            event.isMetaDown());

        lmb = event.isPrimaryButtonDown() ? MouseButtonState.PRESSED : MouseButtonState.RELEASED;
        mmb = event.isMiddleButtonDown() ? MouseButtonState.PRESSED : MouseButtonState.RELEASED;
        rmb = event.isSecondaryButtonDown() ? MouseButtonState.PRESSED : MouseButtonState.RELEASED;

        cursorPosition = new CursorPosition((int)Math.round(event.getSceneX()), (int)Math.round(event.getSceneY()));
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
        return windowClosing;
    }

    @Override
    public ContextType getContext()
    {
        return context;
    }

    @Override
    public void show()
    {
        Platform.runLater(stage::show);
    }

    @Override
    public void hide()
    {
        Platform.runLater(stage::hide);
    }

    @Override
    public void focus()
    {
        Platform.runLater(stage::requestFocus);
    }

    @Override
    public boolean isHighDPI()
    {
        return false;
    }

    @Override
    public boolean isWindowClosing()
    {
        return windowClosing;
    }

    @Override
    public void requestWindowClose()
    {
        windowClosing = true;
    }

    @Override
    public void cancelWindowClose()
    {
        windowClosing = false;
    }

    @Override
    public void close()
    {
        Platform.runLater(stage::close);
        context.close();
    }

    @Override
    public WindowSize getWindowSize()
    {
        return windowSize;
    }

    @Override
    public WindowPosition getWindowPosition()
    {
        return windowPosition;
    }

    @Override
    public void setWindowTitle(String title)
    {
        Platform.runLater(() -> stage.setTitle(title));
    }

    @Override
    public void setWindowSize(int width, int height)
    {
        Platform.runLater(() ->
        {
            root.setPrefWidth(width);
            root.setPrefHeight(height);
            stage.sizeToScene();
        });
    }

    @Override
    public void setWindowPosition(int x, int y)
    {
        Platform.runLater(() ->
        {
            stage.setX(x);
            stage.setY(y);
        });
    }

    @Override
    public boolean isFocused()
    {
        return focused;
    }

    @Override
    public MouseButtonState getMouseButtonState(int buttonIndex)
    {
        switch(buttonIndex)
        {
            case 0: return lmb;
            case 1: return rmb;
            case 2: return mmb;
            default: return MouseButtonState.RELEASED;
        }
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
}
