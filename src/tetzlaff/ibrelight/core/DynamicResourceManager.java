package tetzlaff.ibrelight.core;

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.util.AbstractImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Optional;

public interface DynamicResourceManager
{
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
    Optional<AbstractImage> loadEnvironmentMap(File environmentFile) throws FileNotFoundException;

    /**
     * Sets the tonemapping curve used to interpret the photographic data.
     * The data points specified using this function will be interpolated to form a smooth decoding curve.
     * @param linearLuminanceValues A sequence of luminance values interpreted as physically linear (gamma decoded).
     * @param encodedLuminanceValues A sequence of gamma-encoded luminance values representing the actual pixel values
     *                               that might be found in the photographs.
     */
    void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues);

    /**
     * Accept the current light calibration (intended to be used only by the application, when in light calibration mode).
     */
    void setLightCalibration(Vector3 lightCalibration);
}
