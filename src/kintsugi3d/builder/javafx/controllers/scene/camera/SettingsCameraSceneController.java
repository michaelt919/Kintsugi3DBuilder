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

package kintsugi3d.builder.javafx.controllers.scene.camera;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import kintsugi3d.builder.javafx.util.SafeLogScaleNumberStringConverter;
import kintsugi3d.builder.javafx.util.SafeNumberStringConverter;
import kintsugi3d.builder.javafx.util.StaticUtilities;

public class SettingsCameraSceneController implements Initializable
{

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
    @FXML private TextField twistTextField;
    @FXML private Slider twistSlider;
    @FXML private TextField fovTextField;
    @FXML private Slider fovSlider;
    @FXML private TextField focalLengthTextField;
    @FXML private Slider focalLengthSlider;

    @FXML private CheckBox orthographicCheckBox;

    private final DoubleProperty fov = new SimpleDoubleProperty();
    private final DoubleProperty focalLength = new SimpleDoubleProperty();

    private final SafeNumberStringConverter n = new SafeNumberStringConverter(0);
    private final SafeLogScaleNumberStringConverter n10 = new SafeLogScaleNumberStringConverter(1);

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        StaticUtilities.makeWrapAroundNumeric(-180, 180, azimuthTextField);
        StaticUtilities.makeClampedNumeric(-90, 90, inclinationTextField);
        StaticUtilities.makeWrapAroundNumeric(-180, 180, twistTextField);

        StaticUtilities.makeNumeric(xCenterTextField);
        StaticUtilities.makeNumeric(yCenterTextField);
        StaticUtilities.makeNumeric(zCenterTextField);

        StaticUtilities.makeNumeric(fovTextField);
        StaticUtilities.makeNumeric(focalLengthTextField);

        fov.addListener(change -> focalLength.setValue(18 / Math.tan(fov.getValue() * Math.PI / 360 /* convert and divide by 2 */)));
        focalLength.addListener(change -> fov.setValue(360 / Math.PI /* convert and multiply by 2) */ * Math.atan(18 / focalLength.getValue())));

        distanceSlider.setLabelFormatter(new StringConverter<Double>()
        {
            @Override
            public String toString(Double object)
            {
                String out = n10.toString(object);
                if (out.length() >= 4)
                {
                    return out.substring(0, 4);
                }
                else
                {
                    return out;
                }
            }

            @Override
            public Double fromString(String string)
            {
                throw new UnsupportedOperationException();
            }
        });
    }

    public final ChangeListener<CameraSetting> changeListener =
        (observable, oldValue, newValue) ->
        {
            if (oldValue != null)
            {
                unbind(oldValue);
            }

            if (newValue != null)
            {
                bind(newValue);
                setDisabled(newValue.isLocked());
            }
            if (newValue == null)
            {
                setDisabled(true);
            }
        };

    public void setDisabled(Boolean disabled)
    {
        root.setDisable(disabled);
    }

    private void bind(CameraSetting camera)
    {

        xCenterTextField.textProperty().bindBidirectional(camera.xCenterProperty(), n);
        yCenterTextField.textProperty().bindBidirectional(camera.yCenterProperty(), n);
        zCenterTextField.textProperty().bindBidirectional(camera.zCenterProperty(), n);
        azimuthTextField.textProperty().bindBidirectional(camera.azimuthProperty(), n);
        inclinationTextField.textProperty().bindBidirectional(camera.inclinationProperty(), n);
        distanceTextField.textProperty().bindBidirectional(camera.log10DistanceProperty(), n10);
        twistTextField.textProperty().bindBidirectional(camera.twistProperty(), n);
        fovTextField.textProperty().bindBidirectional(camera.fovProperty(), n);
        focalLengthTextField.textProperty().bindBidirectional(camera.focalLengthProperty(), n);

        xCenterSlider.valueProperty().bindBidirectional(camera.xCenterProperty());
        yCenterSlider.valueProperty().bindBidirectional(camera.yCenterProperty());
        zCenterSlider.valueProperty().bindBidirectional(camera.zCenterProperty());
        azimuthSlider.valueProperty().bindBidirectional(camera.azimuthProperty());
        inclinationSlider.valueProperty().bindBidirectional(camera.inclinationProperty());
        distanceSlider.valueProperty().bindBidirectional(camera.log10DistanceProperty());
        twistSlider.valueProperty().bindBidirectional(camera.twistProperty());
        fovSlider.valueProperty().bindBidirectional(camera.fovProperty());
        focalLengthSlider.valueProperty().bindBidirectional(camera.focalLengthProperty());

        orthographicCheckBox.selectedProperty().bindBidirectional(camera.orthographicProperty());

        fov.bindBidirectional(camera.fovProperty());
        focalLength.bindBidirectional(camera.focalLengthProperty());
    }

    private void unbind(CameraSetting camera)
    {

        xCenterTextField.textProperty().unbindBidirectional(camera.xCenterProperty());
        yCenterTextField.textProperty().unbindBidirectional(camera.yCenterProperty());
        zCenterTextField.textProperty().unbindBidirectional(camera.zCenterProperty());
        azimuthTextField.textProperty().unbindBidirectional(camera.azimuthProperty());
        inclinationTextField.textProperty().unbindBidirectional(camera.inclinationProperty());
        distanceTextField.textProperty().unbindBidirectional(camera.log10DistanceProperty());
        twistTextField.textProperty().unbindBidirectional(camera.twistProperty());
        fovTextField.textProperty().unbindBidirectional(camera.fovProperty());
        focalLengthTextField.textProperty().unbindBidirectional(camera.focalLengthProperty());

        xCenterSlider.valueProperty().unbindBidirectional(camera.xCenterProperty());
        yCenterSlider.valueProperty().unbindBidirectional(camera.yCenterProperty());
        zCenterSlider.valueProperty().unbindBidirectional(camera.zCenterProperty());
        azimuthSlider.valueProperty().unbindBidirectional(camera.azimuthProperty());
        inclinationSlider.valueProperty().unbindBidirectional(camera.inclinationProperty());
        distanceSlider.valueProperty().unbindBidirectional(camera.log10DistanceProperty());
        twistSlider.valueProperty().unbindBidirectional(camera.twistProperty());
        fovSlider.valueProperty().unbindBidirectional(camera.fovProperty());
        focalLengthSlider.valueProperty().unbindBidirectional(camera.focalLengthProperty());

        orthographicCheckBox.selectedProperty().unbindBidirectional(camera.orthographicProperty());

        fov.unbindBidirectional(camera.fovProperty());
        focalLength.unbindBidirectional(camera.focalLengthProperty());
    }
}
