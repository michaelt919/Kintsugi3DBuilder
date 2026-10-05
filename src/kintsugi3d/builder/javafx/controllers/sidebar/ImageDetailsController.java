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
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import kintsugi3d.gl.util.ImageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class ImageDetailsController
{
    private static final Logger LOG = LoggerFactory.getLogger(ImageDetailsController.class);
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

    private static final double MIN_ZOOM_VIEWPORT_SIZE = 10.0;

    /**
     * Called when controller is created.
     * Calls setImage() with null.
     * Creates buttons using createButtons(), then binds their width to be equal.
     * Display Image is always the size of detailBox minus 8 for padding.
     * Adds clip to prevent long crop images.
     */
    public void initialize()
    {
        setImage(null); // Sets default state to not shown.

        Collection<Region> buttons = createButtons(); // Creates all buttons using method. Can add new buttons there as well.

        for (Region button : buttons) // For every button in buttonRow
        {
            // Bind buttons to each other so they all take up same size
            button.prefWidthProperty().bind(buttonRow.widthProperty()
                .subtract(buttonRow.getSpacing()*(buttonRow.getChildren().size() - 1))
                .divide(buttonRow.getChildren().size()));
        }

        // Bind width to detailBox width - 8px padding
        // For some reason this only works if we bind it to detailBox, not to stackPane.
        // (stackPane seems to involve displayImage in its width calculation which creates a cycle)
        displayImage.fitWidthProperty().bind(detailBox.widthProperty()
            .subtract(detailBox.getPadding().getLeft() + detailBox.getPadding().getRight()));

        // Height is bound to stack panes height
        displayImage.fitHeightProperty().bind(stackPane.heightProperty());

        stackPane.widthProperty().addListener((obs, oldValue, newValue) ->
            // Use Platform.runLater since setting prefHeight during its own layout pass seems to cause problems.
            Platform.runLater(() ->
            {
                stackPane.setPrefHeight(newValue.doubleValue());

                // Aspect ratio of the stackPane could change if it's limited by the window height.
                adjustViewportWidth();
            }));

        // Listener detects whenever stack pane's height changes
        // This handles situations when the window is resized
        // and the aspect ratio of the stackPane might correspondingly change.
        stackPane.heightProperty().addListener((obs, oldValue, newValue) ->
            // Call function to update viewport
            // Use Platform.runLater to ensure that the state is consistent
            // (i.e. not in a situation where height was updated but width update still needs to happen)
            adjustViewportWidth());
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
                // convert tiff image if necessary
                if (imageFile.getAbsolutePath().toLowerCase(Locale.ROOT).matches(".*\\.tiff?"))
                {
                    try
                    {
                        BufferedImage bufferedImage = ImageHelper.read(imageFile).getBufferedImage();
                        originalImage = SwingFXUtils.toFXImage(bufferedImage, null);
                    }
                    catch (IOException e)
                    {
                        LOG.error("Could not convert tiff image: ", e);
                    }
                }
                else
                {
                    originalImage = new Image(imageFile.toURI().toString()); //Assigns original image with the file
                }

                currentImage = originalImage; //Current image gets set to the original image
                displayImage.setImage(originalImage); //ImageView is set to originalImage

                //Resets viewport
                currentWidth = currentImage.getWidth();
                currentHeight = currentImage.getHeight();
                currentX = 0;
                currentY = 0;

                // Default viewport
                refreshViewport();

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
    private Collection<Region> createButtons()
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

        return List.of(zoom, pan, marquee, reset);
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
     * This method is for adjusting the image when the panel is being resized.
     * To avoid zoom drift, we lock the viewport height as unchanging and adjust the width to match current aspect ratio.
     * (Any alternative that adjusts both width and height would have the undesirable effect that alternating changes
     * to width and height can over time result in persistent changes to the viewport even if the aspect ratio comes
     * back to the original value).
     */
    private void adjustViewportWidth()
    {
        // Make sure that there is an image and that the stack pane has positive width and height
        if (currentImage != null && displayImage.getViewport() != null &&
            stackPane.getWidth() > 0 && stackPane.getHeight() > 0)
        {
            // Get the image width
            double imgOrigWidth = currentImage.getWidth();

            // Adjustment is only needed if we are zoomed in / cropped.
            // (otherwise, simple letterboxing works fine without this)
            if (currentWidth < imgOrigWidth - 0.5 || currentHeight < currentImage.getHeight() - 0.5)
            {
                // Adjust width to fill horizontal space using the pane's new aspect ratio.
                // Clamp dimensions so they do not exceed the actual image bounds
                double newWidth = calculateViewportWidthFromHeight(currentHeight);

                // Update the x-coordinate for the new width
                currentX = adjustCoordinate(currentX, imgOrigWidth, currentWidth, newWidth);

                currentWidth = newWidth;
            }
        }

        // Apply the viewport update
        refreshViewport();
    }

    private double calculateViewportWidthFromHeight(double height)
    {
        return Math.min(currentImage.getWidth(), height * (stackPane.getWidth() / stackPane.getHeight()));
    }

    private double calculateViewportHeightFromWidth(double width)
    {
        return Math.min(currentImage.getHeight(), width * (stackPane.getHeight() / stackPane.getWidth()));
    }


    private void refreshViewport()
    {
        displayImage.setViewport(new Rectangle2D(currentX, currentY, currentWidth, currentHeight));
    }

    /**
     * Handles scrolling in the image for zoom scroll up to enlarge down to zoom out.
     * @param event
     */
    @FXML
    public void scrollZoom(ScrollEvent event)
    {
        // Will exit this function if we have no image or if canZoom is false
        if (ScrollEvent.SCROLL.equals(event.getEventType()) && event.getDeltaY() != 0.0 && currentImage != null && canZoom)
        {
            // Gets base dimensions for the image and its aspect ratio
            double imgOrigWidth = currentImage.getWidth();
            double imgOrigHeight = currentImage.getHeight();

            // Gets the height of the stack pane
            double paneHeight = stackPane.getHeight();

            // Determine zoom (1 - normalized pixel size raised to the power of Delta Y)
            double zoomFactor = Math.pow((paneHeight - 1.0) / paneHeight, event.getDeltaY());

            // Calculate anticipated new width and height.
            // Ensure that we're not zooming out beyond the full image size
            // or zooming in closer than MIN_ZOOM_VIEWPORT_SIZE.
            // Round to avoid complicated numerical precision issues.
            double heightFromZoom = Math.max(MIN_ZOOM_VIEWPORT_SIZE,
                Math.min(imgOrigHeight, currentHeight * zoomFactor));

            // Compare width from zooming with width derived from height via aspect ratio
            double widthFromZoom = Math.min(imgOrigWidth, currentWidth * zoomFactor);
            double widthFromHeight = calculateViewportWidthFromHeight(heightFromZoom);

            if (widthFromHeight + 0.5 < widthFromZoom)
            {
                // Width was zoomed significantly more than height.
                // This indicates that the vertical axis is not our limiting factor (after zoom has been applied)
                // and we should have applied zoom to horizontal axis instead.
                // Therefore, apply zoom to width rather than height.
                double heightFromWidth = calculateViewportHeightFromWidth(widthFromZoom);

                // Adjust x to stay centered on previous viewport center
                currentX = adjustCoordinate(currentX, imgOrigWidth, currentWidth, widthFromZoom);
                currentY = adjustCoordinate(currentY, imgOrigHeight, currentHeight, heightFromWidth);

                currentWidth = widthFromZoom;
                currentHeight = heightFromWidth;

                // Send the changes to the viewport
                refreshViewport();
            }
            else
            {
                // Adjust y to stay centered on previous viewport center
                currentX = adjustCoordinate(currentX, imgOrigWidth, currentWidth, widthFromHeight);
                currentY = adjustCoordinate(currentY, imgOrigHeight, currentHeight, heightFromZoom);

                // Zoom "normally" via height
                currentWidth = widthFromHeight;
                currentHeight = heightFromZoom;

                // Send the changes to the viewport
                refreshViewport();
            }

            event.consume();
        }
    }

    private static double adjustCoordinate(double currentValue, double maxValue, double oldDimension, double newDimension)
    {
        return Math.max(0, Math.min(maxValue - newDimension, // Clamp coordinates within image boundaries
            currentValue + oldDimension / 2.0 - newDimension / 2.0));
    }

    /**
     * Resets viewport to original settings (No zoom or panning)
     */
    private void resetViewport()
    {
        if (currentImage != null) // If there is an image
        {
            // Reset viewport default coordinates
            currentWidth = currentImage.getWidth();
            currentHeight = currentImage.getHeight();
            currentX = 0;
            currentY = 0;

            // Set default viewport
            refreshViewport();
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
            refreshViewport();
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
        if (currentImage != null) // Safeguard if image is there
        {
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
                refreshViewport();
            }
            else if (canCrop) // If cropping
            {
                // Gets the max and min X and Y we can be at
                double imageMinX = displayImage.getBoundsInParent().getMinX();
                double imageMinY = displayImage.getBoundsInParent().getMinY();
                double imageMaxX = displayImage.getBoundsInParent().getMaxX() - selectionBox.getStrokeWidth();
                double imageMaxY = displayImage.getBoundsInParent().getMaxY() - selectionBox.getStrokeWidth();

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
                if (currentImage != null) // Exits method if there is no image
                {
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
                    refreshViewport();
                }
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
    private void zoom()
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
    private void pan()
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
    private void marquee()
    {
        canCrop = true;
        canPan = false;
        canZoom = false;
    }

    /**
     * Method to handle reset button.
     * Sets booleans for crop, pan, and zoom to false.
     * Resets viewport.
     * Sets selectionBox to invisible and currentImage to originalImage.
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
