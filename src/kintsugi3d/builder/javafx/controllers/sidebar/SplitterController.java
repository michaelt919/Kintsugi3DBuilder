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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class SplitterController
{
    @FXML private Region testRegion;
    @FXML private Region splitRegion;
    @FXML private StackPane rightHandle;
    @FXML private StackPane leftHandle;
    @FXML private AnchorPane splitPane;
    @FXML private HBox splitHBox;

    private double offSet;

    public void initialize()
    {
        Rectangle clipRect = new Rectangle();

        clipRect.heightProperty().bind(splitPane.heightProperty());

        // X position of the clip starts right at the splitter handle
        DoubleBinding splitterX = Bindings.createDoubleBinding(() ->
            splitHBox.getLayoutX() + leftHandle.getWidth(),
            splitHBox.layoutXProperty(),
            leftHandle.widthProperty()
        );

        clipRect.xProperty().bind(splitterX);

        // Width covers from the splitter handle to the right edge of splitPane
        clipRect.widthProperty().bind(splitPane.widthProperty().subtract(splitterX));
        clipRect.setMouseTransparent(true);

        testRegion.setClip(clipRect);
        /*
        Global.state().getSplitCanvasModel().addCanvasChangedListener({

            splitPane.setVisible(true);
            splitPane.setManaged(true);
        }); */
    }

    @FXML
    public void mousePressed(MouseEvent event)
    {
        double currentX = getSplitterX(event);
        double currentRightAnchor = getRightAnchorValue();

        offSet = splitPane.getWidth() - currentX - currentRightAnchor;
    }

    @FXML
    private void mouseDragged(MouseEvent event)
    {
        double currentX = getSplitterX(event);

        double distanceToRightEdge = splitPane.getWidth() - currentX;

        double targetAnchor = distanceToRightEdge - offSet;

        double minAnchor = 0.0;
        double maxAnchor = Math.max(0.0, splitPane.getWidth() - splitHBox.getWidth());

        double clampedAnchor = Math.max(minAnchor, Math.min(targetAnchor, maxAnchor));

        AnchorPane.setRightAnchor(splitHBox, clampedAnchor);
    }

    private double getSplitterX(MouseEvent event) {
        Point2D pointInPane = splitPane.sceneToLocal(event.getSceneX(), event.getSceneY());
        return pointInPane.getX();
    }

    private double getRightAnchorValue() {
        Double anchor = AnchorPane.getRightAnchor(splitHBox);
        return (anchor != null) ? anchor : 0.0;
    }

    @FXML
    public void closeSplitter()
    {
        splitPane.setVisible(false);
        splitPane.setManaged(false);
    }
}
