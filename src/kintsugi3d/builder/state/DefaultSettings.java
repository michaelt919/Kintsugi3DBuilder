/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state;

import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.util.ShadingParameterMode;

public final class DefaultSettings
{
    public static void apply(SettingsModel settingsModel)
    {
        settingsModel.createBooleanSetting("lightCalibrationMode", false);
        settingsModel.createObjectSetting("currentLightCalibration", Vector2.ZERO);
        settingsModel.createBooleanSetting("occlusionEnabled", true, true);
        settingsModel.createBooleanSetting("fresnelEnabled", false, true);
        settingsModel.createBooleanSetting("pbrGeometricAttenuationEnabled", false, true);
        settingsModel.createBooleanSetting("relightingEnabled", false);
        settingsModel.createBooleanSetting("shadowsEnabled", false, true);
        settingsModel.createBooleanSetting("visibleLightsEnabled", true);
        settingsModel.createBooleanSetting("lightWidgetsEnabled", false);
        settingsModel.createBooleanSetting("visibleCameraPosesEnabled", false);
        settingsModel.createBooleanSetting("visibleSavedCameraPosesEnabled", false);
        settingsModel.createNumericSetting("weightExponent", 16.0f, true);
        settingsModel.createNumericSetting("isotropyFactor", 0.0f, true);
        settingsModel.createNumericSetting("occlusionBias", 0.0025f, true);
        settingsModel.createObjectSetting("weightMode", ShadingParameterMode.PER_PIXEL, true);
        settingsModel.createBooleanSetting("is3DGridEnabled", true, true);
        settingsModel.createBooleanSetting("isCameraVisualEnabled", false, true);
        settingsModel.createBooleanSetting("compassEnabled", false, true);
        settingsModel.createBooleanSetting("multisamplingEnabled", false, true);
        settingsModel.createBooleanSetting("halfResolutionEnabled", false, true);
        settingsModel.createBooleanSetting("sceneWindowOpen", false);
        settingsModel.createBooleanSetting("buehlerAlgorithm", true, true);
        settingsModel.createNumericSetting("buehlerViewCount", 5, true);
    }
}
