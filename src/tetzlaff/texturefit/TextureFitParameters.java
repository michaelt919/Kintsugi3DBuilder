/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.texturefit;

public class TextureFitParameters 
{
    // Sampling parameters
    private float gamma = 2.2f;
    private boolean cameraVisibilityTestEnabled = true;
    private float cameraVisibilityTestBias = 0.0025f;

    private int imageWidth = 1024;
    private int imageHeight = 1024;
    private int textureSize = 2048;
    private int textureSubdivision = 16;
    private boolean imageRescalingEnabled = false;
    private boolean imagePreprojectionUseEnabled = false;
    private boolean imagePreprojectionGenerationEnabled = false;

    private boolean areLightSourcesInfinite;

    // Diffuse fitting parameters
    private float diffuseDelta = 1.0f;
    private int diffuseIterations = 64;
    private float diffuseComputedNormalWeight = 0.0f;
    private float diffuseInputNormalWeight = Float.MAX_VALUE;

    private boolean debugModeEnabled = false;
    private boolean lightIntensityEstimationEnabled = true;
    private boolean lightOffsetEstimationEnabled = false;
    private boolean diffuseTextureEnabled = true;
    private boolean normalTextureEnabled = false;
    private boolean specularTextureEnabled = true;
    private boolean levenbergMarquardtOptimizationEnabled = false;

    private double[] linearLuminanceValues;
    private byte[] encodedLuminanceValues;

    private String primaryViewName;

    public float getGamma()
    {
        return this.gamma;
    }

    public void setGamma(float gamma)
    {
        this.gamma = gamma;
    }

    public boolean isCameraVisibilityTestEnabled()
    {
        return this.cameraVisibilityTestEnabled;
    }

    public void setCameraVisibilityTestEnabled(boolean cameraVisibilityTestEnabled)
    {
        this.cameraVisibilityTestEnabled = cameraVisibilityTestEnabled;
    }

    public float getCameraVisibilityTestBias()
    {
        return this.cameraVisibilityTestBias;
    }

    public void setCameraVisibilityTestBias(float cameraVisibilityTestBias)
    {
        this.cameraVisibilityTestBias = cameraVisibilityTestBias;
    }

    public int getImageWidth()
    {
        return this.imageWidth;
    }

    public void setImageWidth(int imageWidth)
    {
        this.imageWidth = imageWidth;
    }

    public int getImageHeight()
    {
        return this.imageHeight;
    }

    public void setImageHeight(int imageHeight)
    {
        this.imageHeight = imageHeight;
    }

    public int getTextureSize()
    {
        return this.textureSize;
    }

    public void setTextureSize(int textureSize)
    {
        this.textureSize = textureSize;
    }

    public int getTextureSubdivision()
    {
        return this.textureSubdivision;
    }

    public void setTextureSubdivision(int textureSubdivision)
    {
        this.textureSubdivision = textureSubdivision;
    }

    public boolean isImageRescalingEnabled()
    {
        return this.imageRescalingEnabled;
    }

    public void setImageRescalingEnabled(boolean imageRescalingEnabled)
    {
        this.imageRescalingEnabled = imageRescalingEnabled;
    }

    public boolean isImagePreprojectionUseEnabled()
    {
        return this.imagePreprojectionUseEnabled;
    }

    public void setImagePreprojectionUseEnabled(boolean imagePreprojectionUseEnabled)
    {
        this.imagePreprojectionUseEnabled = imagePreprojectionUseEnabled;
    }

    public boolean isImagePreprojectionGenerationEnabled()
    {
        return this.imagePreprojectionGenerationEnabled;
    }

    public void setImagePreprojectionGenerationEnabled(
            boolean imagePreprojectionGenerationEnabled)
    {
        this.imagePreprojectionGenerationEnabled = imagePreprojectionGenerationEnabled;
    }

    public float getDiffuseDelta()
    {
        return this.diffuseDelta;
    }

    public void setDiffuseDelta(float diffuseDelta)
    {
        this.diffuseDelta = diffuseDelta;
    }

    public int getDiffuseIterations()
    {
        return this.diffuseIterations;
    }

    public void setDiffuseIterations(int diffuseIterations)
    {
        this.diffuseIterations = diffuseIterations;
    }

    public float getDiffuseComputedNormalWeight()
    {
        return this.diffuseComputedNormalWeight;
    }

    public void setDiffuseComputedNormalWeight(float diffuseComputedNormalWeight)
    {
        this.diffuseComputedNormalWeight = diffuseComputedNormalWeight;
    }

    public float getDiffuseInputNormalWeight()
    {
        return this.diffuseInputNormalWeight;
    }

    public void setDiffuseInputNormalWeight(float diffuseInputNormalWeight)
    {
        this.diffuseInputNormalWeight = diffuseInputNormalWeight;
    }

    public boolean areLightSourcesInfinite()
    {
        return this.areLightSourcesInfinite;
    }

    public String getPrimaryViewName()
    {
        return primaryViewName;
    }

    public void setPrimaryViewName(String primaryViewName)
    {
        this.primaryViewName = primaryViewName;
    }

    public boolean isLightOffsetEstimationEnabled()
    {
        return this.lightOffsetEstimationEnabled;
    }

    public void setLightOffsetEstimationEnabled(boolean lightOffsetEstimationEnabled)
    {
        this.lightOffsetEstimationEnabled = lightOffsetEstimationEnabled;
    }

    public boolean isDiffuseTextureEnabled()
    {
        return this.diffuseTextureEnabled;
    }

    public void setDiffuseTextureEnabled(boolean diffuseTextureEnabled)
    {
        this.diffuseTextureEnabled = diffuseTextureEnabled;
    }

    public boolean isNormalTextureEnabled()
    {
        return this.normalTextureEnabled;
    }

    public void setNormalTextureEnabled(boolean normalTextureEnabled)
    {
        this.normalTextureEnabled = normalTextureEnabled;
    }

    public boolean isSpecularTextureEnabled()
    {
        return this.specularTextureEnabled;
    }

    public void setSpecularTextureEnabled(boolean specularTextureEnabled)
    {
        this.specularTextureEnabled = specularTextureEnabled;
    }

    public boolean isLevenbergMarquardtOptimizationEnabled()
    {
        return this.levenbergMarquardtOptimizationEnabled;
    }

    public void setLevenbergMarquardtOptimizationEnabled(boolean levenbergMarquardtOptimizationEnabled)
    {
        this.levenbergMarquardtOptimizationEnabled = levenbergMarquardtOptimizationEnabled;
    }

    public double[] getLinearLuminanceValues()
    {
        return this.linearLuminanceValues;
    }

    public void setLinearLuminanceValues(double... linearLuminanceValues)
    {
        this.linearLuminanceValues = linearLuminanceValues;
    }

    public byte[] getEncodedLuminanceValues()
    {
        return this.encodedLuminanceValues;
    }

    public void setEncodedLuminanceValues(byte... encodedLuminanceValues)
    {
        this.encodedLuminanceValues = encodedLuminanceValues;
    }

    public boolean isLightIntensityEstimationEnabled()
    {
        return lightIntensityEstimationEnabled;
    }

    public void setLightIntensityEstimationEnabled(boolean lightIntensityEstimationEnabled)
    {
        this.lightIntensityEstimationEnabled = lightIntensityEstimationEnabled;
    }

    public boolean isDebugModeEnabled()
    {
        return debugModeEnabled;
    }

    public void setDebugModeEnabled(boolean debugModeEnabled)
    {
        this.debugModeEnabled = debugModeEnabled;
    }

}
