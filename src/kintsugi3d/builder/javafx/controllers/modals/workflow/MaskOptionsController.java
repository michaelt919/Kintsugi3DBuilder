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

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import kintsugi3d.builder.javafx.controllers.modals.LiveProjectSettingsControllerBase;

public class MaskOptionsController extends LiveProjectSettingsControllerBase
{
    @FXML private Pane rootNode;
    
    @FXML private CheckBox occlusionCheckBox;
    @FXML private TextField occlusionBiasTextField;
    @FXML private Slider occlusionBiasSlider;

    @FXML private CheckBox edgeProximityWeightCheckBox;
    @FXML private TextField edgeProximityMarginTextField;
    @FXML private Slider edgeProximityMarginSlider;
    @FXML private TextField edgeProximityCutoffTextField;
    @FXML private Slider edgeProximityCutoffSlider;

    @Override
    public Region getRootNode()
    {
        return rootNode;
    }

    @Override
    public void initPage()
    {
        // bind text fields
        bindFloatSetting(occlusionBiasTextField, "occlusionBias", 0, 1);
        bindBooleanSetting(occlusionCheckBox, "occlusionEnabled");
        bindBooleanSetting(edgeProximityWeightCheckBox, "edgeProximityWeightEnabled");
        bindNormalizedSetting(edgeProximityMarginTextField, "edgeProximityMargin");
        bindNormalizedSetting(edgeProximityCutoffTextField, "edgeProximityCutoff");

        // bind sliders
        bindNumericSetting(occlusionBiasSlider, "occlusionBias");
        bindNumericSetting(edgeProximityMarginSlider, "edgeProximityMargin");
        bindNumericSetting(edgeProximityCutoffSlider, "edgeProximityCutoff");

        // Disable occlusion bias setting when occlusion itself is disabled.
        occlusionBiasTextField.disableProperty().bind(occlusionCheckBox.selectedProperty().not());
        occlusionBiasSlider.disableProperty().bind(occlusionCheckBox.selectedProperty().not());

        // Disable margin / cutoff settings when edge proximity weight / feathering is disabled.
        edgeProximityMarginTextField.disableProperty().bind(edgeProximityWeightCheckBox.selectedProperty().not());
        edgeProximityMarginSlider.disableProperty().bind(edgeProximityWeightCheckBox.selectedProperty().not());
        edgeProximityCutoffTextField.disableProperty().bind(edgeProximityWeightCheckBox.selectedProperty().not());
        edgeProximityCutoffSlider.disableProperty().bind(edgeProximityWeightCheckBox.selectedProperty().not());

        setCanAdvance(true);
        setCanConfirm(true);
    }
}
