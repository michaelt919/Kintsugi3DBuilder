/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.util;

import kintsugi3d.gl.vecmath.DoubleVector3;
import kintsugi3d.gl.vecmath.Vector3;

public final class SRGB
{
    private SRGB()
    {
    }

    /**
     * Converts sRGB to linear
     * @param sRGBColor
     * @return
     */
    public static DoubleVector3 toLinear(DoubleVector3 sRGBColor)
    {
        return sRGBColor.applyOperator(x ->
        {
            if(x <= 0.04045)
            {
                return x / 12.92;
            }
            else
            {
                return Math.pow((x + 0.055) / 1.055, 2.4);
            }
        });
    }

    /**
     * Converts sRGB to linear
     * @param sRGBColor
     * @return
     */
    public static Vector3 toLinear(Vector3 sRGBColor)
    {
        return toLinear(sRGBColor.asDoublePrecision()).asSinglePrecision();
    }


    /**
     * Converts linear to sRGB
     * @param linearColor
     * @return
     */
    public static DoubleVector3 fromLinear(DoubleVector3 linearColor)
    {
        return linearColor.applyOperator(x ->
        {
            if(x <= 0.0031308)
            {
                return 12.92 * x;
            }
            else
            {
                return 1.055 * Math.pow(x, 1.0/2.4) - 0.055;
            }
        });
    }

    /**
     * Converts linear to sRGB
     * @param linearColor
     * @return
     */
    public static Vector3 fromLinear(Vector3 linearColor)
    {
        return fromLinear(linearColor.asDoublePrecision()).asSinglePrecision();
    }

    public static double luminanceFromLinear(DoubleVector3 linearColor)
    {
        return linearColor.dot(new DoubleVector3(0.2126729, 0.7151522, 0.0721750));
    }

    public static float luminanceFromLinear(Vector3 linearColor)
    {
        return (float)luminanceFromLinear(linearColor.asDoublePrecision());
    }

    public static double luminanceFromSRGB(DoubleVector3 sRGBColor)
    {
        return luminanceFromLinear(toLinear(sRGBColor));
    }

    public static float luminanceFromSRGB(Vector3 sRGBColor)
    {
        return luminanceFromLinear(toLinear(sRGBColor));
    }
}
