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
    public static void applyGlobalDefaults(GeneralSettingsModel settingsModel)
    {
        // Application state
        settingsModel.createBooleanSetting("lightCalibrationMode", false);
        settingsModel.createObjectSetting("currentLightCalibration", Vector2.ZERO);
        settingsModel.createBooleanSetting("sceneWindowOpen", false);

        // Graphics settings
        settingsModel.createBooleanSetting("multisamplingEnabled", false, true);
        settingsModel.createBooleanSetting("halfResolutionEnabled", false, true);

        // Shading options
        settingsModel.createBooleanSetting("fresnelEnabled", false, true);
        settingsModel.createBooleanSetting("pbrGeometricAttenuationEnabled", false, true);

        // Lighting options
        settingsModel.createBooleanSetting("relightingEnabled", false);
        settingsModel.createBooleanSetting("shadowsEnabled", false, true);

        // Visualization options
        settingsModel.createBooleanSetting("visibleLightsEnabled", true);
        settingsModel.createBooleanSetting("lightWidgetsEnabled", false);
        settingsModel.createBooleanSetting("visibleCameraPosesEnabled", false);
        settingsModel.createBooleanSetting("visibleSavedCameraPosesEnabled", false);
        settingsModel.createBooleanSetting("is3DGridEnabled", true, true);
        settingsModel.createBooleanSetting("isCameraVisualEnabled", false, true);
        settingsModel.createBooleanSetting("compassEnabled", false, true);

        // Image-based rendering options
        settingsModel.createObjectSetting("weightMode", ShadingParameterMode.PER_PIXEL, true);
        settingsModel.createNumericSetting("weightExponent", 16.0f, true);
        settingsModel.createNumericSetting("isotropyFactor", 0.0f, true);
        settingsModel.createBooleanSetting("buehlerAlgorithm", true, true);
        settingsModel.createNumericSetting("buehlerViewCount", 8, true);
    }

    public static void applyProjectDefaults(GeneralSettingsModel settingsModel)
    {
        // Mask / feathering settings
        settingsModel.createBooleanSetting("occlusionEnabled", true);
        settingsModel.createNumericSetting("occlusionBias", 0.0025f);
        settingsModel.createBooleanSetting("edgeProximityWeightEnabled", true);
        settingsModel.createNumericSetting("edgeProximityMargin", 0.1f);
        settingsModel.createNumericSetting("edgeProximityCutoff", 0.01f);

        // Tone calibration settings
        settingsModel.createBooleanSetting("flatfieldCorrected", false);

        // Specular fit settings
        settingsModel.createNumericSetting("textureSize", 2048);
        settingsModel.createNumericSetting("basisCount", 8);
        settingsModel.createNumericSetting("specularMinWidthFrac", 0.2f);
        settingsModel.createNumericSetting("specularMaxWidthFrac", 1.0f);
        settingsModel.createBooleanSetting("constantTermEnabled", false);
        settingsModel.createNumericSetting("basisResolution", 90);
        settingsModel.createNumericSetting("basisComplexityFrac", 1.0f);
        settingsModel.createNumericSetting("metallicity", 0.0f);
        settingsModel.createBooleanSetting("smithMaskingShadowingEnabled", true);
        settingsModel.createNumericSetting("convergenceTolerance", 0.00001f);
        settingsModel.createBooleanSetting("normalOptimizationEnabled", true);
        settingsModel.createNumericSetting("minNormalDamping", 1.0f);
        settingsModel.createNumericSetting("normalSmoothIterations", 0);
        settingsModel.createNumericSetting("unsuccessfulLMIterationsAllowed", 8);
        settingsModel.createBooleanSetting("openViewerOnProcessingComplete", false);
    }
}
