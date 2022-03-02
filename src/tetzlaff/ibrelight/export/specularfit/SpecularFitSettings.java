/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import java.io.File;

import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.models.ReadonlySettingsModel;

public class SpecularFitSettings extends TextureFitSettings
{
    public final int basisCount;
    public final int microfacetDistributionResolution;

    private double convergenceTolerance = 0.00001;
    private double specularSmoothness = 0.0;
    private double metallicity = 0.0;
    private boolean normalRefinementEnabled = true;
    private double minNormalDamping = 1.0;
    private int normalSmoothingIterations = 0;

    private boolean smithMaskingShadowingEnabled = true;
    private boolean levenbergMarquardtEnabled = true;
    private int unsuccessfulLMIterationsAllowed = 8;

    private ViewSet reconstructionViewSet = null;

    /**
     * Constructs an object to hold the settings for specular texture fitting.
     * @param width The width of the textures
     * @param height The height of the textures
     * @param basisCount The number of basis functions to use for the specular lobe.
     * @param microfacetDistributionResolution The number of discrete values in the definition of the specular lobe.
     * @param outputDirectory The directory where the results should be saved.
     * @param additional Other settings from the IBRelight interface
     */
    public SpecularFitSettings(int width, int height, int basisCount, int microfacetDistributionResolution,
                               File outputDirectory, ReadonlySettingsModel additional)
    {
        super(width, height, outputDirectory, additional);

        if (basisCount <= 0)
        {
            throw new IllegalArgumentException("Basis count must be greater than zero.");
        }
        else if (microfacetDistributionResolution <= 0)
        {
            throw new IllegalArgumentException("Microfacet distribution resolution must be greater than zero.");
        }

        this.basisCount = basisCount;
        this.microfacetDistributionResolution = microfacetDistributionResolution;
    }

    /**
     * Gets the convergence tolerance used to determine whether the Levenberg-Marquardt algorithm for optimizing
     * the normal map has converged.
     * @return
     */
    public double getConvergenceTolerance()
    {
        return convergenceTolerance;
    }

    /**
     * Sets the convergence tolerance used to determine whether the Levenberg-Marquardt algorithm for optimizing
     * the normal map has converged.
     * @param convergenceTolerance
     */
    public void setConvergenceTolerance(double convergenceTolerance)
    {
        if (convergenceTolerance < 0)
        {
            throw new IllegalArgumentException("Convergence tolerance must not be less than zero.");
        }

        this.convergenceTolerance = convergenceTolerance;
    }

    /**
     * Gets the required smoothness for the specular lobe (the width of the smoothstep function used to optimize it).
     * @return
     */
    public double getSpecularSmoothness()
    {
        return specularSmoothness;
    }

    /**
     * Sets the required smoothness for the specular lobe (the width of the smoothstep function used to optimize it).
     * @param specularSmoothness
     */
    public void setSpecularSmoothness(double specularSmoothness)
    {
        if (specularSmoothness < 0)
        {
            throw new IllegalArgumentException("Specular smoothness must not be less than zero.");
        }

        this.specularSmoothness = specularSmoothness;
    }

    /**
     * Gets the assumed metallicity of the material (metallic meaning that the diffuse reflectance exhibits specular characteristics
     * like the Fresnel effect and is scattered by first-surface microfacet geometry, not subsurface scattering)
     * @return
     */
    public double getMetallicity()
    {
        return metallicity;
    }

    /**
     * Sets the assumed metallicity of the material (metallic meaning that the diffuse reflectance exhibits specular characteristics
     * like the Fresnel effect and is scattered by first-surface microfacet geometry, not subsurface scattering)
     * @return
     */
    public void setMetallicity(double metallicity)
    {
        if (metallicity < 0 || metallicity > 1)
        {
            throw new IllegalArgumentException("Metallicity must be between 0 and 1.");
        }

        this.metallicity = metallicity;
    }

    /**
     * Gets whether normal refinement is enabled (if not, the vertex normals will be assumed to be accurate enough)
     * @return
     */
    public boolean isNormalRefinementEnabled()
    {
        return normalRefinementEnabled;
    }

    /**
     * Sets whether normal refinement is enabled (if not, the vertex normals will be assumed to be accurate enough)
     * @param normalRefinementEnabled
     */
    public void setNormalRefinementEnabled(boolean normalRefinementEnabled)
    {
        this.normalRefinementEnabled = normalRefinementEnabled;
    }

    /**
     * Gets the minimum allowed damping factor for the the Levenberg-Marquardt algorithm for optimizing the normal map.
     * Default is 1.0.
     * Negative values will have the same effect as 0.0.
     * @return
     */
    public double getMinNormalDamping()
    {
        return minNormalDamping;
    }

    /**
     * Sets the minimum allowed damping factor for the the Levenberg-Marquardt algorithm for optimizing the normal map.
     * Default is 1.0.
     * Negative values will have the same effect as 0.0.
     * @param minNormalDamping
     */
    public void setMinNormalDamping(double minNormalDamping)
    {
        // Negative values shouldn't break anything here.
        this.minNormalDamping = minNormalDamping;
    }

    /**
     * Gets the number of smoothing iterations for the normal map.  Default is zero (no smoothing).
     * Negative values will have the same effect as 0.
     * @return
     */
    public int getNormalSmoothingIterations()
    {
        return normalSmoothingIterations;
    }

    /**
     * Sets the number of smoothing iterations for the normal map.  Default is zero (no smoothing).
     * Negative values will have the same effect as 0.
     * @param normalSmoothingIterations
     */
    public void setNormalSmoothingIterations(int normalSmoothingIterations)
    {
        // Negative values shouldn't break anything here.
        this.normalSmoothingIterations = normalSmoothingIterations;
    }

    /**
     * Gets the view set used to create the reconstructed images for manually evaluating the effectiveness of the fit.
     * @return
     */
    public ViewSet getReconstructionViewSet()
    {
        return reconstructionViewSet;
    }

    /**
     * Sets the view set used to create the reconstructed images for manually evaluating the effectiveness of the fit.
     * @return
     */
    public void setReconstructionViewSet(ViewSet reconstructionViewSet)
    {
        this.reconstructionViewSet = reconstructionViewSet;
    }

    /**
     * Whether or not to use height-correlated Smith for masking / shadowing.  Default is true.
     * @return
     */
    public boolean isSmithMaskingShadowingEnabled()
    {
        return smithMaskingShadowingEnabled;
    }

    /**
     * Whether or not to use height-correlated Smith for masking / shadowing.  Default is true.
     * @param smithMaskingShadowingEnabled
     */
    public void setSmithMaskingShadowingEnabled(boolean smithMaskingShadowingEnabled)
    {
        this.smithMaskingShadowingEnabled = smithMaskingShadowingEnabled;
    }

    /**
     * Whether or not to use Levenberg-Marquardt for normal optimization.
     * Default is true.  Highly recommended unless attempting to reproduce Nam et al. 2018.
     * @return
     */
    public boolean isLevenbergMarquardtEnabled()
    {
        return levenbergMarquardtEnabled;
    }

    /**
     * Whether or not to use Levenberg-Marquardt for normal optimization.
     * Highly recommended unless attempting to reproduce Nam et al. 2018.
     * @param levenbergMarquardtEnabled
     */
    public void setLevenbergMarquardtEnabled(boolean levenbergMarquardtEnabled)
    {
        this.levenbergMarquardtEnabled = levenbergMarquardtEnabled;
    }

    /**
     * The number of unsuccessful iterations of Levenberg-Marquardt (iterations which fail to decrease the error
     * by the required threshold) before the algorithm will be considered terminated.
     * @return
     */
    public int getUnsuccessfulLMIterationsAllowed()
    {
        return unsuccessfulLMIterationsAllowed;
    }

    /**
     * The number of unsuccessful iterations of Levenberg-Marquardt (iterations which fail to decrease the error
     * by the required threshold) before the algorithm will be considered terminated.
     * @param unsuccessfulLMIterationsAllowed
     */
    public void setUnsuccessfulLMIterationsAllowed(int unsuccessfulLMIterationsAllowed)
    {
        this.unsuccessfulLMIterationsAllowed = unsuccessfulLMIterationsAllowed;
    }
}
