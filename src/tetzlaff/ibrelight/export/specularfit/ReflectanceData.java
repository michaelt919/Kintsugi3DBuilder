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

import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.util.ColorList;

/**
 * Class that maps output from fragment shader to the expected inputs to the fitting algorithm.
 */
public class ReflectanceData
{
    /**
     * Color and visibility components of the samples
     */
    private final ColorList colorAndVisibility;

    /**
     * Halfway angles, geometric factor, and additional weight for the samples.
     */
    private final ColorList halfwayGeomWeightNDotL;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public ReflectanceData(ColorList colorAndVisibility, ColorList halfwayGeomWeightNDotL)
    {
        this.colorAndVisibility = colorAndVisibility;
        this.halfwayGeomWeightNDotL = halfwayGeomWeightNDotL;
    }

    public Vector3 getColor(int p)
    {
        return colorAndVisibility.get(p).getXYZ();
    }

    public float getVisibility(int p)
    {
        return colorAndVisibility.get(p).w;
    }

    public float getHalfwayIndex(int p)
    {
        return halfwayGeomWeightNDotL.get(p).x;
    }

    public float getGeomRatio(int p)
    {
        return halfwayGeomWeightNDotL.get(p).y;
    }

    public float getAdditionalWeight(int p)
    {
        return halfwayGeomWeightNDotL.get(p).z;
    }

    public float getNDotL(int p)
    {
        return halfwayGeomWeightNDotL.get(p).w;
    }

    public int size()
    {
        return colorAndVisibility.size();
    }
}
