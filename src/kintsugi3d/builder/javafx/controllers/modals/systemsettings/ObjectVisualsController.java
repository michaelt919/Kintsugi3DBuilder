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

package kintsugi3d.builder.javafx.controllers.modals.systemsettings;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import javafx.util.StringConverter;
import kintsugi3d.builder.javafx.core.JavaFXState;

public class ObjectVisualsController implements SystemSettingsControllerBase
{

    private static final Number DEFAULT_VALUE = 1024;//default value for Preload vis and shadow testing txt fields
    private final IntegerProperty previewWidthIntProperty = new SimpleIntegerProperty((Integer) DEFAULT_VALUE);
    private final IntegerProperty previewHeightIntProperty = new SimpleIntegerProperty((Integer) DEFAULT_VALUE);
    private final IntegerProperty depthWidthIntProperty = new SimpleIntegerProperty((Integer) DEFAULT_VALUE);
    private final IntegerProperty depthHeightIntProperty = new SimpleIntegerProperty((Integer) DEFAULT_VALUE);

    @FXML private TextField previewWidthTxtField;
    @FXML private TextField previewHeightTxtField;

    @FXML private CheckBox imageCompressionCheckBox;
    @FXML private CheckBox preloadVisibilityEtcCheckBox;
    @FXML private TextField depthWidthTxtField;
    @FXML private TextField depthHeightTxtField;
    @FXML private CheckBox mipmapCheckBox;
    @FXML private CheckBox reduceViewportResCheckBox;

    @Override
    public void initializeSettingsPage(Window parentWindow, JavaFXState state)
    {
        // Disable width / height fields for depth maps if they are disabled.
        depthWidthTxtField.disableProperty().bind(preloadVisibilityEtcCheckBox.selectedProperty().not());
        depthHeightTxtField.disableProperty().bind(preloadVisibilityEtcCheckBox.selectedProperty().not());

        imageCompressionCheckBox.selectedProperty().bindBidirectional(
            state.getLoadOptionsModel().compression);

        setupTxtFieldProperties(previewWidthIntProperty, previewWidthTxtField);
        setupTxtFieldProperties(previewHeightIntProperty, previewHeightTxtField);
        setupTxtFieldProperties(depthWidthIntProperty, depthWidthTxtField);
        setupTxtFieldProperties(depthHeightIntProperty, depthHeightTxtField);

        previewWidthIntProperty.bindBidirectional(state.getLoadOptionsModel().previewWidth);
        previewHeightIntProperty.bindBidirectional(state.getLoadOptionsModel().previewHeight);

        depthWidthTxtField.disableProperty().bind(preloadVisibilityEtcCheckBox.selectedProperty().not());
        depthHeightTxtField.disableProperty().bind(preloadVisibilityEtcCheckBox.selectedProperty().not());

        preloadVisibilityEtcCheckBox.selectedProperty().bindBidirectional(
            state.getLoadOptionsModel().depthImages);

        depthWidthIntProperty.bindBidirectional(state.getLoadOptionsModel().depthWidth);
        depthHeightIntProperty.bindBidirectional(state.getLoadOptionsModel().depthHeight);

        mipmapCheckBox.selectedProperty().bindBidirectional(state.getLoadOptionsModel().mipmaps);
        reduceViewportResCheckBox.selectedProperty().bindBidirectional(
            state.getSettingsModel().getBooleanProperty("halfResolutionEnabled"));
    }

    //this function is used to hook up the text field's string property to the backend
    //StringProperty --> IntegerProperty
    private static void setupTxtFieldProperties(IntegerProperty integerProperty, TextField txtField)
    {
        StringConverter<Number> numberStringConverter = new StringConverter<>()
        {
            @Override
            public String toString(Number object)
            {
                if (object != null)
                {
                    return Integer.toString(object.intValue());
                }
                else
                {
                    return String.valueOf(DEFAULT_VALUE);
                }
            }

            @Override
            public Number fromString(String string)
            {
                try
                {
                    return Integer.valueOf(string);
                }
                catch (NumberFormatException nfe)
                {
                    return DEFAULT_VALUE;
                }
            }
        };
        txtField.textProperty().bindBidirectional(integerProperty, numberStringConverter);
        txtField.focusedProperty().addListener((ob, o, n) ->
        {
            if (o && !n)
            {
                txtField.setText(integerProperty.getValue().toString());
            }
        });
        integerProperty.addListener((ob, o, n) ->
        {
            if (n.intValue() < 1)
            {
                integerProperty.setValue(1);
            }
        });
    }
}
