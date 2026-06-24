/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.state.scene.UserShader;
import javafx.collections.ListChangeListener;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
/*
This is the controller for the carousel has two main methods. Initialize which is called immediately
when CarouselController is created. This function detects changes to global carousel list and
does action based on if an element was added or removed. LoadCarouselCard is the other function,
this function is called whenever an element has been added to the list. That function just loads the
carousel cards.
 */
public class CarouselController implements Initializable {

    @FXML private HBox containerHBox;
    @FXML private ScrollPane carouselScrollPane;
    @FXML private AnchorPane mainBox;
    @FXML private Button minimizeButton;
    @FXML private Region resizeHandle;
    @FXML private HBox buttonBox;

    private double dragStartY;
    private double initialHeight;
    private static final double RESIZE_HEIGHT = 5.0;
    private static final int LOWER_BOUND = 180;
    private boolean minimized = false;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        // Creates a listener to detect if any elements are added or removed from array list
        Global.state().getCarouselModel().getCarouselShaders().addListener((ListChangeListener<UserShader>) change -> {

            //Once change happens, the while loop looks at what next change is
            while (change.next())
            {
                //If an element was added, loadCarouselCard is called with the additional shader
                if (change.wasAdded())
                {
                    for (UserShader addedShader : change.getAddedSubList())
                    {
                        // Dynamically load the FXML for every shader added to the model
                        loadCarouselCard(addedShader);
                    }
                    Platform.runLater(() -> {
                        carouselScrollPane.setHvalue(1.0);
                    });
                //If an element was removed, we remove the element from the container
                } else if (change.wasRemoved())
                {
                    for (UserShader removedShader : change.getRemoved())
                    {
                        containerHBox.getChildren().removeIf(node -> {
                            return removedShader.equals(node.getUserData());
                        });
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
    private void loadCarouselCard(UserShader shader)
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main/CarouselCard.fxml"));
            Region card = loader.load();

            card.setUserData(shader);

            CarouselCardController cardController = loader.getController();
            cardController.initData(shader);

            HBox.setHgrow(card, Priority.ALWAYS);
            double aspectRatio = 210.0 / 160.0;

            mainBox.heightProperty().addListener((obs, oldHeight, newHeight) -> {
                double width = newHeight.doubleValue() * aspectRatio;
                card.setPrefWidth(width);
                card.setMaxWidth(width);
                card.setMinWidth(width);
            });

            double startWidth = mainBox.getHeight() * aspectRatio;
            card.setPrefWidth(startWidth);
            card.setMaxWidth(startWidth);
            card.setMinWidth(startWidth);

            containerHBox.getChildren().add(card);

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Gets information needed to calculate offset
     * @param event
     */
    @FXML
    public void mousePressed(MouseEvent event) {
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
        double upperBound = mainBox.getParent().getScene().getWindow().getHeight() * .50;

        if (minimized)
        {
            if (newHeight >= 23)
            {
                updateHeight(newHeight);

                if (newHeight >= (LOWER_BOUND / 2.0))
                {
                    maximize();
                }
            }
            else
            {
                updateHeight(23);
            }
        }
        else
        {
            if (newHeight < (LOWER_BOUND/2.0))
            {
                minimize();
            }
            else if ((newHeight >= LOWER_BOUND) && (newHeight <= upperBound))
            {
                updateHeight(newHeight);
            }
            else if (newHeight < LOWER_BOUND)
            {
                updateHeight(LOWER_BOUND);
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
    private void updateHeight(double height){
        mainBox.setPrefHeight(height);
        mainBox.setMinHeight(height);
        mainBox.setMaxHeight(height);
    }

    /**
     * Detects if the release is not past the threshold to call maximize. Will resize bar back to 23
     * @param event
     */
    @FXML
    public void mouseReleased(MouseEvent event)
    {
        if (minimized)
        {
            updateHeight(23);
        }
    }

    /**
     * Un-hides button and the minimize bar while Hiding the cards. Sets minimized to true and updates
     * mainBox height to 23.
     */
    private void minimize()
    {
        updateHeight(23);

        containerHBox.setVisible(false);

        buttonBox.setManaged(true);
        buttonBox.setVisible(true);

        minimizeButton.setManaged(true);
        minimizeButton.setVisible(true);

        minimized = true;
    }

    /**
     * Hides button and the minimize bar while un-hiding the cards. Sets minimized to false.
     */
    private void maximize()
    {

        containerHBox.setVisible(true);

        buttonBox.setManaged(false);
        buttonBox.setVisible(false);

        minimizeButton.setManaged(false);
        minimizeButton.setVisible(false);

        minimized = false;
    }

    /**
     * When button is pressed toggleTopBar is triggered. For now button is only available when
     * minimized. It will update cardHeight to 180 and calls maximize().
     */
    public void toggleTopBar()
    {
        if (minimized)
        {
            updateHeight(180);
            maximize();
        }
        else
        {
            minimize();
        }
    }
}
