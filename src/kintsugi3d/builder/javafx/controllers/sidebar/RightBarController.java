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
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import kintsugi3d.builder.javafx.internal.ObservableUserShaderModel;


import java.util.*;
import java.util.Map.Entry;

public class RightBarController
{
    private static final int DEFAULT_WIDTH = 400;
    private static final int MINIMIZED_WIDTH = 23;

    private static final double RESIZE_WIDTH = 5.0;

    //Alternative LOWER_BOUND: 62
    private static final int LOWER_BOUND = 322;

    @FXML private HBox buttonBox;
    @FXML private VBox mainBox;
    @FXML private Button minimizeButton;
    @FXML private Label detailsLabel;
    @FXML private HBox detailsBox;
    @FXML private VBox imageDetails;

    @FXML private ImageDetailsController imageDetailsController;

    // needed to remove tabs
    private final Map<String, RadioButton> buttonMap = new HashMap<>(8);
    private final Map<String, Pane> tabMap = new HashMap<>(8);

    private final ToggleGroup tabToggleGroup = new ToggleGroup();
    private final List<RadioButton> buttons = new ArrayList<>(8);
    private final Collection<CardTabController> tabControllers = new ArrayList<>(4);


    private String lastSelectedTabLabel = null;
    private boolean minimized = false;

    public Node getRootNode()
    {
        return mainBox;
    }

    public void init(ObservableUserShaderModel shaderModel)
    {
        imageDetailsController.init("C:\\Users\\schmuckij3299\\OneDrive - University of Wisconsin-Stout\\Kintsugi3D_Model\\Statue.k3d.files\\File_Not_Found.png");
        resizeWidth(DEFAULT_WIDTH);
    }

    private RadioButton createButton(String name)
    {
        RadioButton button = new RadioButton(name);

        // Set sizing
        double buttonHeight = 32.0;
        button.setMinHeight(buttonHeight);
        button.setMaxHeight(buttonHeight);
        button.setPrefHeight(buttonHeight);
        button.setMaxWidth(Double.MAX_VALUE);  // Equivalent to 1.7976931348623157E308

        // Set properties
        button.setMnemonicParsing(false);
        button.setSelected(false);
        button.setStyle("-fx-alignment: center;");
        button.getStyleClass().add("stripped-radio-button");
        button.setTextAlignment(TextAlignment.CENTER);

        // Add to ToggleGroup
        button.setToggleGroup(tabToggleGroup);

        // Allow the button to grow horizontally in an HBox
        HBox.setHgrow(button, Priority.ALWAYS);

        buttons.add(button);

        return button;
    }

    public void setVisibility(boolean visible)
    {
        mainBox.setVisible(visible);
        mainBox.setManaged(visible);
        if (visible)
        {
            Platform.runLater(() -> tabControllers.forEach(CardTabController::updateViewportVisibility));
        }
    }

    /**
     * If the Minimize button has a "-" it will call minimize. Alternatively if the
     * minimize button has a "+" it will set the mainBox size to 400 and will call
     * maximize
     */
    public void toggleSideBar()
    {
        if (minimized)
        {
            resizeWidth(DEFAULT_WIDTH);

            maximize();
        }
        else
        {
            minimize();
        }
    }

    /**
     * Uses event parameter to determine if the mouse is within 5 pixels of the edge.
     * If it is, cursor is set to resize cursor. Otherwise, default cursor.
     * @param event
     */
    public void mouseMoved(MouseEvent event)
    {
        if (event.getX() <= RESIZE_WIDTH)
        {
            mainBox.setCursor(Cursor.W_RESIZE);
        }
        else
        {
            mainBox.setCursor(Cursor.DEFAULT);
        }
    }

    /**
     * If the mouse is dragged it first gets the new mouse position, then it finds
     * the upper bound. Next it resizes the tab accordingly: If the box is minimized
     * it will snap back to minimized state if the drag is not far enough. Otherwise,
     * If it's dragged to make it bigger, once it is big enough it will call maximize.
     * If it's maximized and dragged small enough it will go into minimized state.
     * @param event
     */
    @FXML
    public void mouseDragged(MouseEvent event)
    {
        double newWidth = mainBox.getWidth()-event.getX();

        //decimal at end is percentage of screen it can be dragged to
        double upperBound = mainBox.getParent().getScene().getWindow().getWidth() * 0.45;

        //will only preform actions after this method if the cursor is resize cursor
        if (!mainBox.getCursor().equals(Cursor.W_RESIZE))
        {
            return;
        }

        if (minimized) //if in minimized state
        {
            if (newWidth >= MINIMIZED_WIDTH)
            {
                resizeWidth(newWidth);

                if (newWidth >= (LOWER_BOUND/2.0))
                {
                    maximize();
                }
            }
            else
            {
                resizeWidth(MINIMIZED_WIDTH);
            }
        }
        else
        {
            if (newWidth < (LOWER_BOUND/2.0))
            {
                minimize();
            }
            else if ((newWidth >= LOWER_BOUND) && (newWidth <= upperBound))
            {
                resizeWidth(newWidth);
            }
            else if (newWidth < LOWER_BOUND)
            {
                resizeWidth(LOWER_BOUND);
            }
            else if (newWidth > upperBound){
                resizeWidth(upperBound);
            }
        }
    }

    /**
     * If the mouse is released, the method looks to see if the tab is still in the
     * minimize state. If it is it will snap the window back to default 23 pixels wide.
     * @param event
     */
    @FXML
    public void mouseReleased(MouseEvent event)
    {
        if (minimized)
        {
            resizeWidth(MINIMIZED_WIDTH);
        }
    }

    /**
     * This function hides tabs like shaders, materials, etc. It also remembers the
     * tab that was currently being displayed to the user.
     */
    private void hideAllTabs()
    {
        if (lastSelectedTabLabel == null)
        {
            for (Entry<String, RadioButton> entry : buttonMap.entrySet())
            {
                RadioButton button = entry.getValue();

                if (button.isSelected())
                {
                    lastSelectedTabLabel = entry.getKey();
                }
                button.setSelected(false);
            }
        }
    }

    /**
     * Will select the tab that was last displayed to the user.
     */
    private void restoreTab()
    {
        if (lastSelectedTabLabel != null)
        {
            RadioButton lastTab = buttonMap.get(lastSelectedTabLabel);
            lastTab.setSelected(true);
            lastSelectedTabLabel = null;
        }
    }

    /**
     * Hides all the tabs and features of the detail space then will set the mainBox size
     * to 23 pixels. Removes the features abilities to take up space when it hides
     * them. Sets minimized to true and changes minimize button text to "+".
     */
    private void minimize()
    {
        imageDetails.setManaged(false);
        imageDetails.setVisible(false);

        resizeWidth(MINIMIZED_WIDTH);

        buttonBox.setVisible(false);
        buttonBox.setManaged(false);
        detailsLabel.setVisible(false);
        detailsLabel.setManaged(false);

        hideAllTabs();

        for (Node child : detailsBox.getChildren())
        {
            if (!Objects.equals(child, minimizeButton))
            {
                child.setVisible(false);
                child.setManaged(false);
            }
        }
        minimizeButton.setText("+");

        minimized = true;
    }

    /**
     * Unhides all tabs, features, and will make the features take up their space again.
     * Sets minimized to false. Changes minimize button to - again.
     */
    private void maximize()
    {
        imageDetails.setManaged(true);
        imageDetails.setVisible(true);

        buttonBox.setVisible(true);
        buttonBox.setManaged(true);
        detailsLabel.setVisible(true);
        detailsLabel.setManaged(true);

        restoreTab();

        for (Node child: detailsBox.getChildren())
        {
            child.setVisible(true);
            child.setManaged(true);
        }

        minimizeButton.setText("-");
        minimized = false;
    }

    /**
     * Used to condense code. Resizes mainBox according to parameter width.
     * @param width
     */
    private void resizeWidth(double width)
    {
        mainBox.setPrefWidth(width);
        mainBox.setMinWidth(width);
        mainBox.setMaxWidth(width);
    }
    public double getTabWidth(){ return mainBox.getWidth(); }
}
