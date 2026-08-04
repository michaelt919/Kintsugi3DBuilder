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

package kintsugi3d.builder.fit.decomposition;

import kintsugi3d.gl.vecmath.DoubleVector3;

public class BasisData
{
    private DoubleVector3 diffuseColor;
    private double[] redBasis;
    private double[] greenBasis;
    private double[] blueBasis;
    private int name;
    private boolean enabled;

    public BasisData()
    {
        this.diffuseColor = null;
        this.redBasis = null;
        this.greenBasis = null;
        this.blueBasis = null;
        this.name = -1;
        this.enabled = true;
    }

    public BasisData(DoubleVector3 diffuseColor, double[] redBasis, double[] greenBasis, double[] blueBasis,
                     int name, boolean enabled)
    {
        this.diffuseColor = diffuseColor;
        this.redBasis = redBasis;
        this.greenBasis = greenBasis;
        this.blueBasis = blueBasis;
        this.name = name;
        this.enabled = enabled;
    }

    public DoubleVector3 getDiffuseColor()
    {
        return diffuseColor;
    }

    public void setDiffuseColor(DoubleVector3 diffuseColor)
    {
        this.diffuseColor = diffuseColor;
    }

    public double[] getRedBasis()
    {
        return this.redBasis;
    }

    public void setRedBasis(double[] redBasis)
    {
        this.redBasis = redBasis;
    }

    public double[] getGreenBasis()
    {
        return greenBasis;
    }

    public void setGreenBasis(double[] greenBasis)
    {
        this.greenBasis = greenBasis;
    }

    public double[] getBlueBasis()
    {
        return blueBasis;
    }

    public void setBlueBasis(double[] blueBasis)
    {
        this.blueBasis = blueBasis;
    }

    public int getName()
    {
        return this.name;
    }

    public void setName(int name)
    {
        this.name = name;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        }
}
