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

package tetzlaff.gl.javafx;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.CharacterStringConverter;
import org.lwjgl.*;
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
    private final DoubleFramebufferObject<ContextType> framebuffer;

    private WritableImage frontImage;
    private WritableImage backImage;

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

    private ByteBuffer fboCopyBuffer;
    private FramebufferSize fboCopyBufferDimensions;

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

        frontImage = new WritableImage(windowSpec.getWidth(), windowSpec.getHeight());
        backImage = new WritableImage(windowSpec.getWidth(), windowSpec.getHeight());
        imageView = new ImageView(frontImage);

        root = new StackPane(imageView);
        root.setPrefWidth(windowSpec.getWidth());
        root.setPrefHeight(windowSpec.getHeight());
        root.widthProperty().addListener(width -> imageView.setFitWidth(root.getWidth()));
        root.heightProperty().addListener(height -> imageView.setFitHeight(root.getHeight()));

        Scene scene = new Scene(root);
        stage.setScene(scene);

        this.framebuffer = framebuffer;

        this.framebuffer.addSwapListener(frontFBO ->
        {
            if (!primaryStage.isIconified())
            {
                // Read from FBO
                fboCopyBufferDimensions = frontFBO.getSize();

                if (fboCopyBuffer == null || fboCopyBuffer.capacity() != fboCopyBufferDimensions.width * fboCopyBufferDimensions.height * 4)
                {
                    fboCopyBuffer = BufferUtils.createByteBuffer(fboCopyBufferDimensions.width * fboCopyBufferDimensions.height * 4);
                }
                else
                {
                    fboCopyBuffer.clear();
                }

                frontFBO.getTextureReaderForColorAttachment(0).readARGB(fboCopyBuffer);

                // Copy into WritableImage:

                //noinspection FloatingPointEquality
                if (fboCopyBufferDimensions.width != backImage.getWidth() || fboCopyBufferDimensions.height != backImage.getHeight())
                {
                    backImage = new WritableImage(fboCopyBufferDimensions.width, fboCopyBufferDimensions.height);
                }

                //noinspection FloatingPointEquality
                for (int y = fboCopyBufferDimensions.height - 1; y >= 0; y--)
                {
                    IntBuffer fboCopyIntBuffer = fboCopyBuffer.asIntBuffer();
                    fboCopyIntBuffer.position((fboCopyBufferDimensions.height - y - 1) * fboCopyBufferDimensions.width);
                    backImage.getPixelWriter().setPixels(0, y, fboCopyBufferDimensions.width, 1,
                        PixelFormat.getIntArgbInstance(), fboCopyIntBuffer, fboCopyBufferDimensions.width);
                }

                // Swap buffers
                WritableImage tmp = frontImage;
                frontImage = backImage;
                backImage = tmp;
            }
        });

        // use timeline to set up 60Hz refresh cycle for onscreen framebuffer
        Timeline refresh = new Timeline();
        refresh.setCycleCount(Timeline.INDEFINITE);
        refresh.getKeyFrames().add(new KeyFrame(Duration.seconds(1.0 / 60.0), // 60 Hz refresh rate
            event ->
            {
                try
                {
                    if (fboCopyBufferDimensions != null)
                    {
                        imageView.setImage(frontImage);
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }));
        refresh.play();

        stage.widthProperty().addListener((event, oldValue, newValue) -> handleWindowEvent());
        stage.heightProperty().addListener((event, oldValue, newValue) -> handleWindowEvent());
        stage.xProperty().addListener((event, oldValue, newValue) -> handleWindowEvent());
        stage.yProperty().addListener((event, oldValue, newValue) -> handleWindowEvent());

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

        primaryStage.iconifiedProperty().addListener((event1, oldValue, newValue) ->
        {
            if (oldValue && !newValue)
            {
                eventCollector.windowRestored(l1 -> l1.windowRestored(this));
            }
            else if (newValue && !oldValue)
            {
                eventCollector.windowIconified(l1 -> l1.windowIconified(this));
            }
        });

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

        imageView.setOnMousePressed(event ->
        {
            handleMouseEvent(event);
            getButtonIndex(event.getButton()).ifPresent(
                index -> eventCollector.mouseButtonPress(l -> l.mouseButtonPressed(this, index, modifierKeys)));
        });

        imageView.setOnMouseReleased(event ->
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

        imageView.setOnMouseMoved(mouseCursor);
        imageView.setOnMouseDragged(mouseCursor);

        EventHandler<? super MouseEvent> mouseEnter = event ->
        {
            handleMouseEvent(event);
            eventCollector.cursorEnter(l -> l.cursorEntered(this));
        };

        imageView.setOnMouseEntered(mouseEnter);
        imageView.setOnMouseDragEntered(mouseEnter);

        EventHandler<? super MouseEvent> mouseExit = event ->
        {
            handleMouseEvent(event);
            eventCollector.cursorExit(l -> l.cursorExited(this));
        };

        imageView.setOnMouseExited(mouseExit);
        imageView.setOnMouseDragExited(mouseExit);

        imageView.setOnScroll(event -> eventCollector.scroll(
            l -> l.scroll(this, event.getDeltaX(), event.getDeltaY())));

        windowSize = new WindowSize((int)Math.round(imageView.getImage().getWidth()), (int)Math.round(imageView.getImage().getHeight()));
        windowPosition = new WindowPosition((int)Math.round(stage.getX()), (int)Math.round(stage.getY()));
        focused = stage.isFocused();
    }

    private void handleWindowEvent()
    {
        // https://stackoverflow.com/questions/31807329/get-screen-coordinates-of-a-node-in-javafx-8
        Bounds bounds = imageView.getBoundsInLocal();
        Bounds screenBounds = imageView.localToScreen(bounds);
        int x = (int) Math.round(screenBounds.getMinX());
        int y = (int) Math.round(screenBounds.getMinY());
        int width = (int) Math.round(screenBounds.getWidth());
        int height = (int) Math.round(screenBounds.getHeight());

        framebuffer.requestResize(width, height);
        eventCollector.windowSize(l -> l.windowResized(this, width, height));
        eventCollector.framebufferSize(l -> l.framebufferResized(this, width, height));
        windowSize = new WindowSize(width, height);

        if (x != windowPosition.x || y != windowPosition.y)
        {
            eventCollector.windowPos(l -> l.windowMoved(this, x, y));
            windowPosition = new WindowPosition(x, y);
        }
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
