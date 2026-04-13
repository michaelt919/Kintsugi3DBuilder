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

package kintsugi3d.builder.state.project;

public interface CameraSettings
{
    double getXCenter();

    void setXCenter(double xCenter);

    double getYCenter();

    void setYCenter(double yCenter);

    double getZCenter();

    void setZCenter(double zCenter);

    double getAzimuth();

    void setAzimuth(double azimuth);

    double getInclination();

    void setInclination(double inclination);

    double getLog10Distance();

    void setLog10Distance(double log10distance);

    double getTwist();

    void setTwist(double twist);

    double getFOV();

    void setFOV(double fOV);

    double getFocalLength();

    void setFocalLength(double focalLength);

    boolean isLocked();

    void setLocked(boolean locked);

    boolean isOrthographic();

    void setOrthographic(boolean orthographic);

    String getName();

    void setName(String name);
}
