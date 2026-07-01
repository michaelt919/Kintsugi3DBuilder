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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.javafx.core.MainWindowController;
import kintsugi3d.builder.javafx.internal.ObservableCarouselModel;
import kintsugi3d.builder.state.CarouselItem;
import kintsugi3d.builder.state.scene.UserShader;

import java.io.IOException;

/*
    This is the controller for the carousel it does quite a few things.
    1. Detects changes to carouselModel list and adds or removes cards based on that
    2. Allows for the carousel to be resized
    3. Allows for mouse clicks to pass through to the right of the carousel
    4. Loads carousel cards after detection
 */
public class CarouselController
{
    private static final int DEFAULT_HEIGHT = 180;
    private static final int MINIMIZED_HEIGHT = 23;

    @FXML private HBox containerHBox;
    @FXML private ScrollPane carouselScrollPane;
    @FXML private AnchorPane mainBox;
    @FXML private Button minimizeButton;
    @FXML private Region resizeHandle;
    @FXML private HBox buttonBox;
    @FXML private VBox minimizeBar;
    @FXML private Button miniButton;

    private double dragStartY;
    private double initialHeight;
    private boolean minimized = false;
    private boolean wasScrollBarNeeded;
    private double minimizedValue;
    private double totalWidth = 30;

    private ObservableCarouselModel carouselModel;
    private MainWindowController mainWindowBox;

    public void init(ObservableCarouselModel carouselModel, MainWindowController mainWindowBox)
    {
        this.carouselModel = carouselModel;
        this.mainWindowBox = mainWindowBox;

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

                        //If carousel is minimized it will maximize it
                        if (minimized){ maximize(); }
                        //Calculates how big to make mainBox based on amount of carouselCards
                        totalWidth += carouselModel.getCarouselCardWidth() + containerHBox.getSpacing();
                        mainBox.setMaxWidth(totalWidth);
                        mainBox.setMouseTransparent(false);
                    }
                    // If an element was removed, we remove the element from the container
                    else if (change.wasRemoved())
                    {
                        for (CarouselItem removedItem : change.getRemoved())
                        {
                            containerHBox.getChildren().removeIf(node -> removedItem.getShader().equals(node.getUserData()));
                        }
                        //Re-Calculates mainBox width after removing card space from total width
                        totalWidth -= carouselModel.getCarouselCardWidth() + containerHBox.getSpacing();
                        mainBox.setMaxWidth(totalWidth);

                        if (carouselModel.getCarouselItems().isEmpty())
                        {
                            mainBox.setMouseTransparent(true);
                        }
                    }
                }
                scrollBarCheck();
            });

        //Checks for change in height/resize for the carousel and will allocate space accordingly
        mainBox.heightProperty().addListener((observable, oldHeight, newHeight) -> {
            totalWidth = 30;
            for (CarouselItem card : carouselModel.getCarouselItems())
            {
                 totalWidth += carouselModel.getCarouselCardWidth() + containerHBox.getSpacing();
            }

            mainBox.setMaxWidth(totalWidth);

            if (carouselModel.getCarouselItems().isEmpty())
            {
                mainBox.setMaxWidth(0);
            }
        });

        //Detects when the carousel space in mainWindow changes
        Platform.runLater(() -> {
            Region container = (Region) mainBox.getParent().getParent();

            container.widthProperty().addListener((observable, oldWidth, newWidth) -> {
                scrollBarCheck();
            });
            scrollBarCheck();
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

            carouselModel.carouselCardHeightProperty().bind(mainBox.heightProperty());

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

        scrollBarCheck();

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
            else if (newHeight > upperBound)
            {
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
        wasScrollBarNeeded = isScrollBarNeeded();

        minimizedValue = mainBox.getHeight();
        updateHeight(MINIMIZED_HEIGHT);

        containerHBox.setVisible(false);
        carouselScrollPane.setMouseTransparent(true);

        minimizeBar.setVisible(true);
        minimizeBar.setManaged(true);

        miniButton.setVisible(false);

        resizeHandle.setManaged(false);
        resizeHandle.setVisible(false);

        minimized = true;
    }

    /**
     * Hides button and the minimize bar while un-hiding the cards. Sets minimized to false.
     * Also checks for if the scrollbar is needed when maximizing
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
        resizeHandle.setVisible(true);

        containerHBox.setVisible(true);
        carouselScrollPane.setMouseTransparent(false);

        minimizeBar.setVisible(false);
        minimizeBar.setManaged(false);

        miniButton.setVisible(true);

        minimized = false;

        if(wasScrollBarNeeded)
        {
            carouselScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        }
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

    /**
     * Minimize carousel button logic.
     */
    public void minimizeCarousel()
    {
        minimize();
    }

    /**
     * Gets the right and left tab widths then determines if the scroll bar needs to be there
     * This method was created because of a new flickering issue with the scrollbar when
     * resizing the cards it
     */
    private boolean isScrollBarNeeded()
    {
        double scrollBarBound = mainBox.getParent().getScene().getWidth() - mainWindowBox.getRightTabWidth();
        double rightEdge = mainWindowBox.getLeftTabWidth() + mainBox.getWidth();

        double thresholdBuffer = 8.0;

        return !((rightEdge + thresholdBuffer) < scrollBarBound);
    }

    /**
     * Looks to see if scrollbar is needed if it is it will apply AS_NEEDED policy
     * if not will set scrollbarpolicy to NEVER
     */
    private void scrollBarCheck()
    {
        Platform.runLater(() -> {
            if (minimized)
            {
                wasScrollBarNeeded = true;
                return;
            }

            if (isScrollBarNeeded())
            {
                carouselScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            }
            else
            {
                carouselScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            }
        });
    }
}
