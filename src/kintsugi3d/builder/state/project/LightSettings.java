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

import javafx.scene.paint.Color;

public interface LightSettings
{
    boolean isGroupLocked();

    double getTargetX();

    double getTargetY();

    double getTargetZ();

    double getAzimuth();

    double getInclination();

    double getLog10Distance();

    double getIntensity();

    boolean isLocked();

    String getName();

    double getSpotSize();

    double getSpotTaper();

    Color getColor();

    void setTargetX(double value);

    void setTargetY(double value);

    void setTargetZ(double value);

    void setAzimuth(double value);

    void setInclination(double value);

    void setLog10Distance(double value);

    void setIntensity(double value);

    void setLocked(boolean value);

    void setName(String value);

    void setSpotSize(double value);

    void setSpotTaper(double value);

    void setColor(Color value);
}
