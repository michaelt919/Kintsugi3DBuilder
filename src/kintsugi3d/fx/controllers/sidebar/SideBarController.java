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

package kintsugi3d.fx.controllers.sidebar;

import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import kintsugi3d.fx.internal.ObservableCardsModel;
import kintsugi3d.fx.internal.ObservableTabsModel;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class SideBarController
{
    private static final int DEFAULT_WIDTH = 400;
    private static final int MINIMIZED_WIDTH = 23;

    private static final double RESIZE_WIDTH = 5.0;

    //Alternative LOWER_BOUND: 62
    private static final int LOWER_BOUND = 322;

    @FXML private HBox buttonBox;
    @FXML private VBox mainBox;
    @FXML private Button minimizeButton;
    @FXML private Label workspaceLabel;
    @FXML private HBox workspaceBox;
    @FXML private Insets inseters;

    // needed to remove tabs
    private final Map<String, RadioButton> buttonMap = new HashMap<>(4);
    private final Map<Toggle, ObservableCardsModel<?>> inverseButtonMap = new HashMap<>(4);
    private final Map<String, Pane> tabMap = new HashMap<>(4);

    private final ToggleGroup tabToggleGroup = new ToggleGroup();
    private final List<RadioButton> buttons = new ArrayList<>(4);
    private final Collection<CardTabController> tabControllers = new ArrayList<>(4);

    private ObservableTabsModel tabModels;
    private boolean minimized = false;
    private boolean resizingSidebar = false;

    public Node getRootNode()
    {
        return mainBox;
    }

    public void init(ObservableTabsModel tabModels)
    {
        this.tabModels = tabModels;

        tabModels.getAllTabs().forEach(this::addTab);

        tabModels.getObservableTabsMap().addListener((MapChangeListener<String, ObservableCardsModel<?>>) change ->
        {
            if (change.wasAdded())
            {
                addTab(change.getValueAdded());
            }

            if (change.wasRemoved())
            {
                removeTab(change.getValueRemoved().getLabel());
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

        // Update which tab is selected on the backend when a tab is selected
        // It's possible for none to be selected when the panel is collapsed --
        // but we still want the backend to remember the selected tab.
        tabToggleGroup.selectedToggleProperty().addListener(
            (obs, oldValue, newValue) ->
        {
            if (newValue != null)
            {
                tabModels.setActiveTab(inverseButtonMap.get(newValue));
            }
        });

        Toggle selectedToggle = tabToggleGroup.getSelectedToggle();
        if (selectedToggle != null)
        {
            tabModels.setActiveTab(inverseButtonMap.get(selectedToggle));
        }

        resizeWidth(DEFAULT_WIDTH);
    }

    private void removeTab(String key)
    {
        // Remove button and the tab itself from the maps and their actual containers.
        RadioButton button = buttonMap.remove(key);
        inverseButtonMap.remove(button);
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

    private void addTab(ObservableCardsModel<?> model)
    {
        RadioButton newButton = createButton(model.getLabel());
        VBox newTab = createTab(model);

        buttonBox.getChildren().add(newButton);
        mainBox.getChildren().add(newTab);

        buttonMap.put(model.getLabel(), newButton);
        inverseButtonMap.put(newButton, model);
        tabMap.put(model.getLabel(), newTab);

        newTab.visibleProperty().bind(newButton.selectedProperty());
        newTab.managedProperty().bind(newButton.selectedProperty());
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

    private VBox createTab(ObservableCardsModel<?> model)
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
     * This method is for resizing the scroll bar and will trigger events to try to stop the
     * scroll bar flicker
     * @param event
     */
    @FXML
    public void mousePressed(MouseEvent event)
    {
        if (Objects.equals(mainBox.getCursor(), Cursor.E_RESIZE))
        {
            resizingSidebar = true;
            tabControllers.forEach(CardTabController::onDragStarted);
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

        //decimal at end is percentage of screen it can be dragged to
        double upperBound = mainBox.getParent().getScene().getWindow().getWidth() * 0.45;

        //will only preform actions after this method if the cursor is resize cursor
        if (!mainBox.getCursor().equals(Cursor.E_RESIZE))
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
        resizingSidebar = false;

        tabControllers.forEach(CardTabController::onDragEnded); //Calls methods to stop scroll bar flicker

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
        for (Entry<String, RadioButton> entry : buttonMap.entrySet())
        {
            RadioButton button = entry.getValue();
            button.setSelected(false);
        }
    }

    /**
     * Will select the tab that was last displayed to the user.
     */
    private void restoreTab()
    {
        ObservableCardsModel<?> lastSelectedTab = tabModels.getActiveTab();
        if (lastSelectedTab != null)
        {
            RadioButton lastTab = buttonMap.get(lastSelectedTab.getLabel());
            lastTab.setSelected(true);
        }
    }

    /**
     * Hides all the tabs and features of the workspace then will set the mainBox size
     * to 23 pixels. Removes the features abilities to take up space when it hides
     * them. Sets minimized to true and changes minimize button text to "+".
     */
    private void minimize()
    {
        if (!buttonBox.getChildren().isEmpty())
        {
            resizeWidth(MINIMIZED_WIDTH);

            buttonBox.setVisible(false);
            buttonBox.setManaged(false);
            workspaceLabel.setVisible(false);
            workspaceLabel.setManaged(false);

            hideAllTabs();

            for (Node child: workspaceBox.getChildren())
            {
                if (!Objects.equals(child, minimizeButton))
                {
                    child.setVisible(false);
                    child.setManaged(false);
                }
            }
            workspaceBox.setPadding(new Insets(4, 4, 4,4));
            minimizeButton.setText("+");
            minimized = true;
        }
    }

    /**
     * Unhides all tabs, features, and will make the features take up their space again.
     * Sets minimized to false. Changes minimize button to - again.
     */
    private void maximize()
    {
        buttonBox.setVisible(true);
        buttonBox.setManaged(true);
        workspaceLabel.setVisible(true);
        workspaceLabel.setManaged(true);

        restoreTab();

        for (Node child: workspaceBox.getChildren())
        {
            child.setVisible(true);
            child.setManaged(true);
        }

        workspaceBox.setPadding(inseters);
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

        //Calls methods to stop scroll bar flicker
        if (resizingSidebar)
        {
            tabControllers.forEach(controller -> controller.onSidebarWidthChanged(width));
        }
    }

    public void refreshTabs()
    {
        tabControllers.forEach(CardTabController::reloadCardList);
    }
    public double getTabWidth() {return mainBox.getWidth();}
}