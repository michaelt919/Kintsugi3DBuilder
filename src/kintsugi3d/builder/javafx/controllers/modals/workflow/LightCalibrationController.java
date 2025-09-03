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

package kintsugi3d.builder.javafx.controllers.modals.workflow;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.controllers.paged.NonDataPageControllerBase;
import kintsugi3d.builder.javafx.internal.ObservableGlobalSettingsModel;
import kintsugi3d.builder.javafx.util.SafeDecimalNumberStringConverter;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;

public class LightCalibrationController extends NonDataPageControllerBase
{
    @FXML private Pane root;
    @FXML private TextField xTextField;
    @FXML private TextField yTextField;
    @FXML private Slider xSlider;
    @FXML private Slider ySlider;

    private ChangeListener<Vector2> settingsListener;

    private ObservableGlobalSettingsModel settingsModel;

    @Override
    public Region getRootNode()
    {
        return root;
    }

    @Override
    public void initPage()
    {
        StaticUtilities.makeNumeric(xTextField);
        StaticUtilities.makeNumeric(yTextField);

        settingsModel = getState().getSettingsModel();

        setCanAdvance(true);
        setCanConfirm(true);
    }

    @Override
    public void refresh()
    {
        // Bind controller to settings model to synchronize with "currentLightCalibration".
        bind();

        if (Global.state().getIOModel().hasValidHandler())
        {
            // Set the "currentLightCalibration" to the existing calibration values in the view set.
            ViewSet loadedViewSet = Global.state().getIOModel().getLoadedViewSet();

            settingsModel.set("currentLightCalibration",
                loadedViewSet.getLightPosition(loadedViewSet.getLightIndex(0)).getXY());
        }

        // Enables light calibration mode when the window is opened.
        settingsModel.set("lightCalibrationMode", true);

//        // Force square aspect ratio to be more efficient with screen space.
//        Window window = root.getScene().getWindow();
//        window.setWidth(window.getHeight());
//        window.widthProperty().addListener(obs -> window.setWidth(window.getHeight()));
    }

    @Override
    public boolean cancel()
    {
        settingsModel.set("lightCalibrationMode", false);
        unbind();
        return true;
    }

    public boolean confirm()
    {
        Global.state().getIOModel().applyLightCalibration();
        settingsModel.set("lightCalibrationMode", false);
        unbind();
        return true;
    }

    public void bind()
    {
        // Bind sliders to settings model
        ChangeListener<? super Number> xListener = (observable, oldValue, newValue) ->
            settingsModel.set("currentLightCalibration",
                new Vector2(newValue.floatValue(), settingsModel.get("currentLightCalibration", Vector2.class).y));
        xSlider.valueProperty().addListener(xListener);

        ChangeListener<? super Number> yListener = (observable, oldValue, newValue) ->
            settingsModel.set("currentLightCalibration",
                new Vector2(settingsModel.get("currentLightCalibration", Vector2.class).x, newValue.floatValue()));
        ySlider.valueProperty().addListener(yListener);

        settingsListener = (observable, oldValue, newValue) ->
        {
            xSlider.setValue(newValue.x);
            ySlider.setValue(newValue.y);
        };

        settingsModel.getObjectProperty("currentLightCalibration", Vector2.class).addListener(settingsListener);

        Vector3 bounds = getState().getProjectModel().getModelSize();
        double origScale = Math.max(bounds.x, Math.max(bounds.y, bounds.z));
        double finalScale = roundToLeadingDecimal(0.5 * origScale);

        SafeDecimalNumberStringConverter converter = new SafeDecimalNumberStringConverter(0.0f);

        // Bind text fields to sliders and ensure that the range adapts to values entered in text fields.
        xTextField.textProperty().addListener((observable, oldValue, newValue) ->
        {
            double x = converter.fromString(newValue);
            if (x > xSlider.getMax())
            {
                // Adjust bounds first
                double newMax = roundToLeadingDecimal(x * 2.0);
                xSlider.setMin(-newMax);
                xSlider.setMax(newMax);
            }
            else if (x < xSlider.getMin())
            {
                // Adjust bounds first
                double newMax = roundToLeadingDecimal(-x * 2.0);
                xSlider.setMin(-newMax);
                xSlider.setMax(newMax);
            }

            xSlider.setMajorTickUnit(Math.floor(xSlider.getMax() / 2.0));

            xSlider.setValue(x);
        });

        yTextField.textProperty().addListener( (observable, oldValue, newValue) ->
        {
            double y = converter.fromString(newValue);

            if (y > ySlider.getMax())
            {
                double newMax = roundToLeadingDecimal(y * 2.0);
                // Adjust bounds first
                ySlider.setMin(-newMax);
                ySlider.setMax(newMax);
            }
            else if (y < ySlider.getMin())
            {
                // Adjust bounds first
                double newMax = roundToLeadingDecimal(-y * 2.0);
                ySlider.setMin(-newMax);
                ySlider.setMax(newMax);
            }

            ySlider.setMajorTickUnit(Math.floor(ySlider.getMax() / 2.0));

            ySlider.setValue(y);
        });

        xSlider.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            // Compare slider and textbox after both have been reconverted to a standardized string.
            // If they are equivalent, don't change the textbox.
            if (!converter.toString(newValue).equals(converter.toString(converter.fromString(xTextField.getText()))))
            {
                xTextField.setText(converter.toString(newValue));
            }
        });
        ySlider.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            // Compare slider and textbox after both have been reconverted to a standardized string.
            // If they are equivalent, don't change the textbox.
            if (!converter.toString(newValue).equals(converter.toString(converter.fromString(yTextField.getText()))))
            {
                yTextField.setText(converter.toString(newValue));
            }
        });

        // Set initial value to start out synchronized.
        Vector2 originalLightCalibration = settingsModel.get("currentLightCalibration", Vector2.class);

        // Set initial bounds based on original calibration.
        double xMax = Math.max(finalScale, roundToLeadingDecimal(2 * Math.max(originalLightCalibration.x, -originalLightCalibration.x)));
        double yMax = Math.max(finalScale, roundToLeadingDecimal(2 * Math.max(originalLightCalibration.y, -originalLightCalibration.y)));

        xSlider.setMax(xMax);
        xSlider.setMin(-xMax);
        xSlider.setValue(originalLightCalibration.x);
        xSlider.setMajorTickUnit(xSlider.getMax() / 2.0);

        ySlider.setMax(yMax);
        ySlider.setMin(-yMax);
        ySlider.setValue(originalLightCalibration.y);
        ySlider.setMajorTickUnit(ySlider.getMax() / 2.0);

        xTextField.setText(converter.toString(xSlider.getValue()));
        yTextField.setText(converter.toString(ySlider.getValue()));
    }

    private static double roundToLeadingDecimal(double value)
    {
        double orderOfMagnitude = Math.floor(Math.log10(value)); // figure out the leading decimal place
        double oomScale = Math.pow(10, orderOfMagnitude); // put a 1 in the leading decimal place followed by zeros
        // Round to the leading decimal place
        return Math.round(value / oomScale) * oomScale;
    }

    public void unbind()
    {
        settingsModel.getObjectProperty("currentLightCalibration", Vector2.class).removeListener(settingsListener);
    }
}
