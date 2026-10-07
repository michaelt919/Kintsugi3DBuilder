/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit.settings;

public interface ReadonlyNormalOptimizationSettings
{
    /**
     * Gets whether normal refinement is enabled (if not, the vertex normals will be assumed to be accurate enough)
     *
     * @return
     */
    boolean isNormalRefinementEnabled();

    /**
     * Gets the minimum allowed damping factor for the Levenberg-Marquardt algorithm for optimizing the normal map.
     * Default is 1.0.
     * Negative values will have the same effect as 0.0.
     *
     * @return
     */
    double getMinNormalDamping();

    /**
     * Gets the number of smoothing iterations for the normal map.  Default is zero (no smoothing).
     * Negative values will have the same effect as 0.
     *
     * @return
     */
    int getNormalSmoothingIterations();

    /**
     * Whether or not to use Levenberg-Marquardt for normal optimization.
     * Default is true.  Highly recommended unless attempting to reproduce Nam et al. 2018.
     *
     * @return
     */
    boolean isLevenbergMarquardtEnabled();

    /**
     * The number of unsuccessful iterations of Levenberg-Marquardt (iterations which fail to decrease the error
     * by the required threshold) before the algorithm will be considered terminated.
     *
     * @return
     */
    int getUnsuccessfulLMIterationsAllowed();
}
