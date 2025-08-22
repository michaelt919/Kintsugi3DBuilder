/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.modals;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.IOModel;
import kintsugi3d.builder.javafx.controllers.paged.NonDataPageControllerBase;
import kintsugi3d.builder.javafx.core.RecentProjects;
import kintsugi3d.builder.javafx.internal.ProjectModelBase;
import kintsugi3d.util.SRGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.DoubleUnaryOperator;

public class EyedropperController extends NonDataPageControllerBase implements Initializable
{
    private static final Logger LOG = LoggerFactory.getLogger(EyedropperController.class);

    static final String[] validExtensions = {"*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"};

    @FXML private Button chooseImageButton; // appears on top of the image view pane --> visible upon opening
    @FXML private Button chooseNewImageButton; //appears below the color selection txt fields --> hidden upon opening
    @FXML private Button cropButton; //appears below the choose new image button --> hidden upon opening

    @FXML
    private Rectangle selectionRectangle;

    @FXML
    private Rectangle finalSelectRect1, finalSelectRect2, finalSelectRect3, finalSelectRect4, finalSelectRect5, finalSelectRect6;
    private List<Rectangle> finalSelectRectangles;

    @FXML
    private Button button1, button2, button3, button4, button5, button6;

    @FXML
    private Button applyButton;
    private boolean isCropping;//enabled by crop button and disabled when cropping is finished
    private boolean isSelecting;//enabled by "Select Tone Patch" buttons and disabled when selection is finished
    private boolean canResetCrop; //enabled when cropping is finished and disabled when crop is reset to default viewport
    static final String DEFAULT_BUTTON_TEXT = "Select Tone Patch";

    @FXML
    private TextField txtField1, txtField2, txtField3, txtField4, txtField5, txtField6;
    private List<TextField> colorSelectTxtFields;

    @FXML
    private Label colorLabel;

    private List<Color> selectedColors;
    @FXML private ImageView colorPickerImgView;
    private Image selectedFile;

    @FXML private VBox outerVbox;

    private IOModel ioModel = new IOModel();
    private ProjectModelBase projectModel = null;

    private Button sourceButton;

    /**
     * Set to true after the first time the warning about using multiple images for tone calibration has been shown
     * to prevent the warning from appearing every time.
     */
    private boolean multiImageWarningShown = false;

    /**
     * Set to true to prevent warning about unsaved changes when closing the window via the "Confirm" button.
     */
    private boolean confirmExit = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        initPage();
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

    @FXML
    private void handleMousePressed(MouseEvent event)
    {
        //TODO: IF USER SELECTS AREA OUTSIDE OF IMAGE, SHIFT SELECTION BOX INSIDE IMAGE
        if (isSelecting || isCropping)
        {
            selectionRectangle.setVisible(true);
            double x = event.getX();
            double y = event.getY();
            selectionRectangle.setX(x);
            selectionRectangle.setY(y);
            selectionRectangle.setWidth(0);
            selectionRectangle.setHeight(0);
            selectedColors.clear();
        }
    }

    @FXML
    private void handleMouseDragged(MouseEvent event)
    {
        if (isSelecting || isCropping)
        {
            double x = event.getX();
            double y = event.getY();
            double width = x - selectionRectangle.getX();
            double height = y - selectionRectangle.getY();

            selectionRectangle.setWidth(width);
            selectionRectangle.setHeight(height);
        }
    }

    @FXML
    private void handleMouseReleased(MouseEvent event)
    {
        if (isSelecting)
        {
            Color averageColor = getAvgColorFromSelection();

            // Set the color label text
            colorLabel.setText("Selected Tone [0-255]: " + Math.round(getGreyScaleDouble(averageColor)));

            //display average color to user, change text for corresponding text field
            addSelectedColor(averageColor);
        }

        if (isCropping)
        {
            canResetCrop = true;
            cropImage();
        }
    }

    @FXML
    private void enterCropMode()
    {
        //same button is used for cropping and resetting cropping
        if (canResetCrop)
        {//button text is "Reset Crop"
            resetCrop();
        }
        else
        {//button text is "Crop"
            cropButton.setText("Cropping...");
            cropButton.getStyleClass().add("button-selected");
            isCropping = true;
        }

//        resetButtonsText();
        isSelecting = false;
        selectionRectangle.setVisible(false);
    }

    private void resetCrop()
    {
        resetViewport(colorPickerImgView);
        cropButton.setText("Crop");
        canResetCrop = false;
    }

    private void cropImage()
    {
        //get bounds of selection rectangle
        //crop imageView accordingly
        double scaleFactor = calculateImgViewScaleFactor(colorPickerImgView);
        double width = selectionRectangle.getWidth() * scaleFactor;
        double height = selectionRectangle.getHeight() * scaleFactor;
        double x = (selectionRectangle.getX() - colorPickerImgView.getLayoutX()) * scaleFactor;
        double y = (selectionRectangle.getY() - colorPickerImgView.getLayoutY()) * scaleFactor;

        Rectangle2D view = new Rectangle2D(x, y, width, height);
        colorPickerImgView.setViewport(view);
        isCropping = false;
        cropButton.setText("Reset Crop");
        cropButton.getStyleClass().remove("button-selected");

        selectionRectangle.setVisible(false);
    }

    private Color getAvgColorFromSelection()
    {
        double x = selectionRectangle.getX();//(x, y) is the top left corner of the selectionRectangle in windowspace
        double y = selectionRectangle.getY();
        double width = selectionRectangle.getWidth();
        double height = selectionRectangle.getHeight();

        javafx.scene.image.PixelReader pixelReader = colorPickerImgView.getImage().getPixelReader();

        Rectangle2D viewport = colorPickerImgView.getViewport();
        if (viewport == null)
        {
            viewport = resetViewport(colorPickerImgView);
        }

        double scaleFactor;
        if (viewport == getDefaultViewport(colorPickerImgView))
        {//use this scaleFactor when image is not cropped
            scaleFactor = calculateImgViewScaleFactor(colorPickerImgView);
        }
        else
        {
            scaleFactor = calculateImgViewCroppedScaleFactor(colorPickerImgView);
        }


        //getLayoutX(), getLayoutY() refer to top left corner of image in windowspace
        double trueStartX = (x - colorPickerImgView.getLayoutX()) * scaleFactor;
        double trueStartY = (y - colorPickerImgView.getLayoutY()) * scaleFactor;

        double trueEndX = trueStartX + width * scaleFactor;
        double trueEndY = trueStartY + height * scaleFactor;

        trueStartX += viewport.getMinX();//viewport may be offset from the top left corner, this counters that offset
        trueStartY += viewport.getMinY();
        trueEndX += viewport.getMinX();
        trueEndY += viewport.getMinY();

        //read pixels from selected crop
        selectedColors.clear();
        boolean badColorDetected = false;
        for (int posX = (int) trueStartX; posX < trueEndX; posX++)
        {
            for (int posY = (int) trueStartY; posY < trueEndY; posY++)
            {
                try
                {
                    if (viewport.contains(posX, posY))
                    {//only add color if it is inside the viewport (visible to user)
                        Color color = pixelReader.getColor(posX, posY);
                        selectedColors.add(color);
                    }
                    else
                    {
                        badColorDetected = true;
                    }
                }
                catch (IndexOutOfBoundsException e)
                {
                    badColorDetected = true;
                }
            }
        }

        if (badColorDetected)
        {//TODO: CHANGE SOLUTION TO THIS PROBLEM?
            LOG.info("Some colors could not be added. Please confirm that your selection contains the desired colors.");
        }

        return calculateAverageColor(selectedColors);
    }

    private double calculateImgViewCroppedScaleFactor(ImageView imageView)
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

    private double calculateImgViewScaleFactor(ImageView imgView)
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

    private Color calculateAverageColor(List<Color> colors)
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

    private double getGreyScaleDouble(Color color)
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
    @FXML
    private boolean addSelectedColor(Color newColor)
    {
        //update the text field (to int. greyscale value) and its corresponding color square
//        Button sourceButton = resetButtonsText();

        if (sourceButton != null)
        {
            //modify appropriate text field to average greyscale value
            TextField partnerTxtField = getTextFieldForButton(sourceButton);

            //java would use the wrong overload of round() if it used a double
            Integer greyScale = Math.round((float) getGreyScaleDouble(newColor));
            assert partnerTxtField != null;
            partnerTxtField.setText(String.valueOf(greyScale));

            //without these two lines, text field would not update properly
            partnerTxtField.positionCaret(partnerTxtField.getText().length());
            partnerTxtField.positionCaret(0);

            //update square which contains the average color visual for the button
            Rectangle partnerRectangle = getRectangleForButton(sourceButton);
            updateFinalSelectRect(partnerRectangle);

            //disable/enable apply button as needed
            updateApplyButton();

            sourceButton.getStyleClass().remove("button-selected");
            sourceButton.setText(DEFAULT_BUTTON_TEXT);
        }
        else
        {
            Toolkit.getDefaultToolkit().beep();
            return false; //source button is null
        }
        isSelecting = false;
        sourceButton = null;
        return true;//color changed successfully
    }

    private void updateFinalSelectRect(Rectangle rect)
    {//when a text field is updated, update the rectangle beside it
        TextField txtField = getTextFieldForRectangle(rect);

        double greyScale;
        try
        {
            greyScale = Integer.parseInt(txtField.getText());
        }
        catch (NumberFormatException | NullPointerException e)
        {
            greyScale = 0;
        }

        if (greyScale > 255)
        {
            greyScale = 255;
        }

        if (greyScale < 0)
        {
            greyScale = 0;
        }

        double val = greyScale / (255);
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
        updateFinalSelectRect(getRectangleForTextField(sourceTxtField));

        updateApplyButton();
    }

    public void updateApplyButton()
    {
        //only enable apply button if all fields contain good data (integers) and model is loaded
        if (areAllFieldsValid() && hasGoodLoadingModel())
        {
            //TODO: KEEPS BUTTON DISABLED IF NEW MODEL IS LOADED IN
            //only updates setDisable() when text field is modified
            applyButton.setDisable(false);
        }
        else
        {
            applyButton.setDisable(true);
        }
    }

    //returns the text field the button corresponds to
    private TextField getTextFieldForButton(Button sourceButton)
    {
        switch (sourceButton.getId())
        {
            case "button1":
                return txtField1;
            case "button2":
                return txtField2;
            case "button3":
                return txtField3;
            case "button4":
                return txtField4;
            case "button5":
                return txtField5;
            case "button6":
                return txtField6;
            default:
                return null;
        }
    }

    private Rectangle getRectangleForButton(Button sourceButton)
    {
        switch (sourceButton.getId())
        {
            case "button1":
                return finalSelectRect1;
            case "button2":
                return finalSelectRect2;
            case "button3":
                return finalSelectRect3;
            case "button4":
                return finalSelectRect4;
            case "button5":
                return finalSelectRect5;
            case "button6":
                return finalSelectRect6;
            default:
                return null;
        }
    }

    private TextField getTextFieldForRectangle(Rectangle rect)
    {
        switch (rect.getId())
        {
            case "finalSelectRect1":
                return txtField1;
            case "finalSelectRect2":
                return txtField2;
            case "finalSelectRect3":
                return txtField3;
            case "finalSelectRect4":
                return txtField4;
            case "finalSelectRect5":
                return txtField5;
            case "finalSelectRect6":
                return txtField6;
            default:
                return null;
        }
    }

    private Rectangle getRectangleForTextField(TextField txtField)
    {
        switch (txtField.getId())
        {
            case "txtField1":
                return finalSelectRect1;
            case "txtField2":
                return finalSelectRect2;
            case "txtField3":
                return finalSelectRect3;
            case "txtField4":
                return finalSelectRect4;
            case "txtField5":
                return finalSelectRect5;
            case "txtField6":
                return finalSelectRect6;
            default:
                return null;
        }
    }

    @FXML
    private void applyButtonPressed()
    {
        //check to see if all text fields contain valid input, and model is loaded
        if (areAllFieldsValid() && hasGoodLoadingModel())
        {
            ioModel.setTonemapping(
                new double[]{0.031, 0.090, 0.198, 0.362, 0.591, 0.900},
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
            Toolkit.getDefaultToolkit().beep();
            //TODO: PROBABLY CHANGE THIS VERIFICATION METHOD
            LOG.error("Please fill all fields and load a model before performing color calibration.");
        }
    }

    private boolean areAllFieldsValid()
    {
        //only return true if all text fields are filled with good info (integers)
        for (TextField field : colorSelectTxtFields)
        {//TODO: CHECK IF VALS ARE 0-255?
            if (!field.getText().matches("-?\\d+"))
            {//regex to check if input is integer
                return false;
            }
        }
        return true;
    }

    @FXML
    private void enterColorSelectionMode(ActionEvent actionEvent)
    {

        if (cropButton.getStyleClass().contains("button-selected"))
        {
            // In case crop had started but not finished
            cropButton.setText("Crop");
            cropButton.getStyleClass().remove("button-selected");
        }

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
        isCropping = false;
    }

//    private Button resetButtonsText(){
//        Button sourceButton = null;
//        for (Button button: colorSelectButtons){
//            if (!button.getText().equals(DEFAULT_BUTTON_TEXT)) {
//                sourceButton = button;
//            }

    /// /            button.setText(DEFAULT_BUTTON_TEXT);
//        }
//
//        return sourceButton;
//    }

    public void setIOModel(IOModel ioModel)
    {
        this.ioModel = ioModel;

        //initialize txtFields with their respective values
        if (hasGoodLoadingModel())
        {
            DoubleUnaryOperator luminanceEncoding = ioModel.getLuminanceEncodingFunction();
            txtField1.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.031))));
            txtField2.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.090))));
            txtField3.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.198))));
            txtField4.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.362))));
            txtField5.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.591))));
            txtField6.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.900))));

            for (Rectangle rect : finalSelectRectangles)
            {
                rect.setVisible(true);
                updateFinalSelectRect(rect);
            }

            updateApplyButton();
        }
        else
        {
            //TODO: WHAT TO DO IF NO MODEL FOUND?
            LOG.error("Could not bring in luminance encodings: no model found");
        }
    }

    private boolean hasGoodLoadingModel()
    {
        return ioModel.hasValidHandler();
    }

//    public void ExitEyeDropper(){
//        if (exitCallback != null)
//        {
//            exitCallback.run();
//        }
//    }

    @FXML
    private void selectImage(ActionEvent actionEvent)
    {
        if (!multiImageWarningShown)
        {
            Alert alert = new Alert(AlertType.WARNING,
                "Warning: using multiple images for tone calibration can result in inconsistencies in tone interpretation.  "
                    + "To be used for advanced workflows only.",
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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", validExtensions));
        fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());

        try
        {
            fileChooser.setInitialDirectory(ioModel.getLoadedViewSet().getFullResImageFile(0).getParentFile());
        }
        catch (NullPointerException e)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please load a model before using the color checker.");
            alert.setGraphic(null);
            alert.show();
            return;
        }

        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        setImage(file);
    }

    public void setImage(File file)
    {
        if (file != null)
        {
            RecentProjects.setMostRecentDirectory(file.getParentFile());

            //convert tiff image if necessary
            if (file.getAbsolutePath().toLowerCase().matches(".*\\.tiff?"))
            {
                BufferedImage bufferedImage;
                try
                {
                    bufferedImage = ImageIO.read(file);
                    colorPickerImgView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
                }
                catch (IOException e)
                {
                    LOG.error("Could not convert tif image: ", e);
                }
            }
            else
            {
                selectedFile = new Image(file.toURI().toString());
                colorPickerImgView.setImage(selectedFile);
            }

            //update buttons
            chooseImageButton.setVisible(false);
            chooseNewImageButton.setVisible(true);
            cropButton.setVisible(true);

            //testing the code for saving the file
            //Note: Code bellow saves the file however it's not audiomatic. The user has to select where to save it and name the file as well.
            //Stage secondStage = new Stage();
            //File savefile = fileChooser.showSaveDialog(secondStage);
            //fileChooser.setInitialFileName("colorPickerImage");

            //This saves the file to the location path listed
            String path = file.getPath();
            try
            {
                if (projectModel != null)
                {
                    projectModel.setColorCheckerFile(new File(path));
                }
            }
            catch (Exception e)
            {
                LOG.error("Could not save file");
            }

            //reset viewport and crop button text
            resetCrop();
        }
    }

    @Override
    public Region getRootNode()
    {
        return outerVbox;
    }

    @Override
    public void initPage()
    {
        selectedFile = null;

        colorPickerImgView.setPreserveRatio(true);
        colorPickerImgView.setSmooth(true);

        isSelecting = false;
        isCropping = false;
        canResetCrop = false;

        selectedColors = new ArrayList<>();

        colorSelectTxtFields = new ArrayList<>();
        colorSelectTxtFields.add(txtField1);
        colorSelectTxtFields.add(txtField2);
        colorSelectTxtFields.add(txtField3);
        colorSelectTxtFields.add(txtField4);
        colorSelectTxtFields.add(txtField5);
        colorSelectTxtFields.add(txtField6);

        finalSelectRectangles = new ArrayList<>();
        finalSelectRectangles.add(finalSelectRect1);
        finalSelectRectangles.add(finalSelectRect2);
        finalSelectRectangles.add(finalSelectRect3);
        finalSelectRectangles.add(finalSelectRect4);
        finalSelectRectangles.add(finalSelectRect5);
        finalSelectRectangles.add(finalSelectRect6);

        updateApplyButton();

        setCanAdvance(true);
        setCanConfirm(true);
    }

    @Override
    public void refresh()
    {
        setIOModel(Global.state().getIOModel());
        setImage(getPageFrameController().getState().getProjectModel().getColorCheckerFile());

        updateApplyButton();
    }

    @Override
    public boolean confirm()
    {
        applyButtonPressed();

        // Suppress warning about unsaved changes since the changes were just applied automatically.
        confirmExit = true;

        return true;
    }

    @Override
    public boolean close()
    {
        // Suppress warning about unsaved changes since the changes were just applied automatically.
        if (confirmExit)
        {
            return true;
        }
        else
        {
            //TODO: shouldn't this revert to the previous color checker settings?
            Alert alert = new Alert(AlertType.CONFIRMATION, "Discard tone calibration changes?");
            var result = alert.showAndWait();
            return result.isPresent() && result.get().equals(ButtonType.OK);
        }
    }
}
