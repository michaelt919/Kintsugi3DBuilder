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
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.state.cards.ShaderCardFactory;
import kintsugi3d.builder.state.scene.UserShader;
import javafx.collections.ListChangeListener;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
/*
This is the controller for the carousel has two main methods. Initialize which is called immediately
when CarouselController is created. This function detects changes to global carousel list and
does action based on if an element was added or removed. LoadCarouselCard is the other function,
this function is called whenever an element has been added to the list. That function just loads the
carousel cards.
 */
public class CarouselController implements Initializable {

    @FXML
    private HBox containerHBox;

    @FXML
    private ScrollPane carouselScrollPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Creates a listener to detect if any elements are added or removed from array list
        Global.state().getCarouselModel().getCarouselShaders().addListener((ListChangeListener<UserShader>) change -> {
            //Once change happens, the while loop looks at what next change is
            while (change.next()) {
                //If an element was added, loadCarouselCard is called with the additional shader
                if (change.wasAdded()) {
                    for (UserShader addedShader : change.getAddedSubList()) {
                        // Dynamically load the FXML for every shader added to the model
                        loadCarouselCard(addedShader);
                    }
                    Platform.runLater(() -> {
                        carouselScrollPane.setHvalue(0.0);
                    });
                //If an element was removed, we remove the element from the container
                } else if (change.wasRemoved()) {
                    for (UserShader removedShader : change.getRemoved()){
                        containerHBox.getChildren().removeIf(node -> {
                            return removedShader.equals(node.getUserData());
                        });
                    }
                }
            }
        });
    }
    // This function is what loads the Carousel and its cards
    private void loadCarouselCard(UserShader shader) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main/CarouselCard.fxml"));
            Node card = loader.load();

            card.setUserData(shader);
            // Shader data is passed to CarouselCard through initData(shader)
            CarouselCardController cardController = loader.getController();
            cardController.initData(shader);

            // Add the Carousel Cards to the layout through containerHBox
            containerHBox.getChildren().add(card);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
