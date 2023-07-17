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
    private boolean copyThreadRunning = false;
    private Thread nextCopyThread = null;
    private final Object nextCopyThreadLock = new Object();
    private volatile boolean frontImagePending = false;

    private boolean windowClosing = false;

    private CanvasSize canvasSize;
    private CanvasPosition canvasPosition;
    private boolean focused;

    private final Map<Key, KeyState> keyStates = new EnumMap<>(Key.class);
    private ModifierKeys modifierKeys = ModifierKeys.NONE;
    private MouseButtonState lmb = MouseButtonState.RELEASED;
    private MouseButtonState mmb = MouseButtonState.RELEASED;
    private MouseButtonState rmb = MouseButtonState.RELEASED;

    private CursorPosition cursorPosition = new CursorPosition(0, 0);

    private ByteBuffer backCopyBuffer;
    private ByteBuffer frontCopyBuffer;
    private final Object backCopyBufferLock = new Object();
    private boolean copyBufferSwapReady = false;
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

                // Three threads need to be coordinated:
                // 1) the graphics thread (copying data off the framebuffer)
                // 2) the JavaFX thread (setting which Image is the current image for the ImageView)
                // 3) an standalone copy thread for copying into the JavaFX image that doesn't block either graphics or JavaFX
                // 3) must coordinate with both 1) and 2) to prevent race conditions
                synchronized (backCopyBufferLock)
                {
                    if (backCopyBuffer == null || backCopyBuffer.capacity() != fboCopyBufferDimensions.width * fboCopyBufferDimensions.height * 4)
                    {
                        backCopyBuffer = BufferUtils.createByteBuffer(fboCopyBufferDimensions.width * fboCopyBufferDimensions.height * 4);
                    }
                    else
                    {
                        backCopyBuffer.clear();
                    }

                    frontFBO.getTextureReaderForColorAttachment(0).readARGB(backCopyBuffer);

                    copyBufferSwapReady = true;
                }

                Thread copyThread = // Copy into WritableImage on another thread:
                    new Thread(() ->
                    {
                        while(frontImagePending)
                        {
                            Thread.onSpinWait();
                        }

                        // prevent swap in the middle of graphics thread writing to back copy buffer
                        synchronized (backCopyBufferLock)
                        {
                            if (copyBufferSwapReady)
                            {
                                copyBufferSwapReady = false;

                                // Swap copy buffers
                                // back is written to by graphics thread
                                // front is read from by copy thread
                                ByteBuffer tmp = frontCopyBuffer;
                                frontCopyBuffer = backCopyBuffer;
                                backCopyBuffer = tmp;
                            }
                        }

                        //noinspection FloatingPointEquality
                        if (fboCopyBufferDimensions.width != backImage.getWidth() || fboCopyBufferDimensions.height != backImage.getHeight())
                        {
                            backImage = new WritableImage(fboCopyBufferDimensions.width, fboCopyBufferDimensions.height);
                        }

                        //noinspection FloatingPointEquality
                        for (int y = fboCopyBufferDimensions.height - 1; y >= 0; y--)
                        {
                            IntBuffer fboCopyIntBuffer = frontCopyBuffer.asIntBuffer();
                            fboCopyIntBuffer.position((fboCopyBufferDimensions.height - y - 1) * fboCopyBufferDimensions.width);
                            backImage.getPixelWriter().setPixels(0, y, fboCopyBufferDimensions.width, 1,
                                PixelFormat.getIntArgbInstance(), fboCopyIntBuffer, fboCopyBufferDimensions.width);
                        }

                        // Swap images
                        WritableImage tmp = frontImage;
                        frontImage = backImage;
                        backImage = tmp;

                        frontImagePending = true;

                        // prevent race conditions related to starting the next thread
                        synchronized (nextCopyThreadLock)
                        {
                            if (nextCopyThread != null)
                            {
                                // Kick off the next copy thread if another is ready to go.
                                nextCopyThread.start();
                                nextCopyThread = null;
                            }
                            else
                            {
                                // Otherwise, there's no longer a copy thread running
                                copyThreadRunning = false;
                            }
                        }
                    });

                synchronized (nextCopyThreadLock)
                {
                    if (copyThreadRunning)
                    {
                        // Defer starting the thread until the current one has finished
                        nextCopyThread = copyThread;
                    }
                    else
                    {
                        // Start the thread right away.
                        copyThreadRunning = true;
                        copyThread.start();
                    }
                }
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
                    // Use the front image for the image view
                    if (frontImagePending)
                    {
                        imageView.setImage(frontImage);
                        frontImagePending = false;
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

        canvasSize = new CanvasSize((int)Math.round(imageView.getImage().getWidth()), (int)Math.round(imageView.getImage().getHeight()));
        canvasPosition = new CanvasPosition((int)Math.round(stage.getX()), (int)Math.round(stage.getY()));
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
        eventCollector.canvasSize(l -> l.canvasResized(this, width, height));
        eventCollector.framebufferSize(l -> l.framebufferResized(this, width, height));
        canvasSize = new CanvasSize(width, height);

        if (x != canvasPosition.x || y != canvasPosition.y)
        {
            eventCollector.canvasPos(l -> l.canvasMoved(this, x, y));
            canvasPosition = new CanvasPosition(x, y);
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
    public void setWindowTitle(String title)
    {
        Platform.runLater(() -> stage.setTitle(title));
    }

    @Override
    public void setSize(int width, int height)
    {
        Platform.runLater(() ->
        {
            root.setPrefWidth(width);
            root.setPrefHeight(height);
            stage.sizeToScene();
        });
    }

    @Override
    public void setPosition(int x, int y)
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
