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
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.state.CanvasModel;
import kintsugi3d.builder.state.CanvasModelImpl;
import kintsugi3d.builder.state.CarouselItem;
import kintsugi3d.builder.state.CarouselModel;
import kintsugi3d.builder.state.scene.UserShader;
import kintsugi3d.gl.vecmath.IntVector2;
import kintsugi3d.gl.window.CanvasSize;

import java.util.Objects;

/*
This class is for the global state, so we can have a list of shaders in the carousel.
Has two methods, one to get an arraylist of the shaders currently in the carousel.
And another to add a shader to the carousel list.
 */
public class ObservableCarouselModel implements CarouselModel
{
    public static final int DEFAULT_CARD_WIDTH = 210;
    public static final int DEFAULT_CARD_HEIGHT = 160;

    public static final int CARD_SAFE_REGION_BOTTOM_MARGIN = 30;

    private final ObservableList<CarouselItem> carouselItems = FXCollections.observableArrayList();

    private final DoubleProperty carouselCardHeight = new SimpleDoubleProperty(DEFAULT_CARD_HEIGHT);

    // Auto-calculate card width from card height, preserving aspect ratio.
    private final DoubleBinding carouselCardWidth =
        carouselCardHeight.multiply((double)DEFAULT_CARD_WIDTH / (double)DEFAULT_CARD_HEIGHT);

    public ObservableCarouselModel()
    {
        carouselCardHeight.addListener((observable, oldValue, newValue) ->
        {
            ProjectInstance<?> instance = Global.state().getIOModel().getLoadedInstance();
            if (instance != null)
            {
                CanvasSize mainViewSize = Global.state().getMainCanvasModel().getCanvas().getSize();
                int carouselHeight = (int) Math.round(carouselCardHeight.get()); // TODO include margins?

                instance.setSafeRegion(
                    new IntVector2(0, carouselHeight),
                    new IntVector2(mainViewSize.width, mainViewSize.height));
            }

            // TODO need to also refresh safe regions for cards
//            for (CarouselItem item : carouselItems)
//            {
//                Global.state().getCanvasListModel().getCanvas(item.getShader())
//                    .setSafeRegion(0, 0, (int)Math.round(getCarouselCardWidth()), getCardSafeEndY());
//            }
        });
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
            // Set up the rendering backend for the card.
            // GPU resource allocation will happen within a Rendering.runLater call on the graphcs thread.
            Global.state().getCanvasListModel().createCanvas(shader,
                (int)Math.round(getCarouselCardWidth()), (int)Math.round(getCarouselCardHeight()),
                0, 0, (int)Math.round(getCarouselCardWidth()), getCardSafeEndY(),
                framebufferCanvas ->
                {
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

    private int getCardSafeEndY()
    {
        return (int) Math.round(getCarouselCardHeight() - CARD_SAFE_REGION_BOTTOM_MARGIN);
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
        }
    }

    /**
     * Clears all the shaders in carouselItems list.
     */
    @Override
    public void clearCarousel()
    {
        // Remove all shaders from rendering backend.
        for (CarouselItem item : carouselItems)
        {
            Global.state().getCanvasListModel().removeCanvas(item.getShader());
        }

        carouselItems.clear();
    }
}
