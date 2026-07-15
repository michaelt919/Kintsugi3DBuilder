/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.internal;

import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ImageBasedRenderable;
import kintsugi3d.builder.javafx.controllers.sidebar.CarouselCardController;
import kintsugi3d.builder.javafx.controllers.sidebar.CarouselController;
import kintsugi3d.builder.state.CanvasModel;
import kintsugi3d.builder.state.CanvasModelImpl;
import kintsugi3d.builder.state.CarouselItem;
import kintsugi3d.builder.state.CarouselModel;
import kintsugi3d.builder.state.scene.UserShader;
import kintsugi3d.gl.core.FramebufferSize;
import kintsugi3d.gl.vecmath.IntVector2;
import kintsugi3d.gl.window.CanvasSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/*
This class is for the global state, so we can have a list of shaders in the carousel.
Has two methods, one to get an arraylist of the shaders currently in the carousel.
And another to add a shader to the carousel list.
 */
public class ObservableCarouselModel implements CarouselModel
{
    private static final Logger LOG = LoggerFactory.getLogger(ObservableCarouselModel.class);

    public static final int DEFAULT_CARD_WIDTH = 210;
    public static final int DEFAULT_CARD_HEIGHT = 160;

    public static final int CARD_SAFE_REGION_BOTTOM_MARGIN = 30;

    private final ObservableList<CarouselItem> carouselItems = FXCollections.observableArrayList();

    private final DoubleProperty carouselHeight = new SimpleDoubleProperty(DEFAULT_CARD_HEIGHT);
    private final DoubleProperty carouselCardHeight = new SimpleDoubleProperty(DEFAULT_CARD_HEIGHT);

    // Auto-calculate card width from card height, preserving aspect ratio.
    private final DoubleBinding carouselCardWidth =
        carouselCardHeight.multiply((double) DEFAULT_CARD_WIDTH / (double) DEFAULT_CARD_HEIGHT);

    public ObservableCarouselModel()
    {
        // Wait for the global state to be initialized.
        Platform.runLater(() ->
        {
            // Adjust safe region for main view when window is resized.
            Global.state().getMainCanvasModel().addCanvasChangedListener(
                // Bind directly to framebuffer size (not canvas size)
                // to ensure that its always in sync with the rendering framebuffer and reduce jittering.
                framebufferCanvas -> framebufferCanvas.addFramebufferSizeListener(
                    (canvas, width, height) ->
                        refreshMainViewSafeRegion(canvas.getSizeForDisplay())));
        });

        carouselHeight.addListener((observable, oldValue, newValue) ->
        {
            // Refresh safe region for main view
            refreshMainViewSafeRegion();
        });
    }

    private void refreshMainViewSafeRegion()
    {
        refreshMainViewSafeRegion(Global.state().getMainCanvasModel().getCanvas().getSizeForDisplay());
    }

    private void refreshMainViewSafeRegion(CanvasSize mainViewSize)
    {
        ImageBasedRenderable<?> instance = Global.state().getIOModel().getMainRenderable();
        if (instance != null)
        {
            int carouselHeightRounded = (int) Math.round(carouselHeight.get());

            instance.setSafeRegion(
                new IntVector2(0, 0),
                new IntVector2(mainViewSize.width, mainViewSize.height - carouselHeightRounded));
        }
    }

    /**
     * returns the list of items (shader + canvas backend reference) currently held in global carousel model.
     * @return
     */
    @Override
    public ObservableList<CarouselItem> getCarouselItems()
    {
        return carouselItems;
    }

    public double getCarouselHeight()
    {
        return carouselHeight.get();
    }

    public DoubleProperty carouselHeightProperty()
    {
        return carouselHeight;
    }

    public double getCarouselCardHeight()
    {
        return carouselCardHeight.get();
    }

    public DoubleProperty carouselCardHeightProperty()
    {
        return carouselCardHeight;
    }

    public double getCarouselCardWidth()
    {
        return carouselCardWidth.get();
    }

    public DoubleBinding carouselCardWidthProperty()
    {
        return carouselCardWidth;
    }

    /**
     * Looks through the existing shaders if the parameter shader is already
     * in carousel it will not add the shader. If it is not it will add
     * shader to carouselItems list.
     * @param shader
     */
    @Override
    public void addToCarousel(UserShader shader)
    {
        // Prevents duplicate shaders in carousel / if the shaders don't match
        // then shader is sent to carousel
        if (carouselItems.stream().noneMatch(item -> Objects.equals(item.getShader(), shader)))
        {
            int initWidth = (int) Math.round(getCarouselCardWidth());
            int initHeight = (int) Math.round(getCarouselCardHeight());

            // Set up the rendering backend for the card.
            // GPU resource allocation will happen within a Rendering.runLater call on the graphcs thread.
            Global.state().getCanvasListModel().createCanvas(shader,
                initWidth, initHeight,
                0, 0, initWidth, getCardSafeEndY(initHeight),
                framebufferCanvas ->
                {
                    // Bind directly to resize listener to ensure that we are synchronized with the buffer swap cycle.
                    framebufferCanvas.addResizeListener(framebuffer ->
                    {
                        FramebufferSize size = framebuffer.getSize();

                        // Refresh safe region for card
                        ImageBasedRenderable<?> carouselInstance = Global.state().getIOModel().getRenderableForShader(shader);
                        carouselInstance.setSafeRegion(
                            new IntVector2(0, 0),
                            new IntVector2(size.width, getCardSafeEndY(size.height)));
                    });

                    // Create a CanvasModel for connecting JavaFX to the backend.
                    CanvasModel canvas = new CanvasModelImpl();
                    canvas.setCanvas(framebufferCanvas);

                    // After the canvas FBO is allocated we are notified on the graphics thread.
                    // Use Platform runLater to set up the card on the JavaFX side.
                    Platform.runLater(() ->
                    {
                        // This will trigger the FXML to load via observer and subsequently connect to the backend.
                        carouselItems.add(new CarouselItem(shader, canvas));
                    });
                });
        }
    }

    private static int getCardSafeEndY(int fullHeight)
    {
        return fullHeight - CARD_SAFE_REGION_BOTTOM_MARGIN;
    }

    /**
     * Looks through the existing shaders if the parameter shader is already
     * in carousel it will not add the shader. If it is not it will add
     * shader to carouselItems list.
     * @param shader
     */
    @Override
    public void removeFromCarousel(UserShader shader)
    {
        if (carouselItems.stream().anyMatch(item -> Objects.equals(item.getShader(), shader)))
        {
            carouselItems.removeIf(item -> Objects.equals(item.getShader(), shader));

            // Clean up the rendering backend for the card.
            Global.state().getCanvasListModel().removeCanvas(shader);

            if (carouselItems.isEmpty())
            {
                // Recenter main view if the carousel is gone.
                refreshMainViewSafeRegion();
            }
        }
    }

    /**
     * Clears all the shaders in carouselItems list.
     */
    @Override
    public void clearCarousel()
    {
        if (!carouselItems.isEmpty())
        {
            // Remove all shaders from rendering backend.
            for (CarouselItem item : carouselItems)
            {
                Global.state().getCanvasListModel().removeCanvas(item.getShader());
            }

            carouselItems.clear();

            // Recenter main view now that the carousel is gone.
            refreshMainViewSafeRegion();
        }
    }

    private void swapCards(int index1, int index2, CarouselController carouselController)
    {
        ObservableList<CarouselItem> container = FXCollections.observableArrayList();
        container.addAll(carouselItems);

        // Card to move found and is not the first in the list.
        // Get card from the old container at the location to the left.
        CarouselItem temp = carouselItems.get(index1);

        // Swap cards in the new container.
        container.set(index1, carouselItems.get(index2));
        container.set(index2, temp);

        // Remember scroll position
        double currentScrollPosition = carouselController.getHBarValue();

        // Replace all items in the carousel as a single change for optimization (allows the controller to reuse cards).
        carouselItems.setAll(container);
//        carouselItems.clear();
//        carouselItems.addAll(container);

        // Set scroll position after carousel is refreshed.
        Platform.runLater(() -> carouselController.setHBarPosition(currentScrollPosition));
    }

    private CarouselItem findCardByShader(UserShader shader)
    {
        for(CarouselItem item : carouselItems)
        {
            // Check if the current item's shader matches this card.
            if (shader.equals(item.getShader()))
            {
                return item;
            }
        }

        return null;
    }

    public void moveCardLeft(CarouselCardController card)
    {
        CarouselItem moveCard = findCardByShader(card.getShader());
        CarouselController carouselController = card.getCarouselController();

        if (moveCard != null)
        {
            int cardIndex = carouselItems.indexOf(moveCard);
            if (cardIndex > 0)
            {
                swapCards(cardIndex - 1, cardIndex, carouselController);
            }
        }
    }

    public void moveCardRight(CarouselCardController card)
    {
        CarouselItem moveCard = findCardByShader(card.getShader());
        CarouselController carouselController = card.getCarouselController();

        if (moveCard != null)
        {
            int cardIndex = carouselItems.indexOf(moveCard);

            if (cardIndex < carouselItems.size() - 1)
            {
                swapCards(cardIndex + 1, cardIndex, carouselController);
            }
        }
    }
}
