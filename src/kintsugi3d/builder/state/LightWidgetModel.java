/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state;

public interface LightWidgetModel extends ReadonlyLightWidgetModel
{
    void setAzimuthWidgetVisible(boolean azimuthWidgetVisible);
    void setAzimuthWidgetSelected(boolean azimuthWidgetSelected);

    void setInclinationWidgetVisible(boolean inclinationWidgetVisible);
    void setInclinationWidgetSelected(boolean inclinationWidgetSelected);

    void setDistanceWidgetVisible(boolean distanceWidgetVisible);
    void setDistanceWidgetSelected(boolean distanceWidgetSelected);

    void setCenterWidgetVisible(boolean centerWidgetVisible);
    void setCenterWidgetSelected(boolean centerWidgetSelected);
}