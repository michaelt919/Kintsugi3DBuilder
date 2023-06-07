package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.scene.shape.Rectangle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;
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
    private Label colorLabel;

    private List<Color> selectedColors;
    @FXML private ImageView colorPickerImgView;
    @FXML private TextField colorPickerSetField;
    private File selectedFile;
    @FXML private Rectangle averageColorDisplay = new Rectangle(); //displays the average color of selection


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //TODO: REMOVE HARD CODING OF THIS IMAGE
        selectedFile = new File("C:\\Users\\DenneyLuke\\Downloads\\colorGrid2.png");
        colorPickerImgView.setImage(new Image(selectedFile.toURI().toString()));

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
    }

    @FXML
    private void handleMousePressed(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        selectionRectangle.setX(x);
        selectionRectangle.setY(y);
        selectionRectangle.setWidth(0);
        selectionRectangle.setHeight(0);
        selectedColors.clear();
    }

    @FXML
    private void handleMouseDragged(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        double width = x - selectionRectangle.getX();
        double height = y - selectionRectangle.getY();
        selectionRectangle.setWidth(width);
        selectionRectangle.setHeight(height);
    }

    @FXML
    private void handleMouseReleased(MouseEvent event) {
        Color averageColor = screenshotAndFindAvgColor();

        // Set the color label text
        colorLabel.setText("RGB: " + getRGBString(averageColor));

        //display average color to user
        Color prevColor = (Color) averageColorDisplay.getFill();
        if(!prevColor.equals(averageColor))//change the stroke of the box to the previous color, if color changes
            averageColorDisplay.setStroke(prevColor);

        averageColorDisplay.setFill(averageColor);
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

//        double trueEndX = colorPickerImgView.getX() / (colorPickerImgView.getFitWidth() * (int) colorPickerImgView.getImage().getWidth());
//        double trueEndY = colorPickerImgView.getY() / (colorPickerImgView.getFitHeight() * (int) colorPickerImgView.getImage().getHeight());

        double trueEndX = trueStartX + width * scaleFactor;
        double trueEndY = trueStartY + height * scaleFactor;

        System.out.println("X: " + trueStartX + " and " + trueEndX);
        System.out.println("Y: " + trueStartY + " and " + trueEndY);

        //read pixels from selected crop
        selectedColors.clear();
        for (int posX = (int) trueStartX; posX < trueEndX; posX++) {
            for (int posY = (int) trueStartY; posY < trueEndY; posY++) {
                Color color = pixelReader.getColor(posX, posY);
                selectedColors.add(color);
            }
        }

//        int tempMinX = 500;
//        int tempMinY = 10;
//        int tempMaxX = 600;
//        int tempMaxY = 100;
//        for (int posX = tempMinX; posX < tempMaxX; posX++) {
//            for (int posY = tempMinY; posY < tempMaxY + height; posY++) {
//                //Color color = pixelReader.getColor(posX - (int) x, posY - (int) y);
//                Color color = pixelReader.getColor(posX, posY);
//                selectedColors.add(color);
//            }
//        }

//          original color collector
//        for (int posX = (int) x; posX < x + width; posX++) {
//            for (int posY = (int) y; posY < y + height; posY++) {
//                Color color = pixelReader.getColor(posX - (int) x, posY - (int) y);
//                selectedColors.add(color);
//            }
//        }

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
            int i = 1;
            for(Rectangle rect : selectedColorRectangles){
                if (rect.getFill().equals(Color.WHITE) && !colorInList(newColor)) {//only add color if it is not a duplicate
                    rect.setFill(newColor);
                    return true;
                }
                ++i;
            }
            return false;
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
}

