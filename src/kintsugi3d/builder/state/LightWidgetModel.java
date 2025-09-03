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

package kintsugi3d.builder.state;

public class LightWidgetModel implements ReadonlyLightWidgetModel
{
    private boolean azimuthWidgetVisible = true;
    private boolean azimuthWidgetSelected = false;
    private boolean inclinationWidgetVisible = true;
    private boolean inclinationWidgetSelected = false;
    private boolean distanceWidgetVisible = true;
    private boolean distanceWidgetSelected = false;
    private boolean centerWidgetVisible = true;
    private boolean centerWidgetSelected = false;

    @Override
    public boolean isAzimuthWidgetVisible()
    {
        return azimuthWidgetVisible;
    }

    @Override
    public boolean isAzimuthWidgetSelected()
    {
        return azimuthWidgetSelected;
    }

    @Override
    public boolean isInclinationWidgetVisible()
    {
        return inclinationWidgetVisible;
    }

    @Override
    public boolean isInclinationWidgetSelected()
    {
        return inclinationWidgetSelected;
    }

    @Override
    public boolean isDistanceWidgetVisible()
    {
        return distanceWidgetVisible;
    }

    @Override
    public boolean isDistanceWidgetSelected()
    {
        return distanceWidgetSelected;
    }

    @Override
    public boolean isCenterWidgetVisible()
    {
        return centerWidgetVisible;
    }

    @Override
    public boolean isCenterWidgetSelected()
    {
        return centerWidgetSelected;
    }

    public void setAzimuthWidgetVisible(boolean azimuthWidgetVisible)
    {
        this.azimuthWidgetVisible = azimuthWidgetVisible;
    }

    public void setAzimuthWidgetSelected(boolean azimuthWidgetSelected)
    {
        this.azimuthWidgetSelected = azimuthWidgetSelected;
    }

    public void setInclinationWidgetVisible(boolean inclinationWidgetVisible)
    {
        this.inclinationWidgetVisible = inclinationWidgetVisible;
    }

    public void setInclinationWidgetSelected(boolean inclinationWidgetSelected)
    {
        this.inclinationWidgetSelected = inclinationWidgetSelected;
    }

    public void setDistanceWidgetVisible(boolean distanceWidgetVisible)
    {
        this.distanceWidgetVisible = distanceWidgetVisible;
    }

    public void setDistanceWidgetSelected(boolean distanceWidgetSelected)
    {
        this.distanceWidgetSelected = distanceWidgetSelected;
    }

    public void setCenterWidgetVisible(boolean centerWidgetVisible)
    {
        this.centerWidgetVisible = centerWidgetVisible;
    }

    public void setCenterWidgetSelected(boolean centerWidgetSelected)
    {
        this.centerWidgetSelected = centerWidgetSelected;
    }
}
