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

package kintsugi3d.builder.javafx.controllers.sidebar;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.javafx.internal.ObservableCarouselModel;
import kintsugi3d.builder.state.CarouselItem;
import kintsugi3d.builder.state.scene.UserShader;

import java.io.IOException;

/*
This is the controller for the carousel has two main methods. Initialize which is called immediately
when CarouselController is created. This function detects changes to global carousel list and
does action based on if an element was added or removed. LoadCarouselCard is the other function,
this function is called whenever an element has been added to the list. That function just loads the
carousel cards.
 */
public class CarouselController
{
    private static final int DEFAULT_HEIGHT = 180;
    private static final int MINIMIZED_HEIGHT = 23;
    private static final double RESIZE_HEIGHT = 5.0;

    @FXML private HBox containerHBox;
    @FXML private ScrollPane carouselScrollPane;
    @FXML private AnchorPane mainBox;
    @FXML private Button minimizeButton;
    @FXML private Region resizeHandle;
    @FXML private HBox buttonBox;
    @FXML private Separator minimizeSeparator;
    @FXML private VBox minimizeBar;
    @FXML private Button miniButton;

    private double dragStartY;
    private double initialHeight;
    private boolean minimized = false;
    private double minimizedValue;

    private ObservableCarouselModel carouselModel;

    public void init(ObservableCarouselModel carouselModel)
    {
        this.carouselModel = carouselModel;

        // Bind carousel card height to the height of the JavaFX container.
        // Carousel card width will be auto-calculated from height.
        carouselModel.carouselCardHeightProperty().bind(containerHBox.heightProperty());

        // Creates a listener to detect if any elements are added or removed from array list
        carouselModel.getCarouselItems().addListener(
            (ListChangeListener<CarouselItem>) change ->
            {
                miniButton.setVisible(!carouselModel.getCarouselItems().isEmpty() && !minimized);

                // Once change happens, the while loop looks at what next change is
                while (change.next())
                {
                    // If an element was added, loadCarouselCard is called with the additional shader
                    if (change.wasAdded())
                    {
                        for (CarouselItem addedItem : change.getAddedSubList())
                        {
                            // Dynamically load the FXML for every shader added to the model
                            CarouselCardController carouselCard = loadCarouselCard(addedItem.getShader());

                            if (carouselCard == null)
                            {
                                // Card load failed; clean up backend.
                                Global.state().getCanvasListModel().removeCanvas(addedItem.getShader());
                            }
                            else
                            {
                                // Force layout so that the ImageView has real dimensions when connecting to the backend.
                                mainBox.layout();

                                // Wait for layout.
                                Platform.runLater(() ->
                                {
                                    // Connect the backend to the JavaFX frontend.
                                    carouselCard.setupCanvas(addedItem.getCanvasModel());
                                });
                            }
                        }
                        Platform.runLater(() -> carouselScrollPane.setHvalue(1.0));
                        maximize();
                    }
                    // If an element was removed, we remove the element from the container
                    else if (change.wasRemoved())
                    {
                        for (CarouselItem removedItem : change.getRemoved())
                        {
                            containerHBox.getChildren().removeIf(node -> removedItem.getShader().equals(node.getUserData()));
                        }
                    }
                }
            });
    }

    /**
     * Takes in parameter shader and loads a CarouselCard assigned to that shader.
     * Adds the card to the scrollpane through containerHbox. Handles the listener
     * to detect when mainBox size changes and changes the cards height accordingly
     * @param shader
     */
    private CarouselCardController loadCarouselCard(UserShader shader)
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main/CarouselCard.fxml"));
            Region card = loader.load();

            card.setUserData(shader);

            CarouselCardController cardController = loader.getController();
            cardController.init(carouselModel, shader);

            HBox.setHgrow(card, Priority.ALWAYS);

            // Determine card width from the model, which uses aspect ratio to calculate it from the bound height property.
            card.prefWidthProperty().bind(carouselModel.carouselCardWidthProperty());
            card.maxWidthProperty().bind(carouselModel.carouselCardWidthProperty());
            card.minWidthProperty().bind(carouselModel.carouselCardWidthProperty());

            containerHBox.getChildren().add(card);

            return cardController;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets information needed to calculate offset
     * @param event
     */
    @FXML
    public void mousePressed(MouseEvent event)
    {
        dragStartY = event.getSceneY();
        initialHeight = mainBox.getHeight();
    }

    /**
     * If the mouse is dragged it first gets the new mouse position, then it finds
     * the upper bound. Next it resizes the tab accordingly: If the box is minimized
     * it will snap back to minimized state if the drag is not far enough. Otherwise,
     * if it's dragged to make it bigger, once it is big enough it will call maximize.
     * If it's maximized and dragged small enough it will go into minimized state.
     * @param event
     */
    @FXML
    public void mouseDragged(MouseEvent event)
    {
        if ((resizeHandle.getCursor() == null) || !resizeHandle.getCursor().equals(Cursor.S_RESIZE))
        {
            return;
        }

        double difference = event.getSceneY() - dragStartY;
        double newHeight = initialHeight + difference;
        double upperBound = mainBox.getParent().getScene().getWindow().getHeight() * 0.50;

        if (minimized)
        {
            /* Minimized draggable code. Also need to remove setManged for resizeHandle
            if (newHeight >= MINIMIZED_HEIGHT)
            {
                updateHeight(newHeight);

                buttonBox.setMinHeight(newHeight);
                buttonBox.setMaxHeight(newHeight);

                if (newHeight >= (DEFAULT_HEIGHT / 2.0))
                {
                    buttonBox.setMinHeight(MINIMIZED_HEIGHT);
                    buttonBox.setMaxHeight(MINIMIZED_HEIGHT);
                    maximize();
                }
            }
            else
            {
                updateHeight(MINIMIZED_HEIGHT);
                buttonBox.setMinHeight(MINIMIZED_HEIGHT);
                buttonBox.setMaxHeight(MINIMIZED_HEIGHT);
            }
            */
        }
        else
        {
            if (newHeight < (DEFAULT_HEIGHT/2.0))
            {
                minimize();
            }
            else if ((newHeight >= DEFAULT_HEIGHT) && (newHeight <= upperBound))
            {
                updateHeight(newHeight);
            }
            else if (newHeight < DEFAULT_HEIGHT)
            {
                updateHeight(DEFAULT_HEIGHT);
            }
            else if (newHeight > upperBound){
                updateHeight(upperBound);
            }
        }
    }

    /**
     * Used to clear code up, updates all of mainBox height settings to parameter height
     * @param height
     */
    private void updateHeight(double height)
    {
        mainBox.setPrefHeight(height);
        mainBox.setMinHeight(height);
        mainBox.setMaxHeight(height);
    }

    /**
     * Detects if the release is not past the threshold to call maximize. Will resize bar back to MINIMIZED_HEIGHT
     * @param event
     */
    @FXML
    public void mouseReleased(MouseEvent event)
    {
        if (minimized)
        {
            updateHeight(MINIMIZED_HEIGHT);
        }
        buttonBox.setMinHeight(MINIMIZED_HEIGHT);
        buttonBox.setMaxHeight(MINIMIZED_HEIGHT);
    }

    /**
     * Un-hides button and the minimize bar while Hiding the cards. Sets minimized to true and updates
     * mainBox height to MINIMIZED_HEIGHT.
     */
    private void minimize()
    {
        minimizedValue = mainBox.getHeight();
        updateHeight(MINIMIZED_HEIGHT);

        containerHBox.setVisible(false);

        minimizeBar.setVisible(true);
        minimizeBar.setManaged(true);

        miniButton.setVisible(false);

        resizeHandle.setManaged(false);

        minimized = true;
    }

    /**
     * Hides button and the minimize bar while un-hiding the cards. Sets minimized to false.
     */
    private void maximize()
    {
        if (minimizedValue < DEFAULT_HEIGHT)
        {
            updateHeight(DEFAULT_HEIGHT);
        }
        else
        {
            updateHeight(minimizedValue);
        }

        resizeHandle.setManaged(true);

        containerHBox.setVisible(true);

        minimizeBar.setVisible(false);
        minimizeBar.setManaged(false);

        miniButton.setVisible(true);

        minimized = false;
    }

    /**
     * When button is pressed toggleTopBar is triggered. For now button is only available when
     * minimized. It will update cardHeight to DEFAULT_HEIGHT and calls maximize().
     */
    public void toggleTopBar()
    {
        if (minimized)
        {
            maximize();
        }
        else
        {
            minimize();
        }
    }

    public void minimizeCarousel()
    {
        minimize();
    }
}
