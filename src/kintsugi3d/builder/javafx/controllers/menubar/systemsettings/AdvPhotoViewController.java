/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar.systemsettings;//Created by alexk on 7/31/2017.

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import kintsugi3d.builder.javafx.InternalModels;
import kintsugi3d.builder.javafx.internal.SettingsModelImpl;
import kintsugi3d.builder.javafx.util.SafeDecimalNumberStringConverter;
import kintsugi3d.builder.javafx.util.SafeNumberStringConverter;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.util.ShadingParameterMode;

public class AdvPhotoViewController implements Initializable, SystemSettingsControllerBase
{//used to be called IBR Options
    //TODO: LOOK AT FORMATTING OF SLIDERS (text is hard to read)
    @FXML private TextField buehlerTextField;
    @FXML private CheckBox buehlerCheckBox;
    @FXML private CheckBox occlusionCheckBox;
    @FXML private TextField gammaTextField;
    @FXML private TextField weightExponentTextField;
    @FXML private TextField isotropyFactorTextField;
    @FXML private TextField occlusionBiasTextField;
    @FXML private Slider gammaSlider;
    @FXML private Slider weightExponentSlider;
    @FXML private Slider isotropyFactorSlider;
    @FXML private Slider occlusionBiasSlider;
    @FXML private ChoiceBox<ShadingParameterMode> weightModeChoiceBox;
    @FXML private GridPane root;

    private SettingsModelImpl settingsModel;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        init();
    }

    public void bind(SettingsModelImpl injectedSettingsModel)
    {
        buehlerCheckBox.selectedProperty().bindBidirectional(injectedSettingsModel.getBooleanProperty("buehlerAlgorithm"));

        occlusionCheckBox.selectedProperty().bindBidirectional(injectedSettingsModel.getBooleanProperty("occlusionEnabled"));
        weightModeChoiceBox.valueProperty().bindBidirectional(injectedSettingsModel.getObjectProperty("weightMode", ShadingParameterMode.class));

        buehlerTextField.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("buehlerViewCount"),
            new SafeNumberStringConverter(5));

        gammaSlider.valueProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("gamma"));
        gammaTextField.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("gamma"),
            new SafeDecimalNumberStringConverter(2.2f));

        weightExponentSlider.valueProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("weightExponent"));
        weightExponentTextField.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("weightExponent"),
            new SafeDecimalNumberStringConverter(16.0f));

        isotropyFactorSlider.valueProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("isotropyFactor"));
        isotropyFactorTextField.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("isotropyFactor"),
            new SafeDecimalNumberStringConverter(0.0f));

        occlusionBiasSlider.valueProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("occlusionBias"));
        occlusionBiasTextField.textProperty().bindBidirectional(injectedSettingsModel.getNumericProperty("occlusionBias"),
            new SafeDecimalNumberStringConverter(0.0025f));

        this.settingsModel = injectedSettingsModel;

        //TODO: NEED TO UNBIND PARAMETERS UPON CLOSING
        //uncommenting this will lead to exceptions when opening the new settings modal
//        root.getScene().getWindow().setOnCloseRequest(param -> unbind());
    }

    private void unbind()
    {
        buehlerCheckBox.selectedProperty().unbindBidirectional(settingsModel.getBooleanProperty("buehlerAlgorithm"));
        buehlerTextField.textProperty().unbindBidirectional(settingsModel.getNumericProperty("buehlerViewCount"));

        occlusionCheckBox.selectedProperty().unbindBidirectional(settingsModel.getBooleanProperty("occlusionEnabled"));

        weightModeChoiceBox.valueProperty().unbindBidirectional(settingsModel.getObjectProperty("weightMode", ShadingParameterMode.class));

        gammaSlider.valueProperty().unbindBidirectional(settingsModel.getNumericProperty("gamma"));
        gammaTextField.textProperty().unbindBidirectional(settingsModel.getNumericProperty("gamma"));

        weightExponentSlider.valueProperty().unbindBidirectional(settingsModel.getNumericProperty("weightExponent"));
        weightExponentTextField.textProperty().unbindBidirectional(settingsModel.getNumericProperty("weightExponent"));

        isotropyFactorSlider.valueProperty().unbindBidirectional(settingsModel.getNumericProperty("isotropyFactor"));
        isotropyFactorTextField.textProperty().unbindBidirectional(settingsModel.getNumericProperty("isotropyFactor"));

        occlusionBiasSlider.valueProperty().unbindBidirectional(settingsModel.getNumericProperty("occlusionBias"));
        occlusionBiasTextField.textProperty().unbindBidirectional(settingsModel.getNumericProperty("occlusionBias"));
    }

    @Override
    public void init() {
        weightModeChoiceBox.setConverter(new StringConverter<ShadingParameterMode>()
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

        StaticUtilities.makeClampedNumeric(1, 5, gammaTextField);
        StaticUtilities.makeClampedNumeric(1, 1000000, weightExponentTextField);
        StaticUtilities.makeClampedNumeric(0, 1, isotropyFactorTextField);
        StaticUtilities.makeClampedNumeric(0, 0.1, occlusionBiasTextField);
    }

    @Override
    public void bindInfo(InternalModels internalModels) {
        bind(internalModels.getSettingsModel());
    }
}