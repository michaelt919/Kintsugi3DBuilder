package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.beans.InvalidationListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class EyedropperController implements Initializable {

    @FXML
    private Pane rootPane;
    @FXML
    private Rectangle selectionRectangle;

    @FXML
    private Rectangle selectedColor0, selectedColor1, selectedColor2, selectedColor3, selectedColor4, selectedColor5, selectedColor6,
            selectedColor7, selectedColor8, selectedColor9, selectedColor10, selectedColor11, selectedColor12, selectedColor13, selectedColor14;
    private List<Rectangle> selectedColorRectangles;

    @FXML
    private Rectangle finalSelectRect1, finalSelectRect2, finalSelectRect3, finalSelectRect4, finalSelectRect5, finalSelectRect6;
    private List<Rectangle> finalSelectRectangles;

    @FXML Button button1, button2, button3, button4, button5, button6;
    private List<Button> colorSelectButtons;
    final String defaultButtonText = "Select Color";

    @FXML
    private TextField txtField1, txtField2, txtField3, txtField4, txtField5, txtField6;
    private List<TextField> colorSelectTxtFields;

    @FXML
    private Label colorLabel;

    private List<Color> selectedColors;
    @FXML private ImageView colorPickerImgView;
    private File selectedFile;
    @FXML private Rectangle averageColorDisplay = new Rectangle(); //displays the average color of selection

    private boolean selectionAllowed;//enabled by "Select Color" buttons and disabled when selection is finished


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //TODO: REMOVE HARD CODING OF THIS IMAGE
        selectedFile = new File("C:\\Users\\DenneyLuke\\Downloads\\colorGrid2.png");
        colorPickerImgView.setImage(new Image(selectedFile.toURI().toString()));
        selectionAllowed = false;

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

    @FXML
    private void handleMousePressed(MouseEvent event) {//TODO: IF USER SELECTS AREA OUTSIDE OF IMAGE, SHIFT SELECTION BOX INSIDE IMAGE
        if(selectionAllowed) {
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
        if(selectionAllowed) {
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
        if(selectionAllowed) {
            Color averageColor = screenshotAndFindAvgColor();

            // Set the color label text
            colorLabel.setText("RGB: " + getRGBString(averageColor)
            + "\tGreyScale: " + Math.round(getGreyScaleDouble(averageColor)));//TODO: ADD PRECISION TO GREYSCALE ROUNDING?

            //display average color to user
            Color prevColor = (Color) averageColorDisplay.getFill();
            if (!prevColor.equals(averageColor))//change the stroke of the box to the previous color, if color changes
                averageColorDisplay.setStroke(prevColor);

            averageColorDisplay.setFill(averageColor);
        }
    }

    private Color screenshotAndFindAvgColor(){
        double x = selectionRectangle.getX();
        double y = selectionRectangle.getY();
        double width = selectionRectangle.getWidth();
        double height = selectionRectangle.getHeight();

        javafx.scene.image.PixelReader pixelReader = colorPickerImgView.getImage().getPixelReader();


        double scaleFactor;

        if(colorPickerImgView.getImage().getWidth() > colorPickerImgView.getImage().getHeight()) {
            scaleFactor = colorPickerImgView.getImage().getWidth() / colorPickerImgView.getFitWidth();
        }
        else {
            scaleFactor = colorPickerImgView.getImage().getHeight() / colorPickerImgView.getFitHeight();
        }


        double trueStartX = (x - colorPickerImgView.getLayoutX()) * scaleFactor;
        double trueStartY = (y - colorPickerImgView.getLayoutY()) * scaleFactor;

        double trueEndX = trueStartX + width * scaleFactor;
        double trueEndY = trueStartY + height * scaleFactor;

        //read pixels from selected crop
        selectedColors.clear();
        for (int posX = (int) trueStartX; posX < trueEndX; posX++) {
            for (int posY = (int) trueStartY; posY < trueEndY; posY++) {
                Color color = pixelReader.getColor(posX, posY);
                selectedColors.add(color);
            }
        }

        //print selectedColors
         //print all selected colors
//        System.out.println("---------------------------------------------------------------------");
//        for(Color color: selectedColors)
//            System.out.println(getRGBString(color));
//        System.out.println("---------------------------------------------------------------------");

        return calculateAverageColor(selectedColors);
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
        double averageRed = redSum / (double) size;
        double averageGreen = greenSum / (double) size;
        double averageBlue = blueSum / (double) size;

        return Color.color(averageRed, averageGreen, averageBlue);
    }

    private String getRGBString(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return r + ", " + g + ", " + b;
    }

    private double getGreyScaleDouble(Color color){
        double redVal = color.getRed() / 3;
        double greenVal = color.getGreen() / 3;
        double blueVal = color.getBlue() / 3;

        return (redVal + greenVal + blueVal) * 255;
    }

    //returns false if the color is null or has already been added
    public boolean addColor(ActionEvent actionEvent) {
        //get color of selected color
        //if color is null or the same or has already been selected, return false
        //else, find the highest rectangle which does not have a color and change that rectangle's color to the selected color
        Color newColor = (Color) averageColorDisplay.getFill();

        if(newColor.equals(Color.WHITE)) {
            return false;
        }
        else{
            for(Rectangle rect : selectedColorRectangles){
                if (rect.getFill().equals(Color.WHITE)) {
                    if (!colorInList(newColor)){
                        rect.setFill(newColor);//only add color to palette if it is not a duplicate. Still update everything else
                    }
                    Button sourceButton = resetButtonsText();

                    //modify appropriate text field to average greyscale value
                    TextField partnerTxtField = getButtonPartnerTxtField(sourceButton);
                    Double greyScale = getGreyScaleDouble(newColor);//TODO: USE BETTER (weighted) GREYSCALE CONVERSION
                    partnerTxtField.setText(String.valueOf(greyScale));

                    partnerTxtField.positionCaret(partnerTxtField.getText().length());//without these two lines, text field would not update properly
                    partnerTxtField.positionCaret(0);

                    Rectangle partnerRectangle = getButtonPartnerRect(sourceButton);
                    updateFinalSelectRect(partnerRectangle);

                    selectionAllowed = false;
                    selectionRectangle.setVisible(false);
                    return true;
                }
            }
            return false;
        }
    }

    private void updateFinalSelectRect(Rectangle rect) {
        TextField txtField = getRectPartnerTxtField(rect);

        double greyScale;
        try{
            greyScale = Double.valueOf(txtField.getText());
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

    public void applyButtonPressed(ActionEvent actionEvent) {
        //TODO: NEED TO BRING OVER CODE FROM THE OLD COLOR CHECKER
    }

    public void triggerSelection(ActionEvent actionEvent) {
        //change text of button to indicate selection
        Button sourceButton = (Button) actionEvent.getSource();
        resetButtonsText();

        sourceButton.setText("Draw to select...");

        selectionAllowed = true;
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
}
