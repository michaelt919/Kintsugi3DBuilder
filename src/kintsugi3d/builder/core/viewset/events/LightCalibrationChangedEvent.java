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

import kintsugi3d.gl.vecmath.Vector3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Triggered when the light calibration data (intensity and position offset) changes.
 */
public class LightCalibrationChangedEvent
{
    private final List<Vector3> lightPositionList;
    private final List<Vector3> lightIntensityList;

    public LightCalibrationChangedEvent(List<Vector3> lightPositionList, List<Vector3> lightIntensityList)
    {
        this.lightPositionList = lightPositionList == null ? null : new ArrayList<>(lightPositionList);
        this.lightIntensityList = lightIntensityList == null ? null : new ArrayList<>(lightIntensityList);
    }

    public boolean wereLightPositionsChanged()
    {
        return lightPositionList != null;
    }

    public boolean wereLightIntensitiesChanged()
    {
        return lightIntensityList != null;
    }

    /**
     * A list of light source positions, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * Assumed by convention to be in camera space.
     * This list can be much smaller than the number of views if the same illumination conditions apply for multiple views.
     * This array may be added to but should never be removed from as indices are expected to be persistent.
     * @return null if light positions were not modified.
     */
    public List<Vector3> getLightPositionList()
    {
        return Collections.unmodifiableList(lightPositionList);
    }

    /**
     * A list of light source intensities, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * This list can be much smaller than the number of views if the same illumination conditions apply for multiple views.
     * This array may be added to but should never be removed from as indices are expected to be persistent.
     * @return null if light intensities were not modified.
     */
    public List<Vector3> getLightIntensityList()
    {
        return Collections.unmodifiableList(lightIntensityList);
    }
}
