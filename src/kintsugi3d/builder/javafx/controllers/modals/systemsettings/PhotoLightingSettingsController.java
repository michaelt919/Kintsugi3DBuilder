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

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.stage.Window;
import kintsugi3d.builder.javafx.JavaFXState;
import kintsugi3d.builder.javafx.util.SafeNumberStringConverter;

public class PhotoLightingSettingsController implements SystemSettingsControllerBase
{
    @FXML private CheckBox fresnelEffectCheckBox;
    @FXML private CheckBox shadowsCheckBox;
    @FXML private CheckBox phyMaskingCheckBox;
    @FXML private CheckBox relightingCheckBox;
    @FXML private CheckBox visibleLightWidgetsCheckBox;
    private final SafeNumberStringConverter numberStringConverter = new SafeNumberStringConverter(0);

    @Override
    public void initializeSettingsPage(Window parentWindow, JavaFXState state)
    {
        fresnelEffectCheckBox.selectedProperty().bindBidirectional(
            state.getSettingsModel().getBooleanProperty("fresnelEnabled"));
        phyMaskingCheckBox.selectedProperty().bindBidirectional(
            state.getSettingsModel().getBooleanProperty("pbrGeometricAttenuationEnabled"));
        shadowsCheckBox.selectedProperty().bindBidirectional(
            state.getSettingsModel().getBooleanProperty("shadowsEnabled"));
        relightingCheckBox.selectedProperty().bindBidirectional(
            state.getSettingsModel().getBooleanProperty("relightingEnabled"));
        visibleLightWidgetsCheckBox.selectedProperty().bindBidirectional(
            state.getSettingsModel().getBooleanProperty("lightWidgetsEnabled"));
    }
}
