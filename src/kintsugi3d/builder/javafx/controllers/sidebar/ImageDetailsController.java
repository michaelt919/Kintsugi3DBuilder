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
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;

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
    private static final double SCREEN_PIXELS = 100;

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

        // Bind width to detailBox width - 8px padding
        displayImage.fitWidthProperty().bind(detailBox.widthProperty().subtract(8.0));
        //Height is bound to stack panes height
        displayImage.fitHeightProperty().bind(stackPane.heightProperty());

        //Listener detects whenever stack panes width changes
        stackPane.widthProperty().addListener((obs, oldWidth, newWidth) ->
        {
            //If there is a image to display
            if (currentImage != null && displayImage.getViewport() != null)
            {
               updateContainerAspect(); //Call function to update aspect ratio
            }
        });
    }

    /**
     * Takes in the filePath from the Right Bar controller to use as an image in this panel.
     * If filePath is null it will instead hide the detailsBox
     * @param
     */
    public void setImage(String filePath)
    {
        if (filePath != null) //If there is a file path
        {
            //Reveal the image details features
            detailBox.setVisible(true);
            buttonRow.setVisible(true);
            stackPane.setVisible(true);

            File imageFile = new File(filePath); //Creates file from the filePath

            if (imageFile.exists()) //If file exists
            {
                originalImage = new Image(imageFile.toURI().toString()); //Assigns original image with the file
                currentImage = originalImage; //Current image gets set to the original image
                displayImage.setImage(originalImage); //ImageView is set to originalImage

                //Resets viewport
                currentWidth = currentImage.getWidth();
                currentHeight = currentImage.getHeight();
                currentX = 0;
                currentY = 0;

                // Default viewport
                displayImage.setViewport(new Rectangle2D(currentX, currentY, currentWidth, currentHeight));

                updateContainerAspect(); //Update aspect ratio

                Platform.runLater(()->
                {
                    toggleGroup.selectToggle(null); //Removes any button that has been toggled
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
            //Hide the image details features
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
     * This method will return the current aspect ratio that we have for display image
     * @return
     */
    private double getActiveAspect()
    {
        if (displayImage.getViewport() != null) //If we have the image viewport
        {
            //Returns aspect ratio
            return displayImage.getViewport().getWidth() / displayImage.getViewport().getHeight();
        }
        //If we have an image we return current image aspect ratio. If no image is loaded we return 1
        return (currentImage != null) ? (currentImage.getWidth() / currentImage.getHeight()) : 1.0;
    }

    /**
     * This method is called to update the aspect ratio of my container/stack pane
     */

    private void updateContainerAspect()
    {
        if (currentImage == null) return; //If we have no image, exit method

        double activeAspect = getActiveAspect(); //Get current image aspect

        //Find the max screen height by multiplying the screen height by .525 and subtracting 100 (SCREEN_PIXELS)
        double maxScreenHeight = (Screen.getPrimary().getVisualBounds().getHeight() * (.525)) - SCREEN_PIXELS;

        //Gets the minimum value between the original image height and max screen height.
        double maxAllowedHeight = Math.min(originalImage.getHeight(), maxScreenHeight);

        //Unbind the preferred height of stack pane
        stackPane.prefHeightProperty().unbind();

        //Set stackPane preferred height to the max allowed height
        stackPane.setPrefHeight(maxAllowedHeight);
    }

    /**
     * Handles scrolling in the image for zoom scroll up to enlarge down to zoom out.
     * @param event
     */
    @FXML
    public void scrolling(ScrollEvent event)
    {
        //Will exit this function if we have no image or if canZoom is false
        if ((currentImage == null) || !canZoom) return;

        //Gets base dimensions for the image and its aspect ratio
        double imgOrigWidth = currentImage.getWidth();
        double imgOrigHeight = currentImage.getHeight();
        double imageAspect = imgOrigWidth / imgOrigHeight;

        //Gets the dimensions of stack pane and its aspect ratio
        double paneWidth = stackPane.getWidth();
        double paneHeight = stackPane.getHeight();
        double paneAspect = paneWidth / paneHeight;

        //Determine zoom direction
        double zoomFactor = (event.getDeltaY() > 0) ? 0.9 : 1.1;

        //Calculate possible width
        double possibleWidth = currentWidth * zoomFactor;

        //Prevent zooming out past original size
        if (possibleWidth > imgOrigWidth)
        {
            resetViewport();
            event.consume();
            return;
        }

        // Prevent zooming in closer than MAX_ZOOM_SIZE
        if (possibleWidth < MAX_ZOOM_SIZE)
        {
            event.consume();
            return;
        }

        //This fill threshold width determines how fast gray area disappears, .5 is what is set to
        //currently if higher it will take less scrolling to eliminate the gray space
        double fillThresholdWidth = imgOrigWidth * 0.5;

        //Calculates the progress (0.0 = unzoomed, 1.0 = no gray space)
        double fillProgress = (imgOrigWidth - possibleWidth) / (imgOrigWidth - fillThresholdWidth);
        fillProgress = Math.max(0.0, Math.min(1.0, fillProgress));

        //Transition image aspect to stackPane aspect to assign to target Aspect
        double targetAspect = imageAspect + (paneAspect - imageAspect) * fillProgress;

        //Calculate possible height based on target aspect
        double candidateHeight = possibleWidth / targetAspect;

        //Ensure possible height doesn't exceed original image height
        if (candidateHeight > imgOrigHeight)
        {
            candidateHeight = imgOrigHeight;
            possibleWidth = candidateHeight * targetAspect;
        }

        //Adjust X and Y to stay centered on previous viewport center
        double centerX = currentX + (currentWidth / 2.0);
        double centerY = currentY + (currentHeight / 2.0);

        double newX = centerX - (possibleWidth / 2.0);
        double newY = centerY - (candidateHeight / 2.0);

        //Clamp coordinates within image boundaries
        currentX = Math.max(0, Math.min(newX, imgOrigWidth - possibleWidth));
        currentY = Math.max(0, Math.min(newY, imgOrigHeight - candidateHeight));
        currentWidth = possibleWidth;
        currentHeight = candidateHeight;

        //Apply updated viewport
        displayImage.setViewport(new Rectangle2D(currentX, currentY, currentWidth, currentHeight));

        event.consume();
    }

    /**
     * Resets viewport to original settings (No zoom or panning)
     */
    private void resetViewport()
    {
        if (currentImage != null)//If there is an image
        {
            //Reset viewport default coordinates
            currentWidth = currentImage.getWidth();
            currentHeight = currentImage.getHeight();
            currentX = 0;
            currentY = 0;

            //Set default viewport
            displayImage.setViewport(new Rectangle2D(currentX, currentY, currentWidth, currentHeight));

            updateContainerAspect(); //Update aspect ratio
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
        try //Does this if it can, will always do finally statement
        {
            //Checks to see if cropping is enabled (marquee) and looks for if the selection
            //box is wide and tall enough
            if (canCrop && selectionBox.isVisible() && (selectionBox.getWidth() > 5.0) && (selectionBox.getHeight() > 5.0))
            {
                if (currentImage == null) return; //Exits method if there is no image

                //Image bounds within stackPane (accounts for current gray space)
                Bounds imgBounds = displayImage.getBoundsInParent();
                double renderedWidth = imgBounds.getWidth();
                double renderedHeight = imgBounds.getHeight();

                // Current visible viewport dimensions
                Rectangle2D currentVP = displayImage.getViewport();
                double vpWidth = (currentVP != null) ? currentVP.getWidth() : currentImage.getWidth();
                double vpHeight = (currentVP != null) ? currentVP.getHeight() : currentImage.getHeight();
                double vpMinX = (currentVP != null) ? currentVP.getMinX() : 0;
                double vpMinY = (currentVP != null) ? currentVP.getMinY() : 0;

                //Translate selection box relative to actual image inside stackPane
                double relX = selectionBox.getTranslateX() - imgBounds.getMinX();
                double relY = selectionBox.getTranslateY() - imgBounds.getMinY();

                //Scale screen selection pixels to image viewport coordinates
                double scaleX = vpWidth / renderedWidth;
                double scaleY = vpHeight / renderedHeight;

                double selX = vpMinX + (relX * scaleX);
                double selY = vpMinY + (relY * scaleY);
                double selWidth = selectionBox.getWidth() * scaleX;
                double selHeight = selectionBox.getHeight() * scaleY;

                //Get stackPane's aspect ratio to eliminate gray space
                double paneWidth = stackPane.getWidth();
                double paneHeight = stackPane.getHeight();

                if ((paneWidth > 0) && (paneHeight > 0)) //If stack pane isn't 0
                {
                    double paneAspect = paneWidth / paneHeight;
                    double selCenterX = selX + (selWidth / 2.0);
                    double selCenterY = selY + (selHeight / 2.0);

                    //Expand marquee box to fit stackPane's aspect ratio
                    if ((selWidth / selHeight) > paneAspect) // Selection is wider than pane
                    {
                        //Adjust height to match pane aspect
                        selHeight = selWidth / paneAspect;
                    }
                    else //Selection is taller/narrower than pane
                    {
                        //Adjust width to match pane aspect
                        selWidth = selHeight * paneAspect;
                    }

                    //Recenter crop box around original selection center
                    selX = selCenterX - (selWidth / 2.0);
                    selY = selCenterY - (selHeight / 2.0);
                }

                //Clamp coordinates so viewport stays within original image bounds
                double imgOrigWidth = currentImage.getWidth();
                double imgOrigHeight = currentImage.getHeight();

                //Ensure width/height don't exceed image dimensions
                if (selWidth > imgOrigWidth)
                {
                    selWidth = imgOrigWidth;
                    selHeight = selWidth / (paneWidth / paneHeight);
                }
                if (selHeight > imgOrigHeight)
                {
                    selHeight = imgOrigHeight;
                    selWidth = selHeight * (paneWidth / paneHeight);
                }

                //New view port dimensions
                currentX = Math.max(0, Math.min(selX, imgOrigWidth - selWidth));
                currentY = Math.max(0, Math.min(selY, imgOrigHeight - selHeight));
                currentWidth = selWidth;
                currentHeight = selHeight;

                //Apply viewport
                displayImage.setViewport(new Rectangle2D(currentX, currentY, currentWidth, currentHeight));
            }
        }
        finally
        {
            //Hides selection box
            selectionBox.setVisible(false);
            selectionBox.setWidth(0);
            selectionBox.setHeight(0);
        }
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
    }

    /**
     * Returns the imageView display image
     * @return
     */
    public ImageView getDisplayImage()
    {
        return displayImage;
    }

    /**
     * Takes in a boolean and sets stack panes mouse transparency to the opposite
     * @param enable
     */
    public void setMouseInteractionEnabled(boolean enable)
    {
        stackPane.setMouseTransparent(!enable);
    }
}
