/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
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

public class SpecularBasisSettings
{
    private int basisCount = 8;
    private int basisResolution = 90;
    private double specularSmoothness = 0.0;
    private double metallicity = 0.0;

    private boolean smithMaskingShadowingEnabled = true;

    /**
     * @return The number of basis functions to use for the specular lobe.
     */
    public int getBasisCount()
    {
        return basisCount;
    }

    /**
     * @param basisCount The number of basis functions to use for the specular lobe.
     */
    public void setBasisCount(int basisCount)
    {
        if (basisCount <= 0)
        {
            throw new IllegalArgumentException("Basis count must be greater than zero.");
        }
        else
        {
            this.basisCount = basisCount;
        }
    }

    /**
     * @return The number of discrete values in the definition of the specular lobe.
     */
    public int getBasisResolution()
    {
        return basisResolution;
    }

    /**
     * @param basisResolution The number of discrete values in the definition of the specular lobe.
     */
    public void setBasisResolution(int basisResolution)
    {
        if (basisResolution <= 0)
        {
            throw new IllegalArgumentException("Basis resolution must be greater than zero.");
        }
        else
        {
            this.basisResolution = basisResolution;
        }
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
        return this.metallicity;
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
}