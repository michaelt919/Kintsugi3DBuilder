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
import kintsugi3d.builder.javafx.internal.ObservableTabsModel;
import kintsugi3d.builder.javafx.internal.ObservableUserShaderModel;


import java.io.File;
import java.util.*;
import java.util.Map.Entry;

public class RightBarController
{
    private static final int DEFAULT_WIDTH = 400;
    private static final int MINIMIZED_WIDTH = 23;

    private static final double RESIZE_WIDTH = 5.0;

    //Alternative LOWER_BOUND: 62
    private static final int LOWER_BOUND = 322;

    @FXML private VBox mainBox;
    @FXML private Button minimizeButton;
    @FXML private Label detailsLabel;
    @FXML private HBox detailsBox;
    @FXML private HBox textBox;
    @FXML private VBox imageDetails;
    @FXML private Label imageName;
    @FXML private Label imageNameSpace;

    @FXML private ImageDetailsController imageDetailsController;

    // needed to remove tabs
    private final Map<String, RadioButton> buttonMap = new HashMap<>(8);

    private String lastSelectedTabLabel = null;
    private boolean minimized = false;
    private boolean isLoaded = false;

    public Node getRootNode()
    {
        return mainBox;
    }

    public void init(ObservableTabsModel tabsModel, ObservableUserShaderModel shaderModel)
    {
        //Listener for any changes to selectedCards in tabModels
        tabsModel.getAllCards().addListener((ListChangeListener<String>) change ->
        {
            while (change.next()) //While something has been changed
            {
                if (tabsModel.getAllCards().isEmpty()) //If list is empty
                {
                    //Calls setImage() with null (Will not display panel)
                    imageDetailsController.setImage(null);

                    //Image Name will not be displayed
                    imageName.setText("");
                    textBox.setVisible(false);
                    textBox.setManaged(false);
                    isLoaded = false;
                }
                else if (tabsModel.getAllCards().size() == 1)//If list size equals 1
                {
                    //Sends filePath to imageDetailsController setImage(String fileName)
                    imageDetailFunctions(tabsModel, tabsModel.getAllCards().get(0));
                    textBox.setVisible(true);
                    textBox.setManaged(true);
                }
                else
                {
                    //Sends filePath to imageDetailsController setImage(String fileName)
                    imageDetailFunctions(tabsModel, tabsModel.getAllCards().get(tabsModel.getAllCards().size()-1));
                }
            }
        });

        resizeWidth(DEFAULT_WIDTH);
    }

    private void imageDetailFunctions(ObservableTabsModel tabsModel, String filePath)
    {
        imageDetailsController.setImage(filePath);

        //Sets Friendly Name from filePath
        imageNameSpace.setVisible(true);
        Platform.runLater(()->imageName.setText(tabsModel.getFileName(filePath)));
        isLoaded = true;

        if (minimized)
        {
            resizeWidth(DEFAULT_WIDTH);
            maximize();
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
        mainBox.requestLayout();
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
     * Hides all the tabs and features of the detail space then will set the mainBox size
     * to 23 pixels. Removes the features abilities to take up space when it hides
     * them. Sets minimized to true and changes minimize button text to "+".
     */
    private void minimize()
    {
        imageDetails.setManaged(false);
        imageDetails.setVisible(false);

        imageName.setManaged(false);
        imageName.setVisible(false);
        imageNameSpace.setManaged(false);
        imageNameSpace.setVisible(false);

        resizeWidth(MINIMIZED_WIDTH);

        detailsLabel.setVisible(false);
        detailsLabel.setManaged(false);

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
        if (isLoaded)
        {
            imageDetails.setManaged(true);
            imageDetails.setVisible(true);

            imageName.setManaged(true);
            imageName.setVisible(true);
            imageNameSpace.setManaged(true);
            imageNameSpace.setVisible(true);
        }

        detailsLabel.setVisible(true);
        detailsLabel.setManaged(true);

        for (Node child: detailsBox.getChildren())
        {
            child.setVisible(true);
            child.setManaged(true);
        }

        minimizeButton.setText("-");
        minimized = false;
        Platform.runLater(()->detailsBox.requestLayout());
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
        Platform.runLater(()->detailsBox.requestLayout());
    }
    public double getTabWidth(){ return mainBox.getWidth(); }
}
