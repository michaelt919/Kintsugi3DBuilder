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
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

import java.io.File;

public class ImageDetailsController
{
    @FXML private VBox detailBox;
    @FXML private HBox buttonRow;
    @FXML private ImageView displayImage;
    @FXML private StackPane stackPane;
    @FXML private Rectangle selectionBox;

    private boolean canZoom = false;
    private boolean canCrop = false;
    private boolean canPan = false;
    private Image originalImage;
    private Image currentImage;
    private ToggleGroup toggleGroup;
    private double currentWidth;
    private double currentHeight;
    private double currentX;
    private double currentY;
    private double mousePressedX;
    private double mousePressedY;
    private double startViewportX;
    private double startViewportY;

    private static final double MAX_ZOOM_SIZE = 10.0;
    private static final double MAX_IMAGE_SIZE = 372.0;

    /**
     * Called when controller is created.
     * Calls setImage() with null.
     * Creates buttons using createButtons(), then binds their width to be equal.
     * Display Image is always the size of detailBox minus 8 for padding.
     * Adds clip to prevent long crop images.
     */
    public void initialize()
    {
        setImage(null); //Sets default state to not shown.

        createButtons(); // Creates all buttons using method. Can add new buttons there as well.

        for (Node button : buttonRow.getChildren()) //For every button in buttonRow
        {
            //Bind buttons to each other so they all take up same size
            ((RadioButton) button).prefWidthProperty().bind(buttonRow.widthProperty().subtract(
                buttonRow.getSpacing()*buttonRow.getChildren().size()).divide(
                buttonRow.getChildren().size()));
        }
        //Image bound to detailBox width minus 8
        displayImage.fitWidthProperty().bind(detailBox.widthProperty().subtract(8.0));

        Rectangle clip = new Rectangle(); // Clip rectangle

        clip.widthProperty().bind(stackPane.widthProperty()); //Clip bounds are bound to stackPane
        clip.heightProperty().bind(stackPane.heightProperty());

        stackPane.setClip(clip); //Clip assigned to stackPane
    }

    /**
     * Takes in the filePath from the Right Bar controller to use as an image in this panel.
     * If filePath is null it will instead hide the detailsBox
     * @param
     */
    public void setImage(String filePath)
    {
        if (filePath != null)
        {
            detailBox.setVisible(true);
            buttonRow.setVisible(true);
            stackPane.setVisible(true);

            File imageFile = new File(filePath); //Creates file from the filePath

            if (imageFile.exists()) //If file exists
            {
                originalImage = new Image(imageFile.toURI().toString()); //Assigns original image with the file
                currentImage = originalImage; //Current image gets set to the original image
                displayImage.setImage(originalImage); //ImageView is set to originalImage
                Platform.runLater(()->{
                    reset();
                    toggleGroup.selectToggle(null);
                });
            }
            else
            {
                //System error message if the image is not found
                System.err.println("Error: File not found at " + imageFile.getAbsolutePath());
            }
        }
        else
        {
            detailBox.setVisible(false);
            buttonRow.setVisible(false);
            stackPane.setVisible(false);
        }
    }

    /**
     * Creates buttons with events, that are then added to buttonRow.
     * New buttons can be added and automatically given the same properties as current buttons.
     */
    private void createButtons()
    {
        toggleGroup = new ToggleGroup(); // Toggle group created so only 1 Radio button can be selected at once

        RadioButton zoom = createButton("Zoom"); //Zoom button created using createButton() method
        zoom.setOnAction(e -> zoom()); //Zoom action
        buttonRow.getChildren().add(zoom); //Zoom added to buttonRow

        RadioButton pan = createButton("Pan");
        pan.setOnAction(e -> pan());
        buttonRow.getChildren().add(pan);

        RadioButton marquee = createButton("Marquee");
        marquee.setOnAction(e -> marquee());
        buttonRow.getChildren().add(marquee);

        RadioButton reset = createButton("Reset");
        reset.setOnAction(e -> reset());
        buttonRow.getChildren().add(reset);
    }

    /**
     * CreateButton will assign each button with the desired looks/properties. Needs text of button as param.
     * @param name
     * @return Button
     */
    private RadioButton createButton(String name)
    {
        RadioButton button = new RadioButton(name); //New radio button

        // Set sizing
        double buttonHeight = 32.0; //Button Height
        button.setMinHeight(buttonHeight);
        button.setMaxHeight(buttonHeight);
        button.setPrefHeight(buttonHeight);
        button.setMaxWidth(Double.MAX_VALUE);  // Equivalent to 1.7976931348623157E308 / Max width

        // Set properties
        button.setMnemonicParsing(false); //No keyboard shortcuts
        button.setSelected(false); //No button selected
        button.setStyle("-fx-alignment: center;"); //Alignment of text
        button.getStyleClass().add("right-stripped-radio-button"); //Button css (KintsugiStyling.css)
        button.setTextAlignment(TextAlignment.CENTER); //Alignment of text wrapping

        HBox.setHgrow(button, Priority.ALWAYS); //Allow the button to grow horizontally in an HBox
        button.setToggleGroup(toggleGroup); //Allows only 1 button to be selected

        return button;
    }

    /**
     * Handles scrolling in the image for zoom scroll up to enlarge down to zoom out.
     * @param event
     */
    @FXML
    public void scrolling(ScrollEvent event)
    {
        //Will stop function if there is no image or if canZoom is false
        if ((currentImage == null) || !canZoom) return;

        // If no viewport is set yet, initialize it to the full image size
        if (displayImage.getViewport() == null)
        {
            currentWidth = currentImage.getWidth();
            currentHeight = currentImage.getHeight();
            currentX = 0;
            currentY = 0;
        }

        // If event is positive returns 1.1 (scrolls in), if its negative returns .9 (scrolls out)
        double zoomFactor = (event.getDeltaY() > 0) ? 0.9 : 1.1;

        // Calculate new dimensions
        double newWidth = currentWidth * zoomFactor;
        double newHeight = currentHeight * zoomFactor;

        // Prevent zooming out past the original image size boundary
        if ((newWidth > currentImage.getWidth()) || (newHeight > currentImage.getHeight()))
        {
            resetViewport();
            event.consume();
            return;
        }

        // Prevent zooming in too close ((Optional), less than Max Zoom Size pixels)
        if ((newWidth < MAX_ZOOM_SIZE) || (newHeight <MAX_ZOOM_SIZE))
        {
            event.consume();
            return;
        }

        // Adjust X and Y offsets so it zooms relative to the center of the viewport
        currentX += (currentWidth - newWidth) / 2;
        currentY += (currentHeight - newHeight) / 2;

        currentWidth = newWidth;
        currentHeight = newHeight;

        //Apply the "crop" window to the ImageView
        displayImage.setViewport(new Rectangle2D(currentX, currentY, currentWidth, currentHeight));

        event.consume();
    }

    /**
     * Resets viewport to original settings (No zoom or panning)
     */
    private void resetViewport()
    {
        if (currentImage != null)
        {
            displayImage.setViewport(null); // Clears the zoom and pan

            //sets dimensions back to basic numbers
            currentWidth = currentImage.getWidth();
            currentHeight = currentImage.getHeight();
            currentX = 0;
            currentY = 0;
        }
    }

    /**
     * Method runs as soon as mouse is pressed. Gets pressed coordinates.
     * @param event
     */
    @FXML
    public void startSelection(MouseEvent event)
    {
        //Event location
        mousePressedX = event.getX();
        mousePressedY = event.getY();

        // Initialize viewport if it hasn't been set yet
        if (displayImage.getViewport() == null)
        {
            currentWidth = currentImage.getWidth();
            currentHeight = currentImage.getHeight();
            currentX = 0;
            currentY = 0;
            displayImage.setViewport(new Rectangle2D(currentX, currentY, currentWidth, currentHeight));
        }

        if (canPan) //If panning is active takes the current x any and assigns it to startViewport x and y
        {
            startViewportX = currentX;
            startViewportY = currentY;
        }
        else if (canCrop) //If marquee is active we initiate selectionBox
        {
            selectionBox.setTranslateX(mousePressedX);
            selectionBox.setTranslateY(mousePressedY);
            selectionBox.setWidth(0);
            selectionBox.setHeight(0);
            selectionBox.setVisible(true);
        }
    }

    /**
     * This occurs while the mouse is being dragged.
     * @param event
     */
    @FXML
    public void dragSelection(MouseEvent event)
    {
        if (currentImage == null) return; //Safeguard if image is there

        //Gets current mouse positions
        double currentMouseX = event.getX();
        double currentMouseY = event.getY();

        if (canPan) //If panning
        {
            //Gets pixel difference amount from pan
            double deltaX = currentMouseX - mousePressedX;
            double deltaY = currentMouseY - mousePressedY;

            // Scale coordinates matching actual pixel image amount
            Bounds bounds = displayImage.getBoundsInParent();

            double renderedWidth = bounds.getWidth();
            double renderedHeight = bounds.getHeight();

            double scaleX = currentWidth / renderedWidth;
            double scaleY = currentHeight / renderedHeight;

            //Target X and Y will contain the location on actual image after the scale we are trying to reach
            double targetX = startViewportX - (deltaX * scaleX);
            double targetY = startViewportY - (deltaY * scaleY);

            // Gets the max X and Y we can be at
            double maxX = currentImage.getWidth() - currentWidth;
            double maxY = currentImage.getHeight() - currentHeight;

            // If is over the max we set it to max
            currentX = Math.max(0, Math.min(targetX, maxX));
            currentY = Math.max(0, Math.min(targetY, maxY));

            //Sets new viewport
            displayImage.setViewport(new Rectangle2D(currentX, currentY, currentWidth, currentHeight));
        }
        else if (canCrop) // If cropping
        {
            // Gets the max and min X and Y we can be at
            double imageMinX = displayImage.getBoundsInParent().getMinX();
            double imageMinY = displayImage.getBoundsInParent().getMinY();
            double imageMaxX = displayImage.getBoundsInParent().getMaxX()-selectionBox.getStrokeWidth();
            double imageMaxY = displayImage.getBoundsInParent().getMaxY()-selectionBox.getStrokeWidth();

            // Finds the minimum of mousePressed and imageMax, then the max of that or imageMin
            double clampedStartX = Math.max(imageMinX, Math.min(mousePressedX, imageMaxX));
            double clampedStartY = Math.max(imageMinY, Math.min(mousePressedY, imageMaxY));

            // Finds the minimum of currentMouse and imageMax, then the max of that or imageMin
            double clampedMouseX = Math.max(imageMinX, Math.min(currentMouseX, imageMaxX));
            double clampedMouseY = Math.max(imageMinY, Math.min(currentMouseY, imageMaxY));

            // Creates selection box that is within bounds
            double boxX = Math.min(clampedStartX, clampedMouseX);
            double boxY = Math.min(clampedStartY, clampedMouseY);
            double boxWidth = Math.abs(clampedMouseX - clampedStartX);
            double boxHeight = Math.abs(clampedMouseY - clampedStartY);

            //Actually sets the selection box
            selectionBox.setTranslateX(boxX);
            selectionBox.setTranslateY(boxY);
            selectionBox.setWidth(boxWidth);
            selectionBox.setHeight(boxHeight);
        }
    }

    /**
     * This method handles operations after the drag ends.
     * @param event
     */
    @FXML
    private void endSelection(MouseEvent event)
    {
        if (canCrop && selectionBox.isVisible() && (selectionBox.getWidth() > 5) && (selectionBox.getHeight() > 5.0))
        {
            // If the viewport exists we get those dimensions, otherwise we get dimensions of full image
            double visibleWidth = (displayImage.getViewport() != null) ? displayImage.getViewport().getWidth() : currentImage.getWidth();
            double visibleHeight = (displayImage.getViewport() != null) ? displayImage.getViewport().getHeight() : currentImage.getHeight();
            double visibleX = (displayImage.getViewport() != null) ? displayImage.getViewport().getMinX() : 0;
            double visibleY = (displayImage.getViewport() != null) ? displayImage.getViewport().getMinY() : 0;

            // Finds the width of the displayed image
            double renderedWidth = displayImage.getBoundsInParent().getWidth();
            double renderedHeight = displayImage.getBoundsInParent().getHeight();

            //If the width is equal to 0 or less it gets the width of stackPane
            if (renderedWidth <= 0) renderedWidth = stackPane.getWidth();
            if (renderedHeight <= 0) renderedHeight = stackPane.getHeight();

            // Finds scale factor for what is CURRENTLY visible on screen
            double scaleX = visibleWidth / renderedWidth;
            double scaleY = visibleHeight / renderedHeight;

            // Min x and y for the current image displayed
            double imageMinX = displayImage.getBoundsInParent().getMinX();
            double imageMinY = displayImage.getBoundsInParent().getMinY();

            // Subtracts the max of 0 and min x/y (this accounts for any padding) from selectionBox
            double trueX = selectionBox.getTranslateX() - Math.max(0, imageMinX);
            double trueY = selectionBox.getTranslateY() - Math.max(0, imageMinY);

            // Shifts other coordinates from the padding
            double actualX = visibleX + (trueX * scaleX);
            double actualY = visibleY + (trueY * scaleY);
            double actualWidth = selectionBox.getWidth() * scaleX;
            double actualHeight = selectionBox.getHeight() * scaleY;

            //Adjusts selection to keep the same aspect ratio. Will always be same size
            double centerX = actualX + actualWidth / 2.0;
            double centerY = actualY + actualHeight / 2.0;
            double viewAspect = stackPane.getWidth() / stackPane.getHeight();
            double viewportWidth = actualWidth;
            double viewportHeight = actualHeight;

            if (viewportWidth / viewportHeight < viewAspect)
            {
                viewportWidth = viewportHeight * viewAspect;
            }
            else
            {
                viewportHeight = viewportWidth / viewAspect;
            }

            currentX = centerX - viewportWidth / 2.0;
            currentY = centerY - viewportHeight / 2.0;
            currentWidth = viewportWidth;
            currentHeight = viewportHeight;

            //Clamps selection box to prevent out of bounds.
            currentX = Math.max(0, Math.min(currentX, currentImage.getWidth() - currentWidth));
            currentY = Math.max(0, Math.min(currentY, currentImage.getHeight() - currentHeight));

            // changes viewport to selection rectangle
            displayImage.setViewport(new Rectangle2D(currentX, currentY, currentWidth, currentHeight));

            // Dynamic container shrinking logic
            double imgW = currentImage.getWidth();
            double imgH = currentImage.getHeight();
            double currentPaneWidth = stackPane.getWidth();

            // Safe fallback tracking if layout properties haven't fully drawn yet
            if (currentPaneWidth <= 0)
            {
                currentPaneWidth = renderedWidth;
            }

            // Calculate how high the image should render
            double aspect = currentHeight / currentWidth;
            double targetRenderedHeight = currentPaneWidth * aspect;

            // Caps the height for image using Max Image Size
            double finalHeight = Math.min(MAX_IMAGE_SIZE, targetRenderedHeight);

            // Update stackPane max height
            stackPane.setPrefHeight(finalHeight);
            stackPane.setMaxHeight(finalHeight);
        }
        selectionBox.setVisible(false); // Remove selection box
    }

    /**
     * !!!THIS METHOD IS ONLY USED WITH ACTUAL CROP NOT VIEWPORT CHANGE!!! (Currently actual crop code is commented out)
     * Crops the image to the parameter cropRect which is a 2D rectangle.
     * @param cropRect
     * @return
     */
    private Image cropImage(Rectangle2D cropRect)
    {
        // Extract and round the coordinates
        int x = (int) Math.round(cropRect.getMinX());
        int y = (int) Math.round(cropRect.getMinY());
        int width = (int) Math.round(cropRect.getWidth());
        int height = (int) Math.round(cropRect.getHeight());

        // Safeguard boundaries with max image size
        x = Math.max(0, Math.min(x, (int) currentImage.getWidth() - 1));
        y = Math.max(0, Math.min(y, (int) currentImage.getHeight() - 1));
        width = Math.max(1, Math.min(width, (int) currentImage.getWidth() - x));
        height = Math.max(1, Math.min(height, (int) currentImage.getHeight() - y));

        var pixelReader = currentImage.getPixelReader(); // Get the PixelReader from image

        if (pixelReader == null)
        {
            // Pixel reader exception statement
            throw new IllegalStateException("Image pixel reader is not available. Ensure the image is fully loaded.");
        }

        // Create a new image with the cropped dimensions and returns it
        return new WritableImage(pixelReader, x, y, width, height);
    }

    /**
     * Method to handle zoom button.
     * Sets booleans for crop and pan to false and true for zoom.
     * Sets selectionBox to invisible.
     */
    @FXML
    public void zoom()
    {
        canCrop = false;
        canPan = false;
        canZoom = true;
        selectionBox.setVisible(false);
    }

    /**
     * Method to handle pan button.
     * Sets booleans for crop and zoom to false and true for pan.
     * Sets selectionBox to invisible.
     */
    @FXML
    public void pan()
    {
        canCrop = false;
        canPan = true;
        canZoom = false;
        selectionBox.setVisible(false);
    }

    /**
     * Method to handle marquee button.
     * Sets booleans for zoom and pan to false and true for crop.
     */
    @FXML
    public void marquee()
    {
        canCrop = true;
        canPan = false;
        canZoom = false;
    }

    /**
     * Method to handle reset button.
     * Sets booleans for crop, pan, and zoom to false.
     * Resets viewport.
     * Sets selectionBox to invisible, currentImage to originalImage, and stackPane height to computed size.
     */
    @FXML
    public void reset()
    {
        canCrop = false;
        canPan = false;
        canZoom = false;
        resetViewport();
        selectionBox.setVisible(false);
        displayImage.setImage(originalImage);
        currentImage = originalImage;
        stackPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
        stackPane.setMaxHeight(Region.USE_COMPUTED_SIZE);
    }
    /* (This code is if you want to actually crop the image instead of changing the viewport)
    @FXML
    private void endSelection(MouseEvent event)
    {
        if (canCrop && selectionBox.isVisible() && (selectionBox.getWidth() > 5) && (selectionBox.getHeight() > 5.0))
        {
            // If the viewport exists we get those dimensions, otherwise we get dimensions of full image
            double visibleWidth = (displayImage.getViewport() != null) ? displayImage.getViewport().getWidth() : currentImage.getWidth();
            double visibleHeight = (displayImage.getViewport() != null) ? displayImage.getViewport().getHeight() : currentImage.getHeight();
            double visibleX = (displayImage.getViewport() != null) ? displayImage.getViewport().getMinX() : 0;
            double visibleY = (displayImage.getViewport() != null) ? displayImage.getViewport().getMinY() : 0;

            // Finds the width of the displayed image
            double renderedWidth = displayImage.getBoundsInParent().getWidth();
            double renderedHeight = displayImage.getBoundsInParent().getHeight();

            //If the width is equal to 0 or less it gets the width of stackPane
            if (renderedWidth <= 0) renderedWidth = stackPane.getWidth();
            if (renderedHeight <= 0) renderedHeight = stackPane.getHeight();

            // Finds scale factor for what is CURRENTLY visible on screen
            double scaleX = visibleWidth / renderedWidth;
            double scaleY = visibleHeight / renderedHeight;

            // Min x and y for the current image displayed
            double imageMinX = displayImage.getBoundsInParent().getMinX();
            double imageMinY = displayImage.getBoundsInParent().getMinY();

            // Subtracts the max of 0 and min x/y (this accounts for any padding) from selectionBox
            double trueX = selectionBox.getTranslateX() - Math.max(0, imageMinX);
            double trueY = selectionBox.getTranslateY() - Math.max(0, imageMinY);

            // Shifts other coordinates from the padding
            double actualX = visibleX + (trueX * scaleX);
            double actualY = visibleY + (trueY * scaleY);
            double actualWidth = selectionBox.getWidth() * scaleX;
            double actualHeight = selectionBox.getHeight() * scaleY;

            // Prevents the position from ever being out of bounds (Should already be accounted for, but just in case)
            currentX = Math.max(0, Math.min(actualX, currentImage.getWidth() - actualWidth));
            currentY = Math.max(0, Math.min(actualY, currentImage.getHeight() - actualHeight));
            currentWidth = actualWidth;
            currentHeight = actualHeight;

            // Crops current image and assigns to current image (original image is never changed)
            currentImage = cropImage(new Rectangle2D(currentX, currentY, currentWidth, currentHeight));
            displayImage.setImage(currentImage);

            // Dynamic container shrinking logic
            double imgW = currentImage.getWidth();
            double imgH = currentImage.getHeight();
            double currentPaneWidth = stackPane.getWidth();

            // Safe fallback tracking if layout properties haven't fully drawn yet
            if (currentPaneWidth <= 0)
            {
                currentPaneWidth = renderedWidth;
            }

            // Calculate how high the image should render
            double aspect = imgH / imgW;
            double targetRenderedHeight = currentPaneWidth * aspect;

            // Caps the height for image using Max Image Size
            double finalHeight = Math.min(MAX_IMAGE_SIZE, targetRenderedHeight);

            // Update stackPane max height
            stackPane.setPrefHeight(finalHeight);
            stackPane.setMaxHeight(finalHeight);

            // Reset viewport with new cropped image
            currentWidth = imgW;
            currentHeight = imgW * (finalHeight / currentPaneWidth);
            currentX = 0;
            currentY = 0;

            // Initialize the new viewport
            displayImage.setViewport(new Rectangle2D(currentX, currentY, currentWidth, currentHeight));
        }
        selectionBox.setVisible(false); // Remove selection box
    }*/
}
