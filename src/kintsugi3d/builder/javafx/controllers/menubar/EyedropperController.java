package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.builder.core.LoadingModel;

import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.DoubleUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EyedropperController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(EyedropperController.class);

    @FXML
    private Rectangle selectionRectangle;

    @FXML
    private Rectangle paletteColor0, paletteColor1, paletteColor2, paletteColor3, paletteColor4, paletteColor5, paletteColor6,
            paletteColor7, paletteColor8, paletteColor9, paletteColor10, paletteColor11, paletteColor12, paletteColor13, paletteColor14;
    private List<Rectangle> paletteColorRectangles;
    private int selectedColorInsertIndex; //first index in color palette which does not have a color assigned to it

    @FXML
    private Rectangle finalSelectRect1, finalSelectRect2, finalSelectRect3, finalSelectRect4, finalSelectRect5, finalSelectRect6;
    private List<Rectangle> finalSelectRectangles;

    @FXML
    private Button button1, button2, button3, button4, button5, button6;
    private List<Button> colorSelectButtons;

    @FXML
    private Button cropButton;

    @FXML
    private Button applyButton;
    private boolean isCropping;//enabled by crop button and disabled when cropping is finished
    private boolean isSelecting;//enabled by "Select Color" buttons and disabled when selection is finished
    private boolean canResetCrop; //enabled when cropping is finished and disabled when crop is reset to default viewport
    private boolean firstColorSelected;

    static final String DEFAULT_BUTTON_TEXT = "Select Color";

    @FXML
    private TextField txtField1, txtField2, txtField3, txtField4, txtField5, txtField6;
    private List<TextField> colorSelectTxtFields;

    @FXML
    private Label colorLabel;

    private List<Color> selectedColors;
    @FXML private ImageView colorPickerImgView;
    private Image selectedFile;
    @FXML private Rectangle averageColorPreview = new Rectangle(); //displays the average color of selection


    private LoadingModel loadingModel = new LoadingModel();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        SharedDataModel sharedDataModel = SharedDataModel.getInstance();
        selectedFile = sharedDataModel.getSelectedImage();
        colorPickerImgView.setImage(selectedFile);
        colorPickerImgView.setPreserveRatio(true);
        colorPickerImgView.setSmooth(true);

        isSelecting = false;
        isCropping = false;
        canResetCrop = false;
        firstColorSelected = false;

        selectedColorInsertIndex = 0;

        selectedColors = new ArrayList<>();
        paletteColorRectangles = new ArrayList<>();
        paletteColorRectangles.add(paletteColor0);
        paletteColorRectangles.add(paletteColor1);
        paletteColorRectangles.add(paletteColor2);
        paletteColorRectangles.add(paletteColor3);
        paletteColorRectangles.add(paletteColor4);
        paletteColorRectangles.add(paletteColor5);
        paletteColorRectangles.add(paletteColor6);
        paletteColorRectangles.add(paletteColor7);
        paletteColorRectangles.add(paletteColor8);
        paletteColorRectangles.add(paletteColor9);
        paletteColorRectangles.add(paletteColor10);
        paletteColorRectangles.add(paletteColor11);
        paletteColorRectangles.add(paletteColor12);
        paletteColorRectangles.add(paletteColor13);
        paletteColorRectangles.add(paletteColor14);

        colorSelectButtons = new ArrayList<>();
        colorSelectButtons.add(button1);
        colorSelectButtons.add(button2);
        colorSelectButtons.add(button3);
        colorSelectButtons.add(button4);
        colorSelectButtons.add(button5);
        colorSelectButtons.add(button6);

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
    }

    private static Rectangle2D resetViewport(ImageView imageView) {
        //reset the viewport to default value (view entire image)
        Rectangle2D defaultViewport = getDefaultViewport(imageView);

        imageView.setViewport(defaultViewport);

        return defaultViewport;
    }

    private static Rectangle2D getDefaultViewport(ImageView imageView){
        Image image = imageView.getImage();
        return new Rectangle2D(0, 0, image.getWidth(), image.getHeight());
    }

    @FXML
    private void handleMousePressed(MouseEvent event) {
        //TODO: IF USER SELECTS AREA OUTSIDE OF IMAGE, SHIFT SELECTION BOX INSIDE IMAGE
        if(isSelecting || isCropping) {
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
    private void handleMouseDragged(MouseEvent event) {
        if(isSelecting || isCropping) {
            double x = event.getX();
            double y = event.getY();
            double width = x - selectionRectangle.getX();
            double height = y - selectionRectangle.getY();

            selectionRectangle.setWidth(width);
            selectionRectangle.setHeight(height);
        }
    }

    @FXML
    private void handleMouseReleased(MouseEvent event) {
        if(isSelecting) {
            Color averageColor = getAvgColorFromSelection();

            // Set the color label text
            colorLabel.setText("Greyscale: " + Math.round(getGreyScaleDouble(averageColor)));

            //display average color to user
            updateAverageColorPreview(averageColor);

            firstColorSelected = true;
        }

        if(isCropping){
            canResetCrop = true;
            cropImage();
        }
    }

    public void enterCropMode() {
        //same button is used for cropping and resetting cropping
        if(canResetCrop){//button text is "Reset Crop"
            resetViewport(colorPickerImgView);
            cropButton.setText("Crop");
            canResetCrop = false;
        }
        else{//button text is "Crop"
            cropButton.setText("Cropping...");
            isCropping = true;
        }

        resetButtonsText();
        isSelecting = false;
        selectionRectangle.setVisible(false);
    }

    private void cropImage() {
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

        selectionRectangle.setVisible(false);
    }

    private void updateAverageColorPreview(Color newColor) {
        Color prevColor = (Color) averageColorPreview.getFill();
        if (!prevColor.equals(newColor))//change the stroke of the box to the previous color, if color changes
            averageColorPreview.setStroke(prevColor);

        averageColorPreview.setFill(newColor);
    }

    @FXML//used so the rectangles in color palette can update the average color preview
    public void updateAverageColorPreview(MouseEvent mouseEvent){
        Rectangle sourceRect = (Rectangle) mouseEvent.getSource();
        Color rectColor = (Color) sourceRect.getFill();
        updateAverageColorPreview(rectColor);
    }

    private Color getAvgColorFromSelection(){
        double x = selectionRectangle.getX();//(x, y) is the top left corner of the selectionRectangle in windowspace
        double y = selectionRectangle.getY();
        double width = selectionRectangle.getWidth();
        double height = selectionRectangle.getHeight();

        javafx.scene.image.PixelReader pixelReader = colorPickerImgView.getImage().getPixelReader();

        Rectangle2D viewport = colorPickerImgView.getViewport();
        if(viewport == null){
            viewport = resetViewport(colorPickerImgView);
        }

        double scaleFactor;
        if(viewport == getDefaultViewport(colorPickerImgView)){//use this scaleFactor when image is not cropped
            scaleFactor = calculateImgViewScaleFactor(colorPickerImgView);
        }
        else{
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
        for (int posX = (int) trueStartX; posX < trueEndX; posX++) {
            for (int posY = (int) trueStartY; posY < trueEndY; posY++) {
                try{
                    if(viewport.contains(posX, posY)){//only add color if it is inside the viewport (visible to user)
                        Color color = pixelReader.getColor(posX, posY);
                        selectedColors.add(color);
                    }
                    else{
                        badColorDetected = true;
                    }
                }
                catch (IndexOutOfBoundsException e){
                    badColorDetected = true;
                }
            }
        }

        if(badColorDetected){//TODO: CHANGE SOLUTION TO THIS PROBLEM?
            log.info("Some colors could not be added. Please confirm that your selection contains the desired colors.");
        }

        return calculateAverageColor(selectedColors);
    }

    private double calculateImgViewCroppedScaleFactor(ImageView imageView) {
        Rectangle2D viewport = imageView.getViewport();
        if(viewport.getWidth() > viewport.getHeight()){
            return viewport.getWidth() / imageView.getFitWidth();
        }
        else{
            return viewport.getHeight() / imageView.getFitHeight();
        }
    }

    private double calculateImgViewScaleFactor(ImageView imgView) {
        //getWidth() and getHeight() refer to the full resolution image
        //fitWidth() and fitHeight() refer to the image in the window
        if(imgView.getImage().getWidth() > imgView.getImage().getHeight()) {
            return imgView.getImage().getWidth() / imgView.getFitWidth();
        }
        else {
            return imgView.getImage().getHeight() / imgView.getFitHeight();
        }
    }

    private Color calculateAverageColor(List<Color> colors) {
        double redSum = 0;
        double greenSum = 0;
        double blueSum = 0;

        for (Color color : colors) {
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

    private String getRGBString(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return r + ", " + g + ", " + b;
    }

    private double getGreyScaleDouble(Color color){
        //new calculation uses weighted scaling
        double redVal = color.getRed();
        double greenVal = color.getGreen();
        double blueVal = color.getBlue();

        redVal = Math.pow(redVal, 2.2);
        greenVal = Math.pow(greenVal, 2.2);
        blueVal = Math.pow(blueVal, 2.2);

        redVal *= 0.2126729;
        greenVal *= 0.71522;
        blueVal *= 0.0721750;

        double weightedAverageColor = redVal + greenVal + blueVal;
        weightedAverageColor = Math.pow(weightedAverageColor, 1/2.2);
        return weightedAverageColor * 255;
    }

    //returns false if the color is null or has already been added
    public boolean addSelectedColor() {
        //add selected color to palette
        //if color is null, or has already been selected, return false
        //else, find the highest rectangle in the palette which does not have a color
        //      and change that rectangle's color to the selected color (kept track of using selectedColorInsertIndex)
        //also, update the text field (to int. greyscale value) and its corresponding color square
        Color newColor = (Color) averageColorPreview.getFill();

        if (firstColorSelected) {
            //get the rectangle for the first empty color slot in palette
            Rectangle rect = paletteColorRectangles.get(selectedColorInsertIndex);
            if (!isColorInList(newColor)) {
                rect.setFill(newColor);//only add color to palette if it is not a duplicate
            }

            Button sourceButton = resetButtonsText();
            if (sourceButton != null) {
                //modify appropriate text field to average greyscale value
                TextField partnerTxtField = getTextFieldForButton(sourceButton);

                //java would use the wrong overload of round() if it used a double
                Integer greyScale = Math.round((float)getGreyScaleDouble(newColor));
                partnerTxtField.setText(String.valueOf(greyScale));

                //without these two lines, text field would not update properly
                partnerTxtField.positionCaret(partnerTxtField.getText().length());
                partnerTxtField.positionCaret(0);

                //update square which contains the average color visual for the button
                Rectangle partnerRectangle = getRectangleForButton(sourceButton);
                updateFinalSelectRect(partnerRectangle);

                //disable/enable apply button as needed
                updateApplyButton();
            }
            else{
                Toolkit.getDefaultToolkit().beep();
                return false; //source button is null
            }

            ++selectedColorInsertIndex;
            isSelecting = false;
            return true;//color changed successfully
        }
        Toolkit.getDefaultToolkit().beep();
        return false;//no color has been selected yet
    }

    public void removeColor() {
        if(selectedColorInsertIndex > 0){
            Rectangle removeRect = null;
            //find the rectangle which needs to be removed
            //currently, program finds the rectangle which has the same color as the averageColorPreview
            //averageColorPreview takes the color of the last selected rectangle
            //TODO: SHOW HIGHLIGHTING AROUND RECTANGLE WHEN IT IS SELECTED?

            for(Rectangle rect : paletteColorRectangles){
                if(rect.getFill().equals(averageColorPreview.getFill())){
                    removeRect = rect;
                    break;
                }
            }

            if (removeRect == null){//no rectangle matches the color of the average color preview
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            //find removeRect's location in palette by getting the number at the end of its name
            String rectID = removeRect.getId();
            Pattern pattern = Pattern.compile("\\D*(\\d+)$");
            Matcher matcher = pattern.matcher(rectID);
            int startingIndex;
            if (matcher.find()) {
                String numberString = matcher.group(1);
                startingIndex = Integer.parseInt(numberString);
            }
            else{
                Toolkit.getDefaultToolkit().beep();
                return;//TODO: PROPER SOLUTION?
                //pretty sure this would only be reached if a rectangle was improperly named
            }

            //rect 3 color goes to rect 2, rect 2 color goes to rect 1, etc
            for (int i = startingIndex; i < selectedColorInsertIndex; ++i){//from removed rectangle to last rect w/ assigned color
                paletteColorRectangles.get(i).setFill(paletteColorRectangles.get(i + 1).getFill());
            }
            --selectedColorInsertIndex;
        }
    }

    private void updateFinalSelectRect(Rectangle rect) {//when a text field is updated, update the rectangle beside it
        TextField txtField = getTextFieldForRectangle(rect);

        double greyScale;
        try{
            greyScale = Integer.valueOf(txtField.getText());
        }
        catch(NumberFormatException e){
            greyScale = 0;
        }

        if(greyScale > 255){
            greyScale = 255;
        }

        if (greyScale < 0){
            greyScale = 0;
        }

        double val = greyScale / (255);
        rect.setFill(new Color(val, val, val, 1));
        rect.setVisible(true);
    }

    public void updatesFromTextField(KeyEvent event){
        //whenever a text field is updated, update its partner color rectangle and change the visibility of the Apply button
        //if all text fields contain valid info, make the Apply button functional
        //if not, make the button not functional
        TextField sourceTxtField = (TextField) event.getSource();
        updateFinalSelectRect(getRectangleForTextField(sourceTxtField));

        updateApplyButton();
    }

    private void updateApplyButton() {
        //only enable apply button if all fields contain good data (integers) and model is loaded
        if(areAllFieldsValid() && isGoodLoadingModel()){//TODO: KEEPS BUTTON DISABLED IF NEW MODEL IS LOADED IN
                                                        //only updates setDisable() when text field is modified
            applyButton.setDisable(false);
        }
        else{
            applyButton.setDisable(true);
        }
    }

    //returns the text field the button corresponds to
    private TextField getTextFieldForButton(Button sourceButton) {
        switch (sourceButton.getId()){
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

    private Rectangle getRectangleForButton(Button sourceButton) {
        switch (sourceButton.getId()){
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

    private TextField getTextFieldForRectangle(Rectangle rect) {
        switch (rect.getId()){
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

    private Rectangle getRectangleForTextField(TextField txtField) {
        switch (txtField.getId()){
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

    private boolean isColorInList(Color newColor) {
        for (Rectangle rect : paletteColorRectangles) {
            if (rect.getFill().equals(newColor))
                return true;
        }
        return false;
    }

    public void applyButtonPressed() {
        boolean isGoodInput = areAllFieldsValid();//check to see if all text fields contain valid input

        if(isGoodInput && isGoodLoadingModel()) {
            loadingModel.setTonemapping(
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
        else{
            Toolkit.getDefaultToolkit().beep();
            //TODO: PROBABLY CHANGE THIS VERIFICATION METHOD
            System.err.println("Please fill all fields and load a model before performing color calibration.");
        }
    }

    private boolean areAllFieldsValid(){
        //only apply color calibration if all text fields are filled with good info (integers)
        for (TextField field : colorSelectTxtFields){//TODO: CHECK IF VALS ARE 0-255?
            if(!field.getText().matches("-?\\d+")){//regex to check if input is integer
                return false;
            }
        }
        return this.loadingModel.hasValidHandler();
    }

    public void enterColorSelectionMode(ActionEvent actionEvent) {
        //change text of button to indicate selection
        Button sourceButton = (Button) actionEvent.getSource();
        resetButtonsText();

        sourceButton.setText("Draw to select...");

        isSelecting = true;
        isCropping = false;
    }

    private Button resetButtonsText(){
        Button sourceButton = null;
        for (Button button: colorSelectButtons){
            if (!button.getText().equals(DEFAULT_BUTTON_TEXT)) {
                sourceButton = button;
            }
            button.setText(DEFAULT_BUTTON_TEXT);
        }

        return sourceButton;
    }

    public void setLoadingModel(LoadingModel loadingModel){
        //TODO: THIS FUNCTION IS NOT CALLED IF THE COLOR CHECKER IS OPENED BEFORE THE MODEL IS LOADED
        //close color checker when new model is loaded?

        this.loadingModel = loadingModel;

        //initialize txtFields with their respective values
        if (isGoodLoadingModel()){
            DoubleUnaryOperator luminanceEncoding = loadingModel.getLuminanceEncodingFunction();
            txtField1.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.031))));
            txtField2.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.090))));
            txtField3.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.198))));
            txtField4.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.362))));
            txtField5.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.591))));
            txtField6.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.900))));

            for(Rectangle rect : finalSelectRectangles){
                rect.setVisible(true);
                updateFinalSelectRect(rect);
            }

            updateApplyButton();
        }
        else{
            //TODO: WHAT TO DO IF NO MODEL FOUND?
            System.err.println("Could not bring in luminance encodings: no model found");
        }
    }

    private boolean isGoodLoadingModel(){
        return loadingModel.hasValidHandler();
    }
}
