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

package kintsugi3d.builder.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Optional;

import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.EncodableColorImage;

public interface DynamicResourceManager
{
    void requestFragmentShader(File shaderFile);

    void requestFragmentShader(File shaderFile, Map<String, Optional<Object>> extraDefines);

    /**
     * Load a new backplate image.
     * @param backplateFile The backplate image file.
     * @throws FileNotFoundException If the backplate image is not found.
     */
    void loadBackplate(File backplateFile) throws FileNotFoundException;

    /**
     * Load a new environment map image.
     * @param environmentFile The environment map image file.
     * @throws FileNotFoundException If the environment map image is not found.
     */
    Optional<EncodableColorImage> loadEnvironmentMap(File environmentFile) throws FileNotFoundException;

    /**
     * Sets the tonemapping curve used to interpret the photographic data.
     * The data points specified using this function will be interpolated to form a smooth decoding curve.
     * @param linearLuminanceValues A sequence of luminance values interpreted as physically linear (linear sRGB / "gamma decoded").
     * @param encodedLuminanceValues A sequence of sRGB ("gamma-encoded") luminance values representing the actual pixel values
     *                               that might be found in the photographs.
     */
    void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues);

    /**
     * Accept the current light calibration (intended to be used only by the application, when in light calibration mode).
     */
    void setLightCalibration(Vector3 lightCalibration);
}
