package tetzlaff.reflectancefit;

/**
 * A class containing options that may be specified to control the behavior of the reflectance parameter estimation algorithm.
 */
public class Options
{
    private String primaryViewName;

    private float gamma = 2.2f;
    private double[] linearLuminanceValues;
    private byte[] encodedLuminanceValues;

    private boolean imageRescalingEnabled = false;
    private int imageWidth = 1024;
    private int imageHeight = 1024;

    private int textureSize = 2048;
    private boolean diffuseTextureEnabled = true;
    private boolean specularTextureEnabled = true;

    private boolean cameraVisibilityTestEnabled = true;
    private float cameraVisibilityTestBias = 0.0025f;
    private int textureSubdivision = 16;
    private boolean lightIntensityEstimationEnabled = true;
    private float diffuseDelta = 1.0f;
    private int diffuseIterations = 64;
    private float diffuseComputedNormalWeight = 1.0f;

    /**
     * Gets the name of a view that should have the same camera displacement and orientation as the photo that was used to obtain the ColorChecker
     * measurements.  It is used to automatically compute the intensity of the light source, which is in turn used to obtain absolute reflectance
     * measurements.
     * @return The name of the primary view.
     */
    public String getPrimaryViewName()
    {
        return primaryViewName;
    }

    /**
     * Sets the name of a view that should have the same camera displacement and orientation as the photo that was used to obtain the ColorChecker
     * measurements.  It is used to automatically compute the intensity of the light source, which is in turn used to obtain absolute reflectance
     * measurements.
     * @param primaryViewName The name of the primary view.
     */
    public void setPrimaryViewName(String primaryViewName)
    {
        this.primaryViewName = primaryViewName;
    }

    /**
     * Defines the color curve of the image.
     * This should correspond to the gamma used when processing the photos using DCRaw.
     * @return The exponent used for gamma encoding/decoding.
     */
    public float getGamma()
    {
        return this.gamma;
    }

    /**
     * Defines the color curve of the image.
     * This should correspond to the gamma used when processing the photos using DCRaw.
     * @param gamma The exponent used for gamma encoding/decoding.
     */
    public void setGamma(float gamma)
    {
        this.gamma = gamma;
    }

    /**
     * Gets an array containing the known, linear reflectance values of one or more color calibration targets.
     * This array should have the same length as the one returned by getEncodedLuminanceValues().  If this is not the case, behavior may be undefined.
     * The array may be null if color calibration information is not available.
     * If this option is set to null, it will be assumed that a pixel value of 255 corresponds to 100% diffuse reflectance with a perfect gamma curve.
     * @return An array containing the known, linear reflectance values, or null if this information is unavailable.
     */
    public double[] getLinearLuminanceValues()
    {
        return this.linearLuminanceValues.clone();
    }

    /**
     * Specifies a reference to an array containing the known, linear reflectance values of one or more color calibration targets.
     * This array should have the same length as the one returned by getEncodedLuminanceValues().  If this is not the case, behavior may be undefined.
     * The array reference may be null if color calibration information is not available.
     * If this option is set to null, it will be assumed that a pixel value of 255 corresponds to 100% diffuse reflectance with a perfect gamma curve.
     * @param linearLuminanceValues An array containing the known, linear reflectance values, or null if this information is unavailable.
     */
    public void setLinearLuminanceValues(double... linearLuminanceValues)
    {
        this.linearLuminanceValues = linearLuminanceValues.clone();
    }

    /**
     * Gets an array containing the representative pixel values (ranging from 0-255) for one or more color calibration targets.
     * The values in the array are assumed to be unsigned bytes; this means that it is necessary to mask each value with 0x000000FF in order for Java
     * to correctly interpret the value as an exclusively positive value between 0 and 255.
     * This array should have the same length as the one returned by getLinearLuminanceValues().  If this is not the case, behavior may be undefined.
     * The array may be null if color calibration information is not available.
     * If this option is set to null, it will be assumed that a pixel value of 255 corresponds to 100% diffuse reflectance with a perfect gamma curve.
     * @return An array containing the known, linear reflectance values, or null if this information is unavailable.
     */
    public byte[] getEncodedLuminanceValues()
    {
        return this.encodedLuminanceValues.clone();
    }

    /**
     * Specifies a reference to an array containing the representative pixel values (ranging from 0-255) for one or more color calibration targets.
     * The values in the array are assumed to be unsigned bytes; this means that it is necessary to mask each value with 0x000000FF in order for Java
     * to correctly interpret the value as an exclusively positive value between 0 and 255.
     * This array should have the same length as the one returned by getLinearLuminanceValues().  If this is not the case, behavior may be undefined.
     * The array reference may be null if color calibration information is not available.
     * If this option is set to null, it will be assumed that a pixel value of 255 corresponds to 100% diffuse reflectance with a perfect gamma curve.
     * @param encodedLuminanceValues An array containing the known, linear reflectance values, or null if this information is unavailable.
     */
    public void setEncodedLuminanceValues(byte... encodedLuminanceValues)
    {
        this.encodedLuminanceValues = encodedLuminanceValues.clone();
    }

    /**
     * Gets whether the images should automatically be resized.
     * @return true if the images should be resized, false otherwise.
     */
    public boolean isImageRescalingEnabled()
    {
        return this.imageRescalingEnabled;
    }

    /**
     * Sets whether the images should automatically be resized.
     * Setting this to true and also specifying an appropriate resolution using setImageWidth() and setImageHeight() can reduce graphics card memory
     * requirements.
     * @param imageRescalingEnabled true if the images should be resized, false otherwise.
     */
    public void setImageRescalingEnabled(boolean imageRescalingEnabled)
    {
        this.imageRescalingEnabled = imageRescalingEnabled;
    }

    /**
     * Gets the target width of the images if rescaling is enabled.
     * @return The target image width.
     */
    public int getImageWidth()
    {
        return this.imageWidth;
    }

    /**
     * Sets the target width of the images if rescaling is enabled.
     * @param imageWidth The target image width.
     */
    public void setImageWidth(int imageWidth)
    {
        this.imageWidth = imageWidth;
    }

    /**
     * Gets the target height of the images if rescaling is enabled.
     * @return The target image height.
     */
    public int getImageHeight()
    {
        return this.imageHeight;
    }

    /**
     * Sets the target height of the images if rescaling is enabled.
     * @param imageHeight The target image height.
     */
    public void setImageHeight(int imageHeight)
    {
        this.imageHeight = imageHeight;
    }

    /**
     * Gets the desired resolution of the textures to be generated.
     * @return The designated texture resolution.
     */
    public int getTextureSize()
    {
        return this.textureSize;
    }

    /**
     * Sets the desired resolution of the textures to be generated.
     * It is best if this value is a power of 2 (i.e. 256, 512, 1024, 2048, 4096, 8192, etc.)
     * @param textureSize The designated texture resolution.
     */
    public void setTextureSize(int textureSize)
    {
        this.textureSize = textureSize;
    }

    /**
     * Gets whether or not diffuse reflectance is to be included in the reflectance estimation.
     * @return true if diffuse reflectance is to be considered, false otherwise.
     */
    public boolean isDiffuseTextureEnabled()
    {
        return this.diffuseTextureEnabled;
    }

    /**
     * Sets whether or not diffuse reflectance is to be included in the reflectance estimation.
     * @param diffuseTextureEnabled true if diffuse reflectance is to be considered, false otherwise.
     */
    public void setDiffuseTextureEnabled(boolean diffuseTextureEnabled)
    {
        this.diffuseTextureEnabled = diffuseTextureEnabled;
    }

    /**
     * Gets whether or not specular reflectance is to be included in the reflectance estimation.
     * @return true if specular reflectance is to be considered, false otherwise.
     */
    public boolean isSpecularTextureEnabled()
    {
        return this.specularTextureEnabled;
    }

    /**
     * Sets whether or not specualr reflectance is to be included in the reflectance estimation.
     * @param specularTextureEnabled true if specular reflectance is to be considered, false otherwise.
     */
    public void setSpecularTextureEnabled(boolean specularTextureEnabled)
    {
        this.specularTextureEnabled = specularTextureEnabled;
    }

    /**
     * Gets whether or not a visibility test is to be used to mask out occluded samples.
     * This prevents color data from foreground surfaces from being projected onto background surfaces in images where the background surface is
     * occluded by the foreground surface.  Should usually be left enabled.
     * @return True if the visibility test is enabled; false otherwise.
     */
    public boolean isCameraVisibilityTestEnabled()
    {
        return this.cameraVisibilityTestEnabled;
    }

    /**
     * Sets whether or not a visibility test is to be used to mask out occluded samples.
     * This prevents color data from foreground surfaces from being projected onto background surfaces in images where the background surface is
     * occluded by the foreground surface.  Should usually be left enabled.
     * @param cameraVisibilityTestEnabled True if the visibility test is enabled; false otherwise.
     */
    public void setCameraVisibilityTestEnabled(boolean cameraVisibilityTestEnabled)
    {
        this.cameraVisibilityTestEnabled = cameraVisibilityTestEnabled;
    }

    /**
     * Gets the bias to be applied to the visibility test, a small number that eliminates errors caused by round-off.
     * @return The visibility test bias.
     */
    public float getCameraVisibilityTestBias()
    {
        return this.cameraVisibilityTestBias;
    }

    /**
     * Sets the bias to be applied to the visibility test, a small number that eliminates errors caused by round-off.
     * @param cameraVisibilityTestBias The visibility test bias.
     */
    public void setCameraVisibilityTestBias(float cameraVisibilityTestBias)
    {
        this.cameraVisibilityTestBias = cameraVisibilityTestBias;
    }

    /**
     * Gets the number of block divisions that should be applied to the texture along each dimension (width and height).
     * Since this is applied to both the width and the height, the number of total blocks that will be used is equal to this quantity squared.
     * @return The number of texture subdivisions.
     */
    public int getTextureSubdivision()
    {
        return this.textureSubdivision;
    }

    /**
     * Sets the number of block divisions that should be applied to the texture along each dimension (width and height).
     * Since this is applied to both the width and the height, the number of total blocks that will be used is equal to this quantity squared.
     * Setting this to a larger value can prevent the program from crashing, at the cost of some performance.
     * @param textureSubdivision The number of texture subdivisions.
     */
    public void setTextureSubdivision(int textureSubdivision)
    {
        this.textureSubdivision = textureSubdivision;
    }

    /**
     * Gets whether or not the intensity of the light source is determined automatically (and globally) based on the distance from the camera to the
     * object in the "primary view."
     * If this is disabled, the light intensity must be explicitly specified in the ViewSet, or it will be assumed to be 1.0.
     * @return true if global light intensity estimation is enabled, or false otherwise.
     */
    public boolean isLightIntensityEstimationEnabled()
    {
        return lightIntensityEstimationEnabled;
    }

    /**
     * Sets whether or not the intensity of the light source is determined automatically (and globally) based on the distance from the camera to the
     * object in the "primary view."
     * If this is disabled, the light intensity must be explicitly specified in the ViewSet, or it will be assumed to be 1.0.
     * @param lightIntensityEstimationEnabled true if global light intensity estimation is enabled, or false otherwise.
     */
    public void setLightIntensityEstimationEnabled(boolean lightIntensityEstimationEnabled)
    {
        this.lightIntensityEstimationEnabled = lightIntensityEstimationEnabled;
    }

    /**
     * Gets a parameter that affects how much specularities and shadows are filtered.  A lower value will filter more aggressively.
     * @return The diffuse delta parameter.
     */
    public float getDiffuseDelta()
    {
        return this.diffuseDelta;
    }

    /**
     * Sets a parameter that affects how much specularities and shadows are filtered.
     * A lower value will filter more aggressively, but setting this too low could cause visual issues.
     * @param diffuseDelta The diffuse delta parameter.
     */
    public void setDiffuseDelta(float diffuseDelta)
    {
        this.diffuseDelta = diffuseDelta;
    }

    /**
     * Gets the number of iterations to use for the diffuse reflectance estimation step.
     * @return The number of diffuse estimation iterations.
     */
    public int getDiffuseIterations()
    {
        return this.diffuseIterations;
    }

    /**
     * Sets the number of iterations to use for the diffuse reflectance estimation step.
     * More iterations will take longer, but this additional processing will improve the quality of the textures by better filtering out specularities
     * and shadows.  Setting this higher will always improve texture quality, but setting it too high may cause the graphics card to time-out, which
     * will result in program failure.  To combat this time-out issue, the texture subdivision setting can be set higher.
     * @param diffuseIterations The number of diffuse estimation iterations.
     */
    public void setDiffuseIterations(int diffuseIterations)
    {
        this.diffuseIterations = diffuseIterations;
    }

    /**
     * Gets a tolerance factor for cases where the linear regression used to compute the diffuse surface normal is ill-conditioned.
     * Important: This parameter has no effect on the normal computed as part of the specular reflectance estimation.
     * @return he tolerance weight for the surface normal computed with the diffuse reflectance.
     */
    public float getDiffuseComputedNormalWeight()
    {
        return this.diffuseComputedNormalWeight;
    }

    /**
     * Sets a tolerance factor for cases where the linear regression used to compute the diffuse surface normal is ill-conditioned.
     * Setting this higher will increase the tolerance, enabling high resolution surface normals to be used with sparse or poorly sampled datasets,
     * but possibly introducing visual artifacts.  Setting this lower will cause the program to begin relying more and more on the surface normals
     * provided in the input dataset (i.e., the ones computed by Photoscan).  Setting this to zero will cause the software to use the normals computed
     * by Photoscan exclusively; in this limiting case it will not compute a high resolution surface normal texture at all.  Setting this to a very
     * high number will ensure that the program almost always computes surface normals while relying very little on the ones provided by Photoscan.
     * Important: This parameter has no effect on the normal computed as part of the specular reflectance estimation.
     * @param diffuseComputedNormalWeight The tolerance weight for the surface normal computed with the diffuse reflectance.
     */
    public void setDiffuseComputedNormalWeight(float diffuseComputedNormalWeight)
    {
        this.diffuseComputedNormalWeight = diffuseComputedNormalWeight;
    }
}
