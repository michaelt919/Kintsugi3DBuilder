/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.models;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public interface ReadonlyLightingModel 
{
    ReadonlyLightWidgetModel getLightWidgetModel(int index);

    int getLightCount();
    boolean isLightVisualizationEnabled(int index);
    boolean isLightWidgetEnabled(int index);
    boolean areLightWidgetsEthereal();

    Vector3 getAmbientLightColor();
    boolean isEnvironmentMappingEnabled();
    Matrix4 getEnvironmentMapMatrix();
    float getEnvironmentMapFilteringBias();

    ReadonlyLightPrototypeModel getLightPrototype(int i);
    Matrix4 getLightMatrix(int i);
    Vector3 getLightCenter(int i);

    Vector3 getBackgroundColor();
    BackgroundMode getBackgroundMode();
}
