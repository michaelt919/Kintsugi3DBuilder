/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.core.metrics;

/**
 * Bundles linear (physical radiance), sRGB / gamma-encoded, and encoded (tonemapped) RMSE
 */
public class ColorAppearanceRMSE
{
    private double normalizedLinear = Double.POSITIVE_INFINITY;
    private double normalizedSRGB = Double.POSITIVE_INFINITY;
    private double encodedGroundTruth = Double.POSITIVE_INFINITY;
    private long sampleCount = 0;

    public double getNormalizedLinear()
    {
        return normalizedLinear;
    }

    public void setNormalizedLinear(double normalizedLinear)
    {
        this.normalizedLinear = normalizedLinear;
    }

    public double getNormalizedSRGB()
    {
        return normalizedSRGB;
    }

    public void setNormalizedSRGB(double normalizedSRGB)
    {
        this.normalizedSRGB = normalizedSRGB;
    }

    public double getEncodedGroundTruth()
    {
        return encodedGroundTruth;
    }

    public void setEncodedGroundTruth(double encodedGroundTruth)
    {
        this.encodedGroundTruth = encodedGroundTruth;
    }

    public long getSampleCount()
    {
        return sampleCount;
    }

    public void setSampleCount(long sampleCount)
    {
        this.sampleCount = sampleCount;
    }
}
