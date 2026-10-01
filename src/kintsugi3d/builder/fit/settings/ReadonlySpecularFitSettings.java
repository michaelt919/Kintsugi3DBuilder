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

import kintsugi3d.builder.core.texture.TextureResolution;
import kintsugi3d.builder.resources.project.ReadonlyImageCacheSettings;

import java.io.File;

public interface ReadonlySpecularFitSettings
{
    TextureResolution getTextureResolution();

    ReadonlyNormalOptimizationSettings getNormalOptimizationSettings();

    /**
     * Gets a modifiable reference to the image cache settings for the specular fit
     *
     * @return
     */
    ReadonlyImageCacheSettings getImageCacheSettings();

    ReadonlyReconstructionSettings getReconstructionSettings();

    ReadonlyExportSettings getExportSettings();

    /**
     * Gets the convergence tolerance used to determine whether the Levenberg-Marquardt algorithm for optimizing
     * the normal map has converged.
     *
     * @return
     */
    double getConvergenceTolerance();

    /**
     * Gets the convergence tolerance used to determine whether the Levenberg-Marquardt algorithm for optimizing
     * the normal map has converged.
     *
     * @return
     */
    double getPreliminaryConvergenceTolerance();

    /**
     * Whether or not to use height-correlated Smith for masking / shadowing.  Default is true.
     * @return
     */
    boolean isSmithMaskingShadowingEnabled();

    boolean shouldIncludeConstantTerm();

    /**
     * Gets the directory from which to load a prior solution
     *
     * @return
     */
    File getPriorSolutionDirectory();
}
