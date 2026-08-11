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

package kintsugi3d.builder.javafx.controllers.modals.workflow.tonecalibration;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.IOModel;
import kintsugi3d.builder.core.SampledLuminanceEncoding;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.controllers.modals.LiveProjectSettingsManager;
import kintsugi3d.builder.javafx.controllers.paged.NonDataPageControllerBase;
import kintsugi3d.builder.javafx.controllers.sidebar.ImageDetailsController;
import kintsugi3d.builder.core.RecentProjects;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.util.SRGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.DoubleUnaryOperator;

public class EyedropperController extends NonDataPageControllerBase
{
    private static final Logger LOG = LoggerFactory.getLogger(EyedropperController.class);

    private static final String[] VALID_EXTENSIONS = {"*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"};

    private static final double[] LINEAR_LUMINANCE_VALUES = new double[] { 0.031, 0.090, 0.198, 0.362, 0.591, 0.900 };

    @FXML private VBox eydropperImageRoot;
    @FXML private VBox outerVbox;

    @FXML private GridPane curveValuesRoot;

    @FXML private Pane selectionPane;

    @FXML private Rectangle selectionRectangle;
    @FXML private Rectangle finalSelectRect1, finalSelectRect2, finalSelectRect3, finalSelectRect4, finalSelectRect5, finalSelectRect6;

    @FXML private CheckBox useCurveCheckbox;
    @FXML private CheckBox distanceCompensationCheckbox; // inverse-square attenuation
    @FXML private CheckBox flatfieldCheckbox;

    @FXML private Button chooseImageButton; // appears on top of the image view pane --> visible upon opening
    @FXML private Button chooseNewImageButton; //appears below the color selection txt fields --> hidden upon opening
    @FXML private Button button1, button2, button3, button4, button5, button6;

    @FXML private TextField txtField1, txtField2, txtField3, txtField4, txtField5, txtField6;

    @FXML private Label colorLabel;

    @FXML private ImageDetailsController imageDetailsController;

    private Button sourceButton;

    private List<Rectangle> finalSelectRectangles;

    private List<TextField> colorSelectTxtFields;

    private List<Color> selectedColors;

    static final String DEFAULT_BUTTON_TEXT = "Select Tone Patch";

    private boolean isSelecting; // enabled by "Select Tone Patch" buttons and disabled when selection is finished

    private final ObjectProperty<Image> selectedFile = new SimpleObjectProperty<>(null);
    private final BooleanProperty infiniteLightSources = new SimpleBooleanProperty(); // opposite of distance compensation

    // For flatfield setting
    private final LiveProjectSettingsManager projectSettingsManager = new LiveProjectSettingsManager();

    // Use short to avoid sign issues
    private byte[] prevEncodedLuminanceValues;

    private double anchorX;
    private double anchorY;

    /**
     * Set to true after the first time the warning about using multiple images for tone calibration has been shown
     * to prevent the warning from appearing every time.
     */
    private boolean multiImageWarningShown = false;

    private final Map<Button, TextField> textFieldForButton = new HashMap<>(6);
    private final Map<Button, Rectangle> rectangleForButton = new HashMap<>(6);
    private final Map<Rectangle, TextField> textFieldForRectangle = new HashMap<>(6);
    private final Map<TextField, Rectangle> rectangleForTextField = new HashMap<>(6);

    @Override
    public Region getRootNode()
    {
        return outerVbox;
    }

    @Override
    public void initPage()
    {
        //Image settings/looks
        imageDetailsController.getDisplayImage().setPreserveRatio(true);
        imageDetailsController.getDisplayImage().setSmooth(true);

        isSelecting = false;
        selectionPane.setMouseTransparent(true); //Prevent pane from absorbing clicks

        selectedColors = new ArrayList<>(6);

        colorSelectTxtFields = new ArrayList<>(6);
        colorSelectTxtFields.add(txtField1);
        colorSelectTxtFields.add(txtField2);
        colorSelectTxtFields.add(txtField3);
        colorSelectTxtFields.add(txtField4);
        colorSelectTxtFields.add(txtField5);
        colorSelectTxtFields.add(txtField6);

        finalSelectRectangles = new ArrayList<>(6);
        finalSelectRectangles.add(finalSelectRect1);
        finalSelectRectangles.add(finalSelectRect2);
        finalSelectRectangles.add(finalSelectRect3);
        finalSelectRectangles.add(finalSelectRect4);
        finalSelectRectangles.add(finalSelectRect5);
        finalSelectRectangles.add(finalSelectRect6);

        registerToneWidget(button1, txtField1, finalSelectRect1);
        registerToneWidget(button2, txtField2, finalSelectRect2);
        registerToneWidget(button3, txtField3, finalSelectRect3);
        registerToneWidget(button4, txtField4, finalSelectRect4);
        registerToneWidget(button5, txtField5, finalSelectRect5);
        registerToneWidget(button6, txtField6, finalSelectRect6);

        button1.disableProperty().bind(selectedFile.isNull());
        button2.disableProperty().bind(selectedFile.isNull());
        button3.disableProperty().bind(selectedFile.isNull());
        button4.disableProperty().bind(selectedFile.isNull());
        button5.disableProperty().bind(selectedFile.isNull());
        button6.disableProperty().bind(selectedFile.isNull());

        autoApply();

        eydropperImageRoot.disableProperty().bind(useCurveCheckbox.selectedProperty().not());
        curveValuesRoot.disableProperty().bind(useCurveCheckbox.selectedProperty().not());
        flatfieldCheckbox.disableProperty().bind(distanceCompensationCheckbox.selectedProperty().not());

        useCurveCheckbox.selectedProperty().addListener(obs -> autoApply());
        distanceCompensationCheckbox.selectedProperty().addListener(obs -> autoApply());

        projectSettingsManager.bindBooleanSetting(flatfieldCheckbox, "flatfieldCorrected");

        // Set up inverse relationship
        infiniteLightSources.addListener((obs, oldVal, newVal) ->
            distanceCompensationCheckbox.setSelected(!newVal));
        distanceCompensationCheckbox.selectedProperty().addListener((obs, oldVal, newVal) ->
            infiniteLightSources.set(!newVal));
        projectSettingsManager.bindBooleanSetting(infiniteLightSources, "infiniteLightSources");
        distanceCompensationCheckbox.setSelected(!infiniteLightSources.get());

        setCanAdvance(true);
        setCanConfirm(true);
    }

    @Override
    public void refresh()
    {
        projectSettingsManager.refresh(); // for flatfield setting
        // First time opening the page -- get the old values and use them as the starting values.
        if (prevEncodedLuminanceValues == null)
        {
            //initialize txtFields with their respective values
            if (hasValidIOModel())
            {
                IOModel ioModel = Global.state().getIOModel();

                // Initialize from loaded view set (projectSettingsManager will handle flatfieldCorrected)
                ViewSet viewSet = ioModel.getLoadedViewSet();
                useCurveCheckbox.setSelected(viewSet.hasCustomLuminanceEncoding());

                // Refresh the text fields and retrieve their new values.
                refreshToneValueTextFields(ioModel.getLuminanceEncodingFunction());

                if (viewSet.hasCustomLuminanceEncoding())
                {
                    prevEncodedLuminanceValues = viewSet.getEncodedLuminanceValues();

                    // Check if encoding is not "trivial"
                    // (The previous page will use a trivial encoding to force the tone curve checkbox to be enabled by default
                    // but that should not be retained when cancelling)
                    // If somehow a trivial encoding was previously stored, this could be considered a bit of "housekeeping"
                    // as that encoding has no effect and could have a small impact on memory usage / performance
                    double[] prevLinearLuminanceValues = viewSet.getLinearLuminanceValues();
                    //noinspection FloatingPointEquality
                    if (prevEncodedLuminanceValues.length == 1 && prevLinearLuminanceValues[0] == 1.0 && (0x00FF & prevEncodedLuminanceValues[0]) == 255)
                    {
                        // If the user cancels, this indicates that we should clear the luminance encoding on the view set
                        prevEncodedLuminanceValues = null;
                    }
                }

                for (Rectangle rect : finalSelectRectangles)
                {
                    rect.setVisible(true);
                    updateFinalSelectRect(rect);
                }

                autoApply();
            }
            else
            {
                //TODO: WHAT TO DO IF NO MODEL FOUND?
                LOG.error("Could not bring in luminance encodings: no model found");
            }
        }
        else
        {
            // Navigated to the page again after pressing Back and then Next
            // Keep the values the same but try to auto-apply them as a preview.
            autoApply();
        }

        // Set color checker image
        setImage(getState().getProjectModel().getColorCheckerFile());

        autoApply();
    }

    @Override
    public boolean confirm()
    {
        apply();
        return true;
    }

    @Override
    public boolean cancel()
    {
        if (StaticUtilities.confirmCancel())
        {
            projectSettingsManager.cancel(); // for flatfield setting

            IOModel ioModel = Global.state().getIOModel();
            ioModel.requestLightIntensityCalibration(); // in case "infinite light sources" was toggled

            // revert the tone calibration to what it was when the page was opened.
            if (prevEncodedLuminanceValues != null)
            {
                ioModel.setTonemapping(LINEAR_LUMINANCE_VALUES, prevEncodedLuminanceValues);
            }
            else
            {
                ioModel.clearTonemapping();
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    private void registerToneWidget(Button button, TextField textField, Rectangle rectangle)
    {
        textFieldForButton.put(button, textField);
        rectangleForButton.put(button, rectangle);
        textFieldForRectangle.put(rectangle, textField);
        rectangleForTextField.put(textField, rectangle);
    }

    private static Rectangle2D resetViewport(ImageView imageView)
    {
        //reset the viewport to default value (view entire image)
        Rectangle2D defaultViewport = getDefaultViewport(imageView);

        imageView.setViewport(defaultViewport);

        return defaultViewport;
    }

    private static Rectangle2D getDefaultViewport(ImageView imageView)
    {
        Image image = imageView.getImage();
        return new Rectangle2D(0, 0, image.getWidth(), image.getHeight());
    }

    /**
     * Shows selection rectangle
     * Gets starting coordinates (anchorX & anchorY)
     * Sets selection Rectangle to default values
     * Clears selected colors
     * @param event
     */
    @FXML
    private void handleMousePressed(MouseEvent event)
    {
        if (isSelecting) //In selection state
        {
            //Get original pressed location and set selection rectangle to visible
            selectionRectangle.setVisible(true);
            anchorX = event.getX();
            anchorY = event.getY();

            //Set selection rectangle to default values
            selectionRectangle.setTranslateX(0);
            selectionRectangle.setTranslateY(0);
            selectionRectangle.setX(anchorX);
            selectionRectangle.setY(anchorY);
            selectionRectangle.setWidth(0);
            selectionRectangle.setHeight(0);

            //Clear selected colors
            selectedColors.clear();
        }
    }

    /**
     * If in selection state will update selection rectangle with new coordinates
     * @param event
     */
    @FXML
    private void handleMouseDragged(MouseEvent event)
    {
        if (isSelecting) //In selection state
        {
            //Current mouse locations
            double currentMouseX = event.getX();
            double currentMouseY = event.getY();

            ImageView imageView = imageDetailsController.getDisplayImage(); //ImageView in imageDetails

            // Takes image view coordinates and translates them to selectionPane coordinates
            Bounds imgBoundsInSelectionPane = selectionPane.sceneToLocal(imageView.localToScene(imageView.getBoundsInLocal()));

            //Max and min locations
            double imageMinX = imgBoundsInSelectionPane.getMinX();
            double imageMinY = imgBoundsInSelectionPane.getMinY();
            double imageMaxX = imgBoundsInSelectionPane.getMaxX();
            double imageMaxY = imgBoundsInSelectionPane.getMaxY();

            // Clamp starting anchor dragging coordinates
            double clampedStartX = Math.max(imageMinX, Math.min(anchorX, imageMaxX));
            double clampedStartY = Math.max(imageMinY, Math.min(anchorY, imageMaxY));

            double clampedMouseX = Math.max(imageMinX, Math.min(currentMouseX, imageMaxX));
            double clampedMouseY = Math.max(imageMinY, Math.min(currentMouseY, imageMaxY));

            // Lets selection box be in any direction always uses the min of current spot and
            // original spot to find top left position then gets width and height normally
            double boxX = Math.min(clampedStartX, clampedMouseX);
            double boxY = Math.min(clampedStartY, clampedMouseY);
            double boxWidth = Math.abs(clampedMouseX - clampedStartX);
            double boxHeight = Math.abs(clampedMouseY - clampedStartY);

            //Sets selection rectangle to the coordinates found above
            selectionRectangle.setX(boxX);
            selectionRectangle.setY(boxY);
            selectionRectangle.setWidth(boxWidth);
            selectionRectangle.setHeight(boxHeight);
        }
    }

    /**
     * If in selection state will get avg color from the selection rectangle and will give it
     * color tone then displays color to user and hides the selection rectangle
     */
    @FXML
    private void handleMouseReleased()
    {
        if (isSelecting)
        {
            Color averageColor = getAvgColorFromSelection();

            // Set the color label text
            colorLabel.setText(String.format("Selected Tone [0-255]: %d", Math.round(getGreyScaleDouble(averageColor))));

            //display average color to user
            addSelectedColor(averageColor);

            selectionRectangle.setVisible(false); //Make selection rectangle invisible again
        }
    }

    /**
     * If overlay is active, selectionPane catches mouse events.
     * If not active, ImageDetails catches mouse events
     * @param active
     */
    private void setOverlayActive(boolean active)
    {
        selectionPane.setMouseTransparent(!active); //Mouse transparency set to opposite of active

        //Also sets mouse transparency, but the method in image details reverses it again
        //So while selection pane is active, image detail not active and vis-versa
        imageDetailsController.setMouseInteractionEnabled(!active);

        if (!active)//If active is false
        {
            selectionRectangle.setVisible(false); //selection rectangle is not visible
        }
    }

    private Color getAvgColorFromSelection()
    {
        ImageView imageView = imageDetailsController.getDisplayImage(); //Image details imageview

        Image image = imageView.getImage(); //Actual Image

        if (image == null) //If there is no image
        {
            return Color.BLACK; //Return 0 or Black
        }

        PixelReader pixelReader = image.getPixelReader();

        Rectangle2D viewport = imageView.getViewport();

        if (viewport == null)
        {
            viewport = resetViewport(imageView);
        }

        // Convert selectionRectangle bounds from selectionPane into ImageView coordinates
        Bounds rectBoundsInPane = selectionRectangle.getBoundsInParent();
        Bounds rectBoundsInImg = imageView.sceneToLocal(selectionPane.localToScene(rectBoundsInPane));

        // ImageView coordinates to image pixel coordinates
        double renderedWidth = imageView.getBoundsInLocal().getWidth();
        double renderedHeight = imageView.getBoundsInLocal().getHeight();

        if (renderedWidth <= 0 || renderedHeight <= 0) //No image / clicked on something else
        {
            return Color.BLACK;
        }

        //Scale of viewport and rendered width
        double scaleX = viewport.getWidth() / renderedWidth;
        double scaleY = viewport.getHeight() / renderedHeight;

        //Pixel boundaries
        double trueStartX = viewport.getMinX() + (rectBoundsInImg.getMinX() * scaleX);
        double trueStartY = viewport.getMinY() + (rectBoundsInImg.getMinY() * scaleY);
        double trueEndX   = viewport.getMinX() + (rectBoundsInImg.getMaxX() * scaleX);
        double trueEndY   = viewport.getMinY() + (rectBoundsInImg.getMaxY() * scaleY);

        // Clamp to valid image boundaries
        int startX = Math.max(0, (int) Math.floor(trueStartX));
        int startY = Math.max(0, (int) Math.floor(trueStartY));
        int endX   = Math.min((int) Math.ceil(trueEndX), (int) image.getWidth());
        int endY   = Math.min((int) Math.ceil(trueEndY), (int) image.getHeight());

        selectedColors.clear();

        for (int posX = startX; posX < endX; posX++)
        {
            for (int posY = startY; posY < endY; posY++)
            {
                if (viewport.contains(posX, posY))
                {
                    selectedColors.add(pixelReader.getColor(posX, posY));
                }
            }
        }

        if (selectedColors.isEmpty())
        {
            LOG.warn("No pixels were selected.");
            return Color.BLACK;
        }

        return calculateAverageColor(selectedColors);
    }

    private static double calculateImgViewCroppedScaleFactor(ImageView imageView)
    {
        Rectangle2D viewport = imageView.getViewport();
        if (viewport.getWidth() > viewport.getHeight())
        {
            return viewport.getWidth() / imageView.getFitWidth();
        }
        else
        {
            return viewport.getHeight() / imageView.getFitHeight();
        }
    }

    private static double calculateImgViewScaleFactor(ImageView imgView)
    {
        //getWidth() and getHeight() refer to the full resolution image
        //fitWidth() and fitHeight() refer to the image in the window
        if (imgView.getImage().getWidth() > imgView.getImage().getHeight())
        {
            return imgView.getImage().getWidth() / imgView.getFitWidth();
        }
        else
        {
            return imgView.getImage().getHeight() / imgView.getFitHeight();
        }
    }

    private static Color calculateAverageColor(Collection<Color> colors)
    {
        double redSum = 0;
        double greenSum = 0;
        double blueSum = 0;

        for (Color color : colors)
        {
            redSum += color.getRed();
            greenSum += color.getGreen();
            blueSum += color.getBlue();
        }

        int size = colors.size();
        double averageRed = redSum / size;
        double averageGreen = greenSum / size;
        double averageBlue = blueSum / size;

        return Color.color(averageRed, averageGreen, averageBlue);
    }

    private static double getGreyScaleDouble(Color color)
    {
        //new calculation uses weighted scaling
        double redVal = color.getRed();
        double greenVal = color.getGreen();
        double blueVal = color.getBlue();

        redVal = SRGB.toLinear(redVal);
        greenVal = SRGB.toLinear(greenVal);
        blueVal = SRGB.toLinear(blueVal);

        redVal *= 0.2126729;
        greenVal *= 0.71522;
        blueVal *= 0.0721750;

        double weightedAverageColor = redVal + greenVal + blueVal;
        weightedAverageColor = SRGB.fromLinear(weightedAverageColor);
        return weightedAverageColor * 255;
    }

    //returns false if the color is null or has already been added
    private void addSelectedColor(Color newColor)
    {
        if (sourceButton != null)
        {
            // modify appropriate text field to average greyscale value
            TextField partnerTxtField = textFieldForButton.get(sourceButton);

            // java would use the wrong overload of round() if it used a double
            Integer greyScale = Math.round((float) getGreyScaleDouble(newColor));
            assert partnerTxtField != null;
            partnerTxtField.setText(String.valueOf(greyScale));

            // without these two lines, text field would not update properly
            partnerTxtField.positionCaret(partnerTxtField.getText().length());
            partnerTxtField.positionCaret(0);

            // update square which contains the average color visual for the button
            Rectangle partnerRectangle = rectangleForButton.get(sourceButton);
            updateFinalSelectRect(partnerRectangle);

            // auto-apply if possible
            autoApply();

            sourceButton.getStyleClass().remove("button-selected");
            sourceButton.setText(DEFAULT_BUTTON_TEXT);
            sourceButton = null;

            isSelecting = false;

            setOverlayActive(false);
        }
    }

    private void updateFinalSelectRect(Rectangle rect)
    {
        // when a text field is updated, update the rectangle beside it
        TextField txtField = textFieldForRectangle.get(rect);

        double greyScale;
        if (txtField == null)
        {
            greyScale = 0;
        }
        else
        {
            try
            {
                greyScale = Integer.parseInt(txtField.getText());
            }
            catch (NumberFormatException e)
            {
                greyScale = 0;
            }
        }

        if (greyScale > 255)
        {
            greyScale = 255;
        }

        if (greyScale < 0)
        {
            greyScale = 0;
        }

        double val = greyScale / 255;
        rect.setFill(new Color(val, val, val, 1));
        rect.setVisible(true);
    }

    @FXML
    private void updatesFromTextField(KeyEvent event)
    {
        //whenever a text field is updated, update its partner color rectangle and change the visibility of the Apply button
        //if all text fields contain valid info, make the Apply button functional
        //if not, make the button not functional
        TextField sourceTxtField = (TextField) event.getSource();
        updateFinalSelectRect(rectangleForTextField.get(sourceTxtField));

        autoApply();
    }

    private void autoApply()
    {
        // Only apply if all fields contain good data (integers) and model is loaded
        if (areAllFieldsValid() && hasValidIOModel())
        {
            apply();
        }
    }

    @FXML
    private void apply()
    {
        IOModel ioModel = Global.state().getIOModel();

        // light intensities depend on whether inverse-square attenuation is enabled
        ioModel.requestLightIntensityCalibration();

        if (!useCurveCheckbox.selectedProperty().get())
        {
            // tone curve is disabled
            ioModel.clearTonemapping();
        }
        else if (areAllFieldsValid() && hasValidIOModel()) // check to see if all text fields contain valid input, and model is loaded
        {
            ioModel.setTonemapping(
                LINEAR_LUMINANCE_VALUES,
                new byte[]
                {
                    (byte) Integer.parseInt(txtField1.getText()),
                    (byte) Integer.parseInt(txtField2.getText()),
                    (byte) Integer.parseInt(txtField3.getText()),
                    (byte) Integer.parseInt(txtField4.getText()),
                    (byte) Integer.parseInt(txtField5.getText()),
                    (byte) Integer.parseInt(txtField6.getText())
                });
        }
        else
        {
//            Toolkit.getDefaultToolkit().beep();
            //TODO: PROBABLY CHANGE THIS VERIFICATION METHOD
            LOG.error("Please fill all fields and load a model before performing tone calibration.");
        }
    }

    private boolean areAllFieldsValid()
    {
        // only return true if all text fields are filled with good info (integers)
        for (TextField field : colorSelectTxtFields)
        {   //TODO: CHECK IF VALS ARE 0-255?
            if (!field.getText().matches("-?\\d+"))
            {
                // regex to check if input is integer
                return false;
            }
        }
        return true;
    }

    @FXML
    private void enterColorSelectionMode(ActionEvent actionEvent)
    {
        if (sourceButton != null)
        {
            // In case we were already selecting a different patch?
            sourceButton.getStyleClass().remove("button-selected");
            sourceButton.setText(DEFAULT_BUTTON_TEXT);
        }

        //change text of button to indicate selection
        sourceButton = (Button) actionEvent.getSource();
//        resetButtonsText();

        sourceButton.setText("Draw to select...");

        sourceButton.getStyleClass().add("button-selected");

        isSelecting = true;
        setOverlayActive(true);
    }

    private void refreshToneValueTextFields(DoubleUnaryOperator luminanceEncoding)
    {
        // Calculate starting encoded luminance values and remember them so we can cancel.
        byte[] newEncodedLuminanceValues = new byte[LINEAR_LUMINANCE_VALUES.length];
        for (int i = 0; i < LINEAR_LUMINANCE_VALUES.length; i++)
        {
            newEncodedLuminanceValues[i] = (byte) Math.round(luminanceEncoding.applyAsDouble(LINEAR_LUMINANCE_VALUES[i]));
        }

        // Put values into text fields.
        // encoded luminance values are essentially unsigned bytes but will be interpreted as signed unless we're explicit about it.
        txtField1.setText(Integer.toString(0x00FF & newEncodedLuminanceValues[0]));
        txtField2.setText(Integer.toString(0x00FF & newEncodedLuminanceValues[1]));
        txtField3.setText(Integer.toString(0x00FF & newEncodedLuminanceValues[2]));
        txtField4.setText(Integer.toString(0x00FF & newEncodedLuminanceValues[3]));
        txtField5.setText(Integer.toString(0x00FF & newEncodedLuminanceValues[4]));
        txtField6.setText(Integer.toString(0x00FF & newEncodedLuminanceValues[5]));
    }

    private static boolean hasValidIOModel()
    {
        return Global.state().getIOModel().hasLoadedRenderable();
    }

    @FXML
    private void selectImage(ActionEvent actionEvent)
    {
        if (!multiImageWarningShown)
        {
            Alert alert = new Alert(AlertType.WARNING,
                "Warning: using multiple images for tone calibration can result in inconsistencies in tone interpretation.  To be used for advanced workflows only.",
                ButtonType.OK, ButtonType.CANCEL);
            alert.setGraphic(null);
            var result = alert.showAndWait();
            if (result.isEmpty() || !result.get().equals(ButtonType.OK))
            {
                // User cancelled; do not select a new image
                return;
            }
            else
            {
                // User confirmed; do not show warning again while this controller is active.
                multiImageWarningShown = true;
            }
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Image File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", VALID_EXTENSIONS));
        fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());

        try
        {
            fileChooser.setInitialDirectory(Global.state().getIOModel().getLoadedViewSet().getFullResImageFile(0).getParentFile());
        }
        catch (NullPointerException e)
        {
            Alert alert = new Alert(AlertType.ERROR, "Please load a model before using the color checker.");
            alert.setGraphic(null);
            alert.show();
            return;
        }

        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        setImage(file);
    }

    private void setImage(File file)
    {
        if (file != null)
        {
            RecentProjects.setMostRecentDirectory(file.getParentFile());

            //convert tiff image if necessary
            if (file.getAbsolutePath().toLowerCase(Locale.ROOT).matches(".*\\.tiff?"))
            {
                try
                {
                    imageDetailsController.setImage(file.getPath());
                    BufferedImage bufferedImage = ImageHelper.read(file).getBufferedImage();
                }
                catch (IOException e)
                {
                    LOG.error("Could not convert tif image: ", e);
                }
            }
            else
            {
                selectedFile.set(new Image(file.toURI().toString()));
                imageDetailsController.setImage(file.getPath());
            }

            //update buttons
            chooseImageButton.setVisible(false);
            chooseNewImageButton.setVisible(true);

            //testing the code for saving the file
            //Note: Code bellow saves the file however it's not audiomatic. The user has to select where to save it and name the file as well.
            //Stage secondStage = new Stage();
            //File savefile = fileChooser.showSaveDialog(secondStage);
            //fileChooser.setInitialFileName("colorPickerImage");

            //This saves the file to the location path listed
            try
            {
                getState().getProjectModel().setColorCheckerFile(new File(file.getPath()));
            }
            catch (RuntimeException e)
            {
                LOG.error("Could not save file");
            }
        }
    }

    public void reset()
    {
        IOModel ioModel = Global.state().getIOModel();

        // Clear tonemapping and reset text fields to a standard curve
        ioModel.clearTonemapping();
        refreshToneValueTextFields(new SampledLuminanceEncoding().encodeFunction);

        // Disable curve
        useCurveCheckbox.setSelected(false);

        // Enable distance compensation (i.e. disable "infinite light sources") by default
        distanceCompensationCheckbox.setSelected(true);

        projectSettingsManager.resetSettingsToDefaults(); // reset flatfield setting
    }
}
