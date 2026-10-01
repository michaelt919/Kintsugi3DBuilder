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

package kintsugi3d.builder.core.viewset.events;

/**
 * Triggered when the luminance encoding / tone calibration changes.
 */
public class LuminanceEncodingChangedEvent
{
    /**
     * The reference linear luminance values used for decoding pixel colors.
     */
    private final double[] linearLuminanceValues;

    /**
     * The reference encoded luminance values used for decoding pixel colors.
     */
    private final byte[] encodedLuminanceValues;

    public LuminanceEncodingChangedEvent(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        if (linearLuminanceValues != null)
        {
            this.linearLuminanceValues = linearLuminanceValues.clone();
        }
        else
        {
            this.linearLuminanceValues = null;
        }

        if (encodedLuminanceValues != null)
        {
            this.encodedLuminanceValues = encodedLuminanceValues.clone();
        }
        else
        {
            this.encodedLuminanceValues = null;
        }
    }

    public double[] getLinearLuminanceValues()
    {
        return linearLuminanceValues.clone();
    }

    public byte[] getEncodedLuminanceValues()
    {
        return encodedLuminanceValues.clone();
    }
}
