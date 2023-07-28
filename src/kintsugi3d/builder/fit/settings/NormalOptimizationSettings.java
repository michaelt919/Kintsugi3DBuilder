/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit.settings;

public class NormalOptimizationSettings
{
    private boolean normalRefinementEnabled = true;
    private double minNormalDamping = 1.0;
    private int normalSmoothingIterations = 0;
    private boolean levenbergMarquardtEnabled = true;
    private int unsuccessfulLMIterationsAllowed = 8;

    public NormalOptimizationSettings()
    {
    }

    /**
     * Gets whether normal refinement is enabled (if not, the vertex normals will be assumed to be accurate enough)
     *
     * @return
     */
    public boolean isNormalRefinementEnabled()
    {
        return normalRefinementEnabled;
    }

    /**
     * Sets whether normal refinement is enabled (if not, the vertex normals will be assumed to be accurate enough)
     *
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
     *
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
     *
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
     *
     * @return
     */
    public int getNormalSmoothingIterations()
    {
        return normalSmoothingIterations;
    }

    /**
     * Sets the number of smoothing iterations for the normal map.  Default is zero (no smoothing).
     * Negative values will have the same effect as 0.
     *
     * @param normalSmoothingIterations
     */
    public void setNormalSmoothingIterations(int normalSmoothingIterations)
    {
        // Negative values shouldn't break anything here.
        this.normalSmoothingIterations = normalSmoothingIterations;
    }

    /**
     * Whether or not to use Levenberg-Marquardt for normal optimization.
     * Default is true.  Highly recommended unless attempting to reproduce Nam et al. 2018.
     *
     * @return
     */
    public boolean isLevenbergMarquardtEnabled()
    {
        return levenbergMarquardtEnabled;
    }

    /**
     * Whether or not to use Levenberg-Marquardt for normal optimization.
     * Highly recommended unless attempting to reproduce Nam et al. 2018.
     *
     * @param levenbergMarquardtEnabled
     */
    public void setLevenbergMarquardtEnabled(boolean levenbergMarquardtEnabled)
    {
        this.levenbergMarquardtEnabled = levenbergMarquardtEnabled;
    }

    /**
     * The number of unsuccessful iterations of Levenberg-Marquardt (iterations which fail to decrease the error
     * by the required threshold) before the algorithm will be considered terminated.
     *
     * @return
     */
    public int getUnsuccessfulLMIterationsAllowed()
    {
        return unsuccessfulLMIterationsAllowed;
    }

    /**
     * The number of unsuccessful iterations of Levenberg-Marquardt (iterations which fail to decrease the error
     * by the required threshold) before the algorithm will be considered terminated.
     *
     * @param unsuccessfulLMIterationsAllowed
     */
    public void setUnsuccessfulLMIterationsAllowed(int unsuccessfulLMIterationsAllowed)
    {
        this.unsuccessfulLMIterationsAllowed = unsuccessfulLMIterationsAllowed;
    }
}