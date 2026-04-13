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

package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class SelectionPageController<T> extends PageControllerBase<T, SelectionPage<T>>
{
    private static final double BUTTON_MIN_WIDTH = 400.0;
    private static final String[] BUTTON_STYLE_CLASSES = { "wireframeSubtitle", "toggleButtonLarge" };
    private static final Insets BUTTON_INSETS = new Insets(16.0);

    private final ToggleGroup buttons = new ToggleGroup();

    @FXML private VBox rootNode;
    @FXML private Label promptLabel;
    @FXML private VBox optionsRoot;

    @Override
    public Region getRootNode()
    {
        return rootNode;
    }

    @Override
    public void initPage()
    {
        this.getCanAdvanceObservable().bind(buttons.selectedToggleProperty().isNotNull());
    }

    @Override
    public void refresh()
    {
        ToggleButton prevSelected = (ToggleButton)buttons.getSelectedToggle();
        String prevSelectedName = null;
        if (prevSelected != null)
        {
            prevSelectedName = prevSelected.getText();
        }

        promptLabel.setText(getPage().getPrompt());

        // Clear and repopulate
        buttons.getToggles().clear();
        optionsRoot.getChildren().clear();

        for (var choice : getPage().getChoices())
        {
            // create button
            ToggleButton button = new ToggleButton();
            button.setText(choice.getKey());
            button.setMinWidth(BUTTON_MIN_WIDTH);
            button.getStyleClass().addAll(BUTTON_STYLE_CLASSES);
            button.setPadding(BUTTON_INSETS);
            button.setOnAction(e -> onButtonAction(button, choice.getValue()));

            // create anchor pane
            AnchorPane anchorPane = new AnchorPane();
            anchorPane.getChildren().add(button);
            AnchorPane.setTopAnchor(button, 0.0);
            AnchorPane.setBottomAnchor(button, 0.0);
            AnchorPane.setLeftAnchor(button, 0.0);
            AnchorPane.setRightAnchor(button, 0.0);

            // add to root node and button group
            optionsRoot.getChildren().add(anchorPane);
            buttons.getToggles().add(button);

            if (choice.getKey().equals(prevSelectedName))
            {
                buttons.selectToggle(button);
            }
        }
    }

    private void onButtonAction(Toggle button, Page<? super T, ?> page)
    {
        if (button.isSelected())
        {
            this.getPage().setNextPage(page);
        }
        else
        {
            getPage().setNextPage(null);
        }
    }

    @Override
    public void receiveData(T data)
    {
    }
}
