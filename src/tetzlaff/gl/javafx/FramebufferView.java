/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.gl.javafx;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.CharacterStringConverter;
import org.lwjgl.*;
import tetzlaff.gl.core.FramebufferSize;
import tetzlaff.gl.window.*;

public final class FramebufferView extends Region
{
    private final ImageView imageView;
    private FramebufferCanvas<?> canvas;

    private WritableImage frontImage;
    private WritableImage backImage;
    private boolean copyThreadRunning = false;
    private Thread nextCopyThread;
    private final Object nextCopyThreadLock = new Object();
    private volatile boolean frontImagePending = false;

    private volatile ByteBuffer frontCopyBuffer;
    private ByteBuffer backCopyBuffer;
    private final Object backCopyBufferLock = new Object();
    private boolean copyBufferSwapReady = false;

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

    public FramebufferView()
    {
        this.imageView = new ImageView();
        this.getChildren().add(imageView);
        this.widthProperty().addListener(width -> imageView.setFitWidth(this.getWidth()));
        this.heightProperty().addListener(height -> imageView.setFitHeight(this.getHeight()));

        // use timeline to set up 60Hz refresh cycle for onscreen framebuffer
        Timeline refresh = new Timeline();
        refresh.setCycleCount(Animation.INDEFINITE);
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
                catch(RuntimeException e)
                {
                    e.printStackTrace();
                }
            }));
        refresh.play();

        imageView.setOnMousePressed(event ->
        {
            if (canvas != null)
            {
                getButtonIndex(event.getButton()).ifPresent(
                    index -> canvas.pressMouseButton(index, getCursorPosition(event), getModifierKeys(event)));
            }
        });

        imageView.setOnMouseReleased(event ->
        {
            if (canvas != null)
            {
                getButtonIndex(event.getButton()).ifPresent(
                    index -> canvas.releaseMouseButton(index, getCursorPosition(event), getModifierKeys(event)));
            }
        });

        EventHandler<? super MouseEvent> mouseCursor = event ->
        {
            if (canvas != null)
            {
                canvas.moveCursor(getCursorPosition(event), getModifierKeys(event));
            }
        };

        imageView.setOnMouseMoved(mouseCursor);
        imageView.setOnMouseDragged(mouseCursor);

        EventHandler<? super MouseEvent> mouseEnter = event ->
        {
            if (canvas != null)
            {
                canvas.cursorEnter(getCursorPosition(event), getModifierKeys(event));
            }
        };

        imageView.setOnMouseEntered(mouseEnter);
        imageView.setOnMouseDragEntered(mouseEnter);

        EventHandler<? super MouseEvent> mouseExit = event ->
        {
            if (canvas != null)
            {
                canvas.cursorExit(getCursorPosition(event), getModifierKeys(event));
            }
        };

        imageView.setOnMouseExited(mouseExit);
        imageView.setOnMouseDragExited(mouseExit);

        imageView.setOnScroll(event ->
        {
            if (canvas != null)
            {
                canvas.scroll(event.getDeltaX(), event.getDeltaY());
            }
        });


        this.widthProperty().addListener((event, oldValue, newValue) -> handleWindowEvent());
        this.heightProperty().addListener((event, oldValue, newValue) -> handleWindowEvent());
    }

    /**
     * Registers for key and window events from the stage
     * @param stage
     */
    public void registerKeyAndWindowEventsFromStage(Stage stage)
    {
        stage.getScene().setOnKeyPressed(event ->
        {
            if (canvas != null)
            {
                canvas.pressKey(KeyCodeMaps.codeToKey(event.getCode()), getModifierKeys(event));
            }
        });

        stage.getScene().setOnKeyReleased(event ->
        {
            if (canvas != null)
            {
                canvas.releaseKey(KeyCodeMaps.codeToKey(event.getCode()), getModifierKeys(event));
            }
        });

        stage.getScene().setOnKeyTyped(event ->
        {
            if (canvas != null)
            {
                canvas.typeKey(KeyCodeMaps.codeToKey(event.getCode()), getModifierKeys(event));
                Character character = new CharacterStringConverter().fromString(event.getCharacter());
                if (character != null)
                {
                    canvas.typeCharacter(character, getModifierKeys(event));
                }
            }
        });

        stage.xProperty().addListener((event, oldValue, newValue) -> handleWindowEvent());
        stage.yProperty().addListener((event, oldValue, newValue) -> handleWindowEvent());

        stage.focusedProperty().addListener((event, oldValue, newValue) ->
        {
            if (canvas != null)
            {
                if (oldValue && !newValue)
                {
                    canvas.loseFocus();
                }
                else if (newValue && !oldValue)
                {
                    canvas.gainFocus();
                }
            }
        });

        javafx.stage.Window owner = stage.getOwner();
        Stage primaryStage = owner instanceof Stage ? (Stage) owner : stage;
        primaryStage.iconifiedProperty().addListener((event1, oldValue, newValue) ->
        {
            if (canvas != null)
            {
                if (oldValue && !newValue)
                {
                    canvas.restore();
                }
                else if (newValue && !oldValue)
                {
                    canvas.iconify();
                }
            }
        });

        stage.setOnCloseRequest(event ->
        {
            canvas.close();
            event.consume();
        });
    }

    public PollableCanvas3D<?> getCanvas()
    {
        return this.canvas;
    }

    public void setCanvas(FramebufferCanvas<?> canvas)
    {
        this.canvas = canvas;

        if (canvas != null)
        {
            handleWindowEvent(); // sets canvas size and applies vertical flip to imageView
            CanvasSize canvasSize = canvas.getSize();
            frontImage = new WritableImage(canvasSize.width, canvasSize.height);
            backImage = new WritableImage(canvasSize.width, canvasSize.height);
            imageView.setImage(frontImage);

            canvas.addSwapListener(frontFBO ->
            {
                // Read from FBO
                FramebufferSize fboCopyBufferDimensions = frontFBO.getSize();

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
                        while (frontImagePending)
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

                        backImage.getPixelWriter().setPixels(0, 0, fboCopyBufferDimensions.width, fboCopyBufferDimensions.height,
                            PixelFormat.getByteBgraInstance(), frontCopyBuffer, fboCopyBufferDimensions.width * 4);

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
            });
        }
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

        imageView.getTransforms().clear();
        imageView.getTransforms().add(new Scale(1, -1));
        imageView.getTransforms().add(new Translate(0, -height));

        if (canvas != null)
        {
            canvas.changeBounds(new CanvasPosition(x, y), new CanvasSize(width, height));
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

    private static ModifierKeys getModifierKeys(KeyEvent event)
    {
        return new ModifierKeysInstance(
            event.isShiftDown(),
            event.isControlDown(),
            event.isAltDown(),
            event.isMetaDown());
    }

    private static ModifierKeys getModifierKeys(MouseEvent event)
    {
        return new ModifierKeysInstance(
            event.isShiftDown(),
            event.isControlDown(),
            event.isAltDown(),
            event.isMetaDown());
    }

    private CursorPosition getCursorPosition(MouseEvent event)
    {
        return new CursorPosition((int)Math.round(event.getSceneX() - this.getLayoutX()), (int)Math.round(event.getSceneY() - this.getLayoutY()));
    }
}
