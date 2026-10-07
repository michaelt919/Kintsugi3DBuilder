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

public interface ReadonlyBasisOptimizationSettings extends ReadonlyBasisSettings
{
    /**
     * The minimum width of the specular lobe when optimizing (the minimum width of the smoothstep function used).
     * This setting is measured in discrete (integer) units and should be less than the basis resolution and the max width.
     * @return
     */
    int getSpecularMinWidth();

    /**
     * Gets the required smoothness for the specular lobe (the maximum width of the smoothstep function used to optimize it).
     * This setting is measured in discrete (integer) units and should be less than the basis resolution.
     * @return
     */
    int getSpecularMaxWidth();

    /**
     * Gets the number of representative functions that are used to optimize each basis function.
     * @return
     */
    int getBasisComplexity();

    /**
     * Gets the assumed metallicity of the material (metallic meaning that the diffuse reflectance exhibits specular characteristics
     * like the Fresnel effect and is scattered by first-surface microfacet geometry, not subsurface scattering)
     * @return
     */
    double getMetallicity();
}
