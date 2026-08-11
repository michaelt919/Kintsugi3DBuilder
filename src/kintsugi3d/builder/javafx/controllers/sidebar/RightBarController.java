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
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import kintsugi3d.builder.javafx.internal.ObservableTabsModel;
import kintsugi3d.builder.javafx.internal.ObservableUserShaderModel;


import java.util.*;

public class RightBarController
{
    private static final int DEFAULT_WIDTH = 400;
    private static final int MINIMIZED_WIDTH = 23;

    private static final double RESIZE_WIDTH = 5.0;

    //Alternative LOWER_BOUND: 62
    private static final int LOWER_BOUND = 322;

    @FXML private Button minimizeButton;

    @FXML private HBox detailsBox;
    @FXML private HBox textBox;

    @FXML private VBox imageDetails;
    @FXML private VBox mainBox;
    @FXML private VBox panelBox;

    @FXML private Label imageName;
    @FXML private Label imageNameSpace;
    @FXML private Label detailsLabel;

    @FXML private AnchorPane textureLayers;

    @FXML private ScrollPane detailScrollPane;

    @FXML private ScrollBar overlayScrollBar;

    @FXML private ImageDetailsController imageDetailsController;
    @FXML private TextureLayersController textureLayersController;

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
                    textureLayersController.setShown(false);

                    //Image Name will not be displayed
                    imageName.setText("");
                    setVisibilityState(textBox, false);
                    isLoaded = false;
                }
                else
                {
                    //Sends filePath to imageDetailsController setImage(String fileName)
                    imageDetailFunctions(tabsModel, tabsModel.getAllCards().get(tabsModel.getAllCards().size()-1));
                    textureLayersController.setShown(true);
                    setVisibilityState(textBox, true);
                }
            }
        });
        resizeWidth(DEFAULT_WIDTH);

        //Bind the value/scroll position for the overlay and actual scroll pane
        overlayScrollBar.valueProperty().bindBidirectional(detailScrollPane.vvalueProperty());

        // Set min and max bounds
        overlayScrollBar.setMin(0.0);
        overlayScrollBar.setMax(1.0);

        // Compute the thumb size based on visible ratio
        panelBox.heightProperty().addListener((obs, oldVal, newVal) -> updateScrollBarRange());
        detailScrollPane.heightProperty().addListener((obs, oldVal, newVal) -> updateScrollBarRange());

        // Show/hide when content overflows
        overlayScrollBar.visibleProperty().bind(
            panelBox.heightProperty().greaterThan(detailScrollPane.heightProperty())
        );
    }
    private void updateScrollBarRange()
    {
        //Find the height of panel and of the scroll pane
        double viewportHeight = detailScrollPane.getHeight();
        double contentHeight = panelBox.getHeight();

        //If content is greater than the available space
        if ((contentHeight > 0) && (contentHeight > viewportHeight))
        {
            //Calculates visible amount
            double visibleRatio = viewportHeight / contentHeight;
            overlayScrollBar.setVisibleAmount(visibleRatio);
        } else //Don't need scroll bar, 1.0 means that all of content is visible
        {
            overlayScrollBar.setVisibleAmount(1.0);
        }
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
        panelBox.requestLayout();
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
        setVisibilityState(imageDetails, false);
        setVisibilityState(textureLayers, false);

        setVisibilityState(imageName, false);
        setVisibilityState(imageNameSpace, false);

        resizeWidth(MINIMIZED_WIDTH);

        setVisibilityState(detailsLabel, false);

        setVisibilityState(detailScrollPane, false);

        for (Node child : detailsBox.getChildren())
        {
            if (!Objects.equals(child, minimizeButton))
            {
                setVisibilityState(child, false);
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
            setVisibilityState(imageDetails, true);
            setVisibilityState(textureLayers, true);

            setVisibilityState(imageName, true);
            setVisibilityState(imageNameSpace, true);
        }
        setVisibilityState(detailScrollPane, true);

        setVisibilityState(detailsLabel, true);

        for (Node child: detailsBox.getChildren())
        {
            setVisibilityState(child, true);
        }

        minimizeButton.setText("-");
        minimized = false;
        Platform.runLater(()->detailsBox.requestLayout());
    }
    public void setVisibilityState(Node node, boolean active)
    {
        node.setVisible(active);
        node.setManaged(active);
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
