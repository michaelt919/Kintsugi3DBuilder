package tetzlaff.ibrelight.javafx.controllers.menubar;

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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import tetzlaff.ibrelight.core.LoadingModel;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.DoubleUnaryOperator;

public class EyedropperController implements Initializable {

    @FXML
    private Pane colorPickerPane;
    @FXML
    private AnchorPane rootPane;
    @FXML
    private Rectangle selectionRectangle;

    @FXML
    private Rectangle selectedColor0, selectedColor1, selectedColor2, selectedColor3, selectedColor4, selectedColor5, selectedColor6,
            selectedColor7, selectedColor8, selectedColor9, selectedColor10, selectedColor11, selectedColor12, selectedColor13, selectedColor14;
    private List<Rectangle> selectedColorRectangles;

    @FXML
    private Rectangle finalSelectRect1, finalSelectRect2, finalSelectRect3, finalSelectRect4, finalSelectRect5, finalSelectRect6;
    private List<Rectangle> finalSelectRectangles;

    @FXML
    private Button button1, button2, button3, button4, button5, button6;
    private List<Button> colorSelectButtons;

    @FXML
    private Button cropButton;
    private boolean isCropping;//enabled by crop button and disabled when cropping is finished
    private boolean isSelecting;//enabled by "Select Color" buttons and disabled when selection is finished
    private boolean canResetCrop; //enabled when cropping is finished and disabled when crop is reset to default viewport
    private boolean firstColorSelected;

    static final String defaultButtonText = "Select Color";

    @FXML
    private TextField txtField1, txtField2, txtField3, txtField4, txtField5, txtField6;
    private List<TextField> colorSelectTxtFields;

    @FXML
    private Label colorLabel;

    private List<Color> selectedColors;
    @FXML private ImageView colorPickerImgView;
    private File selectedFile;
    @FXML private Rectangle averageColorDisplay = new Rectangle(); //displays the average color of selection


    private LoadingModel loadingModel = new LoadingModel();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        SharedDataModel sharedDataModel = SharedDataModel.getInstance();
        selectedFile = sharedDataModel.getSelectedImage();
        colorPickerImgView.setImage(new Image(selectedFile.toURI().toString()));
        colorPickerImgView.setPreserveRatio(true);
        colorPickerImgView.setSmooth(true);

        isSelecting = false;
        isCropping = false;
        canResetCrop = false;
        firstColorSelected = false;

        selectedColors = new ArrayList<>();
        selectedColorRectangles = new ArrayList<>();
        selectedColorRectangles.add(selectedColor0);
        selectedColorRectangles.add(selectedColor1);
        selectedColorRectangles.add(selectedColor2);
        selectedColorRectangles.add(selectedColor3);
        selectedColorRectangles.add(selectedColor4);
        selectedColorRectangles.add(selectedColor5);
        selectedColorRectangles.add(selectedColor6);
        selectedColorRectangles.add(selectedColor7);
        selectedColorRectangles.add(selectedColor8);
        selectedColorRectangles.add(selectedColor9);
        selectedColorRectangles.add(selectedColor10);
        selectedColorRectangles.add(selectedColor11);
        selectedColorRectangles.add(selectedColor12);
        selectedColorRectangles.add(selectedColor13);
        selectedColorRectangles.add(selectedColor14);

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
    }

    private Rectangle2D resetViewport(ImageView imageView) {
        //reset the viewport to default value (view entire image)
        Rectangle2D defaultViewport = getDefaultViewport(imageView);

        imageView.setViewport(defaultViewport);

        return defaultViewport;
    }

    private Rectangle2D getDefaultViewport(ImageView imageView){
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

            //attempt at detecting when the mouse has left the imageView
//            Point2D selectionRectangleRelativeCoords = colorPickerImgView.localToScene(new Point2D
//                    (colorPickerImgView.getX() + colorPickerImgView.getImage().getRequestedWidth(),
//                            colorPickerImgView.getY() + colorPickerImgView.getImage().getRequestedHeight()));
//
//            double relativeX = selectionRectangleRelativeCoords.getX();
//            double relativeY = selectionRectangleRelativeCoords.getY();
//
//            System.out.println("x is " + x + ", relativeX is " + relativeX);
//            System.out.println("y is " + y + ", relativeY is " + relativeY);
//
//            if (x > relativeX) {
//                System.out.println("X TOO BIG");
//            }
//            if (y > relativeY) {
//                System.out.println("Y TOO BIG");
//            }

            selectionRectangle.setWidth(width);
            selectionRectangle.setHeight(height);
        }
    }

    @FXML
    private void handleMouseReleased(MouseEvent event) {
        if(isSelecting) {
            Color averageColor = screenshotAndFindAvgColor();

            // Set the color label text
            colorLabel.setText("Greyscale: " + Math.round(getGreyScaleDouble(averageColor)));

            //display average color to user
            updateAverageColorDisplay(averageColor);

            firstColorSelected = true;
        }

        if(isCropping){
            canResetCrop = true;
            cropImage(event);
        }
    }

    public void triggerCropping(ActionEvent actionEvent) {
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

    private void cropImage(MouseEvent event) {//might be a better way to access this information
        //this function receives the event from MOUSE RELEASED, so x and y are the bottom right corner, not the top left
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

    private void updateAverageColorDisplay(Color newColor) {
        Color prevColor = (Color) averageColorDisplay.getFill();
        if (!prevColor.equals(newColor))//change the stroke of the box to the previous color, if color changes
            averageColorDisplay.setStroke(prevColor);
        averageColorDisplay.setFill(newColor);
    }

    @FXML//used so the rectangles in color palette can update the average color display
    public void updateAverageColorDisplay(MouseEvent mouseEvent){
        Rectangle sourceRect = (Rectangle) mouseEvent.getSource();
        Color rectColor = (Color) sourceRect.getFill();
        updateAverageColorDisplay(rectColor);
    }

    private Color screenshotAndFindAvgColor(){
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
            System.out.println("Some colors could not be added. Please confirm that your selection contains the desired colors.");
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
    public boolean addSelectedColor(ActionEvent actionEvent) {
        //add selected color to palette
        //if color is null, or has already been selected, return false
        //else, find the highest rectangle in the palette which does not have a color
        //      and change that rectangle's color to the selected color
        //also, update the text field (to int. greyscale value) and its corresponding color square
        Color newColor = (Color) averageColorDisplay.getFill();

        if (firstColorSelected) {
            for (Rectangle rect : selectedColorRectangles) {//add color to color palette in appropriate spot
                if (rect.getFill().equals(Color.WHITE)) {
                    if (!colorInList(newColor)) {
                        rect.setFill(newColor);//only add color to palette if it is not a duplicate
                    }
                    Button sourceButton = resetButtonsText();
                    if (sourceButton != null) {
                        //modify appropriate text field to average greyscale value
                        TextField partnerTxtField = getButtonPartnerTxtField(sourceButton);

                        //java would use the wrong overload of round() if it used a double
                        Integer greyScale = Math.round((float)getGreyScaleDouble(newColor));
                        partnerTxtField.setText(String.valueOf(greyScale));

                        //without these two lines, text field would not update properly
                        partnerTxtField.positionCaret(partnerTxtField.getText().length());
                        partnerTxtField.positionCaret(0);

                        //update square which contains the average color visual for the button
                        Rectangle partnerRectangle = getButtonPartnerRect(sourceButton);
                        updateFinalSelectRect(partnerRectangle);
                    }
                    else{
                        Toolkit.getDefaultToolkit().beep();
                        return false; //source button is null
                    }

                    isSelecting = false;
                    return true;//color changed successfully
                }
            }
        }
        Toolkit.getDefaultToolkit().beep();
        return false;//no color has been selected yet
    }

    private void updateFinalSelectRect(Rectangle rect) {
        TextField txtField = getRectPartnerTxtField(rect);

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

        rect.setFill(numToGreyScaleColor(greyScale));
        rect.setVisible(true);
    }

    public void updateFinalSelectRect(KeyEvent event){
        TextField sourceTxtField = (TextField) event.getSource();
        updateFinalSelectRect(getTxtFieldPartnerRect(sourceTxtField));
    }

    private Color numToGreyScaleColor(Double greyScale) {
        double val = greyScale / (255);
        return new Color(val, val, val, 1);
    }

    //returns the text field the button corresponds to
    private TextField getButtonPartnerTxtField(Button sourceButton) {
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

    private Rectangle getButtonPartnerRect(Button sourceButton) {
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

    private TextField getRectPartnerTxtField(Rectangle rect) {
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

    private Rectangle getTxtFieldPartnerRect(TextField txtField) {
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

    private boolean colorInList(Color newColor) {
        for (Rectangle rect : selectedColorRectangles) {
            if (rect.getFill().equals(newColor))
                return true;
        }
        return false;
    }

    public void applyButtonPressed()
    {
        //only apply color calibration if all text fields are filled with good info (integers)
        boolean allFieldsFull = true;
        for (TextField field : colorSelectTxtFields){//TODO: CHECK IF VALS ARE 0-255?
            if(!field.getText().matches("-?\\d+")){//regex to check if input is integer
                allFieldsFull = false;
                break;
            }
        }

        if(allFieldsFull && goodLoadingModel()) {
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

    public void triggerSelection(ActionEvent actionEvent) {
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
            if (!button.getText().equals(defaultButtonText)) {
                sourceButton = button;
            }
            button.setText(defaultButtonText);
        }

        return sourceButton;
    }

    public void setLoadingModel(LoadingModel loadingModel){
        //TODO: THIS FUNCTION IS NOT CALLED IF THE COLOR CHECKER IS OPENED BEFORE THE MODEL IS LOADED
        //close color checker when new model is loaded?

        this.loadingModel = loadingModel;

        //initialize txtFields with their respective values
        if (goodLoadingModel()){
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
        }
        else{
            //TODO: WHAT TO DO IF NO MODEL FOUND?
            System.err.println("Could not bring in luminance encodings: no model found");
        }
    }

    private boolean goodLoadingModel(){//treat this function as a replacement for (loadingModel == null) b/c that statement doesn't work
//        //TODO: REMOVE JANKY WORKAROUND
//        try{
//            DoubleUnaryOperator luminanceEncoding = loadingModel.getLuminanceEncodingFunction();
//        }
//        catch(Exception e){
//            return false;
//        }
//
//        return true;

        return loadingModel.hasValidHandler();
    }
}