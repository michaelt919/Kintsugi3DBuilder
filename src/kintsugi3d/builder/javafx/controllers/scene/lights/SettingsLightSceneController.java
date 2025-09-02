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

package kintsugi3d.builder.javafx.controllers.scene.lights;//Created by alexk on 7/16/2017.

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import kintsugi3d.builder.javafx.util.SafeLogScaleNumberStringConverter;
import kintsugi3d.builder.javafx.util.SafeNumberStringConverter;
import kintsugi3d.builder.javafx.util.StaticUtilities;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsLightSceneController implements Initializable
{
    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        setDisabled(true);

        StringConverter<Double> pow10converter = new StringConverter<>() {
            @Override
            public String toString(Double object) {
                String s = logScaleNumberStringConverter.toString(object);
                if (s.length() > 4) {
                    return s.substring(0, 4);
                } else {
                    return s;
                }
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        };

        distanceSlider.setLabelFormatter(pow10converter);

        intensitySlider.setLabelFormatter(pow10converter);
        StaticUtilities.bindLogScaleToLinear(intensitySlider.valueProperty(), trueIntensity);

        StaticUtilities.makeNumeric(xCenterTextField);
        StaticUtilities.makeNumeric(yCenterTextField);
        StaticUtilities.makeNumeric(zCenterTextField);

        StaticUtilities.makeClampedNumeric(0, Double.MAX_VALUE, distanceTextField);
        StaticUtilities.makeClampedNumeric(0, Double.MAX_VALUE, intensityTextField);

        StaticUtilities.makeWrapAroundNumeric(-180, 180, azimuthTextField);
        StaticUtilities.makeClampedNumeric(-90, 90, inclinationTextField);

        StaticUtilities.makeClampedNumeric(0.0, 90.0, spotSizeTextField);
        StaticUtilities.makeClampedNumeric(0.0, 1.0, spotTaperTextField);
    }

    @FXML private VBox root;
    @FXML private TextField xCenterTextField;
    @FXML private TextField yCenterTextField;
    @FXML private TextField zCenterTextField;
    @FXML private Slider xCenterSlider;
    @FXML private Slider yCenterSlider;
    @FXML private Slider zCenterSlider;
    @FXML private TextField azimuthTextField;
    @FXML private Slider azimuthSlider;
    @FXML private TextField inclinationTextField;
    @FXML private Slider inclinationSlider;
    @FXML private TextField distanceTextField;
    @FXML private Slider distanceSlider;
    @FXML private TextField intensityTextField;
    @FXML private Slider intensitySlider;
    @FXML private ColorPicker colorPicker;
    @FXML private TextField spotSizeTextField;
    @FXML private Slider spotSizeSlider;
    @FXML private TextField spotTaperTextField;
    @FXML private Slider spotTaperSlider;

    private final DoubleProperty trueIntensity = new SimpleDoubleProperty(1);

    private final SafeNumberStringConverter numberStringConverter = new SafeNumberStringConverter(0);
    private final SafeLogScaleNumberStringConverter logScaleNumberStringConverter = new SafeLogScaleNumberStringConverter(1);

    public final ChangeListener<ObservableLightInstanceSetting> changeListener = (observable, oldValue, newValue) ->
    {
        if (oldValue != null)
        {
            unbind(oldValue);
        }

        if (newValue != null)
        {
            bind(newValue);
            setDisabled(newValue.locked().get() || newValue.isGroupLocked());
        }
        else
        {
            setDisabled(true);
        }
    };

    public void setDisabled(Boolean disabled)
    {
        root.setDisable(disabled);
    }

    private void bind(ObservableLightInstanceSetting setting)
    {
        xCenterTextField.textProperty().bindBidirectional(setting.targetX(), numberStringConverter);
        yCenterTextField.textProperty().bindBidirectional(setting.targetY(), numberStringConverter);
        zCenterTextField.textProperty().bindBidirectional(setting.targetZ(), numberStringConverter);
        azimuthTextField.textProperty().bindBidirectional(setting.azimuth(), numberStringConverter);
        inclinationTextField.textProperty().bindBidirectional(setting.inclination(), numberStringConverter);
        distanceTextField.textProperty().bindBidirectional(setting.log10Distance(), logScaleNumberStringConverter);
        intensityTextField.textProperty().bindBidirectional(setting.intensity(), numberStringConverter);
        spotSizeTextField.textProperty().bindBidirectional(setting.spotSize(), numberStringConverter);
        spotTaperTextField.textProperty().bindBidirectional(setting.spotTaper(), numberStringConverter);
        xCenterSlider.valueProperty().bindBidirectional(setting.targetX());
        yCenterSlider.valueProperty().bindBidirectional(setting.targetY());
        zCenterSlider.valueProperty().bindBidirectional(setting.targetZ());
        azimuthSlider.valueProperty().bindBidirectional(setting.azimuth());
        inclinationSlider.valueProperty().bindBidirectional(setting.inclination());
        distanceSlider.valueProperty().bindBidirectional(setting.log10Distance());
        spotSizeSlider.valueProperty().bindBidirectional(setting.spotSize());
        spotTaperSlider.valueProperty().bindBidirectional(setting.spotTaper());
        trueIntensity.bindBidirectional(setting.intensity());
        colorPicker.valueProperty().bindBidirectional(setting.color());
    }

    private void unbind(ObservableLightInstanceSetting setting)
    {
        xCenterTextField.textProperty().unbindBidirectional(setting.targetX());
        yCenterTextField.textProperty().unbindBidirectional(setting.targetY());
        zCenterTextField.textProperty().unbindBidirectional(setting.targetZ());
        azimuthTextField.textProperty().unbindBidirectional(setting.azimuth());
        inclinationTextField.textProperty().unbindBidirectional(setting.inclination());
        distanceTextField.textProperty().unbindBidirectional(setting.log10Distance());
        intensityTextField.textProperty().unbindBidirectional(setting.intensity());
        spotSizeTextField.textProperty().unbindBidirectional(setting.spotSize());
        spotTaperTextField.textProperty().unbindBidirectional(setting.spotTaper());
        xCenterSlider.valueProperty().unbindBidirectional(setting.targetX());
        yCenterSlider.valueProperty().unbindBidirectional(setting.targetY());
        zCenterSlider.valueProperty().unbindBidirectional(setting.targetZ());
        azimuthSlider.valueProperty().unbindBidirectional(setting.azimuth());
        inclinationSlider.valueProperty().unbindBidirectional(setting.inclination());
        distanceSlider.valueProperty().unbindBidirectional(setting.log10Distance());
        spotSizeSlider.valueProperty().unbindBidirectional(setting.spotSize());
        spotTaperSlider.valueProperty().unbindBidirectional(setting.spotTaper());
        trueIntensity.unbindBidirectional(setting.intensity());
        colorPicker.valueProperty().unbindBidirectional(setting.color());
    }
}
