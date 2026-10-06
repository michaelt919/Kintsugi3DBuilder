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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Map;

public final class ActionButtonFactory
{
    private ActionButtonFactory()
    {
    }

    static void createActionButtons(Iterable<? extends Map<String, Runnable>> actions, VBox buttonBox,
                                    String buttonClass, String separatorClass)
    {
        buttonBox.getChildren().clear();
        actions.forEach(group ->
        {
            Separator separator = new Separator();
            separator.setPrefWidth(200.0);
            separator.getStyleClass().add(separatorClass);
            separator.setPadding(new Insets(16.0, 8.0, 16, 8.0)); // Top, Right, Bottom, Left
            buttonBox.getChildren().add(separator);
            group.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
            {
                HBox hBox = new HBox();
                hBox.setAlignment(Pos.TOP_CENTER);

//                // Button Icon
//                ImageView imageView = new ImageView(MainApplication.getInstance().getIcon());
//                imageView.setFitHeight(16.0);
//                imageView.setFitWidth(16.0);
//                imageView.setPickOnBounds(true);
//                imageView.setPreserveRatio(true);

                // Button
                Button button = new Button(entry.getKey());
                button.setGraphicTextGap(8.0);
                button.setMnemonicParsing(false);
                button.getStyleClass().add(buttonClass);
                button.getStyleClass().add("wireframeBodyStrong");
                button.getStylesheets().add("file:./kintsugiStyling.css");
                button.setOnAction(event -> {

                    /*If uncommented will make it so after a button is clicked
                      boarder will remain to show it is selected*/
                    //button.getStyleClass().add("activated");
                    entry.getValue().run();
                });

                HBox.setMargin(button, new Insets(0, 0, 8, 0));
                hBox.setPadding(new Insets(0, 40.0, 0, 40.0));
                hBox.getChildren().add(button);

                buttonBox.getChildren().add(hBox);
            });
        });
    }
}
