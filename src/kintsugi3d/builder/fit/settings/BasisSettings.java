/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit.settings;

public class BasisSettings
{
    private int basisCount = 8;
    private int basisResolution = 90;
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
