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
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import kintsugi3d.builder.javafx.internal.ObservableCardsModel;
import kintsugi3d.builder.javafx.internal.ObservableTabsModel;

import java.io.IOException;
import java.util.*;
import java.util.List;

public class SideBarController
{
    @FXML private HBox buttonBox;
    @FXML private VBox mainBox;
    @FXML private Button minimizeButton;
    @FXML private Label workspaceLabel;
    @FXML private HBox workspaceBox;

    // needed to remove tabs
    private final Map<String, RadioButton> buttonMap = new HashMap<>(8);
    private final Map<String, Pane> tabMap = new HashMap<>(8);

    private final ToggleGroup tabToggleGroup = new ToggleGroup();
    private final List<RadioButton> buttons = new ArrayList<>(8);
    private final Collection<CardTabController> tabControllers = new ArrayList<>(4);

    private static final double RESIZE_WIDTH = 5.0;
    //Alternative LOWER_BOUND: 62
    private static final int LOWER_BOUND = 322;
    private ObservableTabsModel tabModels;
    private String lastSelectedTabLabel = null;
    private boolean minimized = false;

    public Node getRootNode()
    {
        return mainBox;
    }

    public void init(ObservableTabsModel tabModels)
    {
        this.tabModels = tabModels;

        tabModels.getAllTabs().forEach(this::addTab);

        tabModels.getObservableTabsMap().addListener((MapChangeListener<String, ObservableCardsModel>) change ->
        {
            if (change.wasAdded())
            {
                addTab(change.getValueAdded());
            }

            if (change.wasRemoved())
            {
                removeTab(change.getValueRemoved().getModelLabel());
            }

            // Refresh whether tabs are visible any time the tabs model is updated.
            if (tabModels.getAllTabs().size() < 2)
            {
                buttonBox.setVisible(false);
                buttonBox.setManaged(false);
            }
            else
            {
                buttonBox.setVisible(true);
                buttonBox.setManaged(true);
            }

            // Select the first tab if no tab is selected.
            if (!tabModels.getAllTabs().isEmpty() && (tabToggleGroup.getSelectedToggle() == null))
            {
                buttons.get(0).setSelected(true);
            }
        });
        mainBox.setPrefWidth(400);
        mainBox.setMaxWidth(400);
    }

    private void removeTab(String key)
    {
        // Remove button and the tab itself from the maps and their actual containers.
        RadioButton button = buttonMap.remove(key);
        buttonBox.getChildren().remove(button);
        mainBox.getChildren().remove(tabMap.remove(key));
        buttons.remove(button);

        if (Objects.equals(tabToggleGroup.getSelectedToggle(), button))
        {
            if (this.tabModels.getAllTabs().isEmpty())
            {
                // Deselect if no tabs remain.
                tabToggleGroup.selectToggle(null);
            }
            else
            {
                // Select a tab if we deleted the selected one.
                buttons.get(0).setSelected(true);
            }
        }
    }

    private void addTab(ObservableCardsModel model)
    {
        RadioButton newButton = createButton(model.getModelLabel());
        VBox newTab = createTab(model);

        buttonBox.getChildren().add(newButton);
        mainBox.getChildren().add(newTab);

        buttonMap.put(model.getModelLabel(), newButton);
        tabMap.put(model.getModelLabel(), newTab);

        newTab.visibleProperty().bind(newButton.selectedProperty());
        newTab.managedProperty().bind(newButton.selectedProperty());
    }

    private RadioButton createButton(String name)
    {
        RadioButton button = new RadioButton(name);

        // Set sizing
        button.setMinHeight(32.0);
        button.setMaxHeight(32.0);
        button.setPrefHeight(32.0);
        button.setMaxWidth(Double.MAX_VALUE);  // Equivalent to 1.7976931348623157E308

        // Set properties
        button.setMnemonicParsing(false);
        button.setSelected(false);
        button.setStyle("-fx-alignment: center;");
        button.getStyleClass().add("stripped-radio-button");
        button.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Add to ToggleGroup
        button.setToggleGroup(tabToggleGroup);

        // Allow the button to grow horizontally in an HBox
        HBox.setHgrow(button, javafx.scene.layout.Priority.ALWAYS);

        buttons.add(button);

        return button;
    }

    private VBox createTab(ObservableCardsModel model)
    {
        VBox newTab = null;
        FXMLLoader loader = new FXMLLoader();
        try
        {
            loader.setLocation(getClass().getResource("/fxml/main/leftpanel/CardTab.fxml"));
            newTab = loader.load();
            CardTabController newTabController = loader.getController();

            tabControllers.add(newTabController);

            newTabController.init(model);
        }
        catch (IOException e)
        {
            // throw new RuntimeException(e);
        }
        return newTab;
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
            resizeWidth(400);

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
        if (event.getX() > (mainBox.getWidth() - RESIZE_WIDTH))
        {
            mainBox.setCursor(Cursor.E_RESIZE);
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
        double newWidth = event.getX();
        System.out.println(newWidth);
        //decimal at end is percentage of screen it can be dragged to
        double upperBound = mainBox.getParent().getScene().getWindow().getWidth() * .50;

        //will only preform actions after this method if the cursor is resize cursor
        if (!mainBox.getCursor().equals(Cursor.E_RESIZE))
        {
            return;
        }

        if (minimized) //if in minimized state
        {
            if (newWidth >= 23)
            {
                resizeWidth(newWidth);

                if (newWidth >= (LOWER_BOUND/2.0))
                {
                    maximize();
                }
            }
            else
            {
                resizeWidth(23);
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
            resizeWidth(23);
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
            for (Map.Entry<String, RadioButton> entry : buttonMap.entrySet())
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
     * Hides all the tabs and features of the workspace then will set the mainBox size
     * to 23 pixels. Removes the features abilities to take up space when it hides
     * them. Sets minimized to true and changes minimize button text to "+".
     */
    private void minimize()
    {
        resizeWidth(23);

        buttonBox.setVisible(false);
        workspaceLabel.setVisible(false);

        hideAllTabs();

        for (Node child: workspaceBox.getChildren())
        {
            if (!Objects.equals(child, minimizeButton))
            {
                child.setManaged(false);
            }
        }
        minimizeButton.setTranslateX(-4);
        minimizeButton.setText("+");
        minimized = true;
    }

    /**
     * Unhides all tabs, features, and will make the features take up their space again.
     * Sets minimized to false. Changes minimize button to - again.
     */
    private void maximize()
    {
        buttonBox.setVisible(true);
        workspaceLabel.setVisible(true);

        restoreTab();

        for (Node child: workspaceBox.getChildren())
        {
            child.setManaged(true);
        }

        minimizeButton.setTranslateX(0);
        minimizeButton.setText("-");
        minimized = false;
    }

    /**
     * Used to condense code. Resizes mainBox according to parameter width.
     * @param width
     */
    private void resizeWidth(double width){
        mainBox.setPrefWidth(width);
        mainBox.setMinWidth(width);
        mainBox.setMaxWidth(width);
    }
    public void refreshTabs()
    {
        tabControllers.forEach(CardTabController::refreshCardList);
    }
}