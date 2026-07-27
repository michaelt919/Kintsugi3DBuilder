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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.File;

public class TextureLayersController
{
    @FXML private AnchorPane layersBox;

    @FXML private Region separator1;
    @FXML private Region separator2;

    @FXML private HBox parentHBox;
    @FXML private HBox containerHBox1;
    @FXML private HBox containerHBox2;
    @FXML private HBox containerHBox3;
    @FXML private HBox layerBlendBox;
    @FXML private HBox sizeRegion;
    @FXML private HBox sizeLabelRegion;
    @FXML private HBox featherRegion;
    @FXML private HBox featherLabelRegion;
    @FXML private HBox opacityRegion;
    @FXML private HBox opacityLabelRegion;

    @FXML private VBox sliderBox;

    @FXML private Button undoButton;
    @FXML private Button redoButton;
    @FXML private Button swapButton;
    @FXML private Button eraseButton;
    @FXML private Button fillButton;

    @FXML private Slider sizeSlider;
    @FXML private Slider featherSlider;
    @FXML private Slider opacitySlider;

    @FXML private Label sizeValue;
    @FXML private Label featherValue;
    @FXML private Label opacityValue;

    @FXML private ImageView imagePreview;

    public void initialize()
    {
        widthBindHelper(containerHBox1, parentHBox,.25);
        widthBindHelper(containerHBox2, parentHBox,.34);
        widthBindHelper(containerHBox3, parentHBox,.41);
        widthBindHelper(layerBlendBox, containerHBox3,.8);

        widthBindHelper(sizeSlider, sizeRegion, .45);
        widthBindHelper(sizeLabelRegion, sizeRegion, .15);
        widthBindHelper(featherSlider, featherRegion, .45);
        widthBindHelper(featherLabelRegion, featherRegion, .15);
        widthBindHelper(opacitySlider, opacityRegion, .45);
        widthBindHelper(opacityLabelRegion, opacityRegion, .15);

        createSliderListener(sizeSlider, sizeValue);
        createSliderListener(featherSlider, featherValue);
        createSliderPercentListener(opacitySlider, opacityValue);

        Platform.runLater(()->{
            double imageDimensions = sliderBox.getHeight();
            imagePreview.setFitHeight(imageDimensions);
            imagePreview.setFitWidth(imageDimensions);
            File file = new File("C:\\Users\\schmuckij3299\\OneDrive - University of Wisconsin-Stout\\Kintsugi3D_Model\\Statue.k3d.files\\File_Not_Found.png");
            Image image = new Image(file.toURI().toString());
            imagePreview.setImage(image);
        });
    }

    public void setShown(boolean shown)
    {
        layersBox.setManaged(shown);
        layersBox.setVisible(shown);
    }

    public void widthBindHelper(Region area1, Region area2, double amount)
    {
        area1.prefWidthProperty().bind(area2.widthProperty().multiply(amount));
    }

    public void createSliderPercentListener(Slider slider, Label label)
    {
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            label.setText(String.format("%.0f", newValue.doubleValue()) + '%');
        });
    }

    public void createSliderListener(Slider slider, Label label)
    {
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            label.setText(String.format("%.0f", newValue.doubleValue()));
        });
    }
}
