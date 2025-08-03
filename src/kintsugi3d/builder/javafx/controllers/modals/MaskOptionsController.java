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

package kintsugi3d.builder.javafx.controllers.modals;

import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import kintsugi3d.builder.javafx.internal.SettingsModelImpl;
import kintsugi3d.builder.javafx.util.SafeDecimalNumberStringConverter;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.builder.state.DefaultSettings;
import kintsugi3d.builder.state.SettingsModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class MaskOptionsController implements Initializable
{
    private static final Logger log = LoggerFactory.getLogger(MaskOptionsController.class);

    public CheckBox occlusionCheckBox;
    public TextField occlusionBiasTextField;
    public Slider occlusionBiasSlider;

    public CheckBox edgeProximityWeightCheckBox;
    public TextField edgeProximityMarginTextField;
    public Slider edgeProximityMarginSlider;

    private SettingsModel projectSettingsModel;
    private final SettingsModelImpl localSettingsModel = new SettingsModelImpl();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        // Settings will be staged locally and then applied to the project when the Apply button is clicked.
        DefaultSettings.applyProjectDefaults(localSettingsModel);

        occlusionCheckBox.selectedProperty().bindBidirectional(localSettingsModel.getBooleanProperty("occlusionEnabled"));
        occlusionBiasSlider.valueProperty().bindBidirectional(localSettingsModel.getNumericProperty("occlusionBias"));
        occlusionBiasTextField.textProperty().bindBidirectional(localSettingsModel.getNumericProperty("occlusionBias"),
            new SafeDecimalNumberStringConverter(0.0025f));

        edgeProximityWeightCheckBox.selectedProperty().bindBidirectional(localSettingsModel.getBooleanProperty("edgeProximityWeightEnabled"));
        edgeProximityMarginSlider.valueProperty().bindBidirectional(localSettingsModel.getNumericProperty("edgeProximityMargin"));
        edgeProximityMarginTextField.textProperty().bindBidirectional(localSettingsModel.getNumericProperty("edgeProximityMargin"),
            new SafeDecimalNumberStringConverter(0.1f));

        StaticUtilities.makeClampedNumeric(0, 0.1, occlusionBiasTextField);
        StaticUtilities.makeClampedNumeric(0, 1.0, edgeProximityMarginTextField);
    }

    public void setProjectSettingsModel(SettingsModel projectSettingsModel)
    {
        this.projectSettingsModel = projectSettingsModel;
        localSettingsModel.copyFrom(projectSettingsModel);
    }

    public void apply()
    {
        projectSettingsModel.copyFrom(localSettingsModel);
    }
}
