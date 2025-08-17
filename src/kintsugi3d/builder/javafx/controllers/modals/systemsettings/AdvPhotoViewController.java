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

package kintsugi3d.builder.javafx.controllers.modals.systemsettings;//Created by alexk on 7/31/2017.

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import javafx.util.StringConverter;
import kintsugi3d.builder.javafx.core.JavaFXState;
import kintsugi3d.builder.javafx.internal.SettingsModelImpl;
import kintsugi3d.builder.javafx.util.SafeDecimalNumberStringConverter;
import kintsugi3d.builder.javafx.util.SafeNumberStringConverter;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.util.ShadingParameterMode;

public class AdvPhotoViewController implements SystemSettingsControllerBase
{
    //T ODO: LOOK AT FORMATTING OF SLIDERS (text is hard to read)
    @FXML private TextField buehlerTextField;
    @FXML private CheckBox buehlerCheckBox;
    @FXML private TextField weightExponentTextField;
    @FXML private TextField isotropyFactorTextField;
    @FXML private Slider weightExponentSlider;
    @FXML private Slider isotropyFactorSlider;
    @FXML private ChoiceBox<ShadingParameterMode> weightModeChoiceBox;

    private SettingsModelImpl settingsModel;

    @Override
    public void initializeSettingsPage(Window parentWindow, JavaFXState state)
    {
        weightModeChoiceBox.setConverter(new StringConverter<>()
        {
            @Override
            public String toString(ShadingParameterMode object)
            {
                return object.name();
            }

            @Override
            public ShadingParameterMode fromString(String string)
            {
                return ShadingParameterMode.valueOf(string);
            }
        });
        weightModeChoiceBox.getItems().addAll(ShadingParameterMode.values());

        StaticUtilities.makeClampedNumeric(1, 1000000, weightExponentTextField);
        StaticUtilities.makeClampedNumeric(0, 1, isotropyFactorTextField);

        bind(state.getSettingsModel());
    }

    public void bind(SettingsModelImpl injectedSettingsModel)
    {
        buehlerCheckBox.selectedProperty().bindBidirectional(injectedSettingsModel.getBooleanProperty("buehlerAlgorithm"));

        weightModeChoiceBox.valueProperty().bindBidirectional(injectedSettingsModel.getObjectProperty("weightMode", ShadingParameterMode.class));

        buehlerTextField.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("buehlerViewCount"),
            new SafeNumberStringConverter(5));

        weightExponentSlider.valueProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("weightExponent"));
        weightExponentTextField.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("weightExponent"),
            new SafeDecimalNumberStringConverter(16.0f));

        isotropyFactorSlider.valueProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("isotropyFactor"));
        isotropyFactorTextField.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("isotropyFactor"),
            new SafeDecimalNumberStringConverter(0.0f));

        this.settingsModel = injectedSettingsModel;

        //TODO: NEED TO UNBIND PARAMETERS UPON CLOSING
        //uncommenting this will lead to exceptions when opening the new settings modal
//        root.getScene().getWindow().setOnCloseRequest(param -> unbind());
    }

    private void unbind()
    {
        buehlerCheckBox.selectedProperty().unbindBidirectional(settingsModel.getBooleanProperty("buehlerAlgorithm"));
        buehlerTextField.textProperty().unbindBidirectional(settingsModel.getNumericProperty("buehlerViewCount"));

        weightModeChoiceBox.valueProperty().unbindBidirectional(settingsModel.getObjectProperty("weightMode", ShadingParameterMode.class));

        weightExponentSlider.valueProperty().unbindBidirectional(settingsModel.getNumericProperty("weightExponent"));
        weightExponentTextField.textProperty().unbindBidirectional(settingsModel.getNumericProperty("weightExponent"));

        isotropyFactorSlider.valueProperty().unbindBidirectional(settingsModel.getNumericProperty("isotropyFactor"));
        isotropyFactorTextField.textProperty().unbindBidirectional(settingsModel.getNumericProperty("isotropyFactor"));
    }
}