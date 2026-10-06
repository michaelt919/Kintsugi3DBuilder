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

package kintsugi3d.builder.test;

import kintsugi3d.builder.state.scene.BackgroundMode;
import kintsugi3d.builder.state.scene.ReadonlyLightPrototypeModel;
import kintsugi3d.builder.state.scene.ReadonlyLightWidgetModel;
import kintsugi3d.builder.state.scene.ReadonlyLightingEnvironmentModel;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

public class TestingLightingModel implements ReadonlyLightingEnvironmentModel
{
    @Override
    public ReadonlyLightWidgetModel getLightWidgetModel(int index)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLightCount()
    {
        return 0;
    }

    @Override
    public int getMaxLightCount()
    {
        return 0;
    }

    @Override
    public boolean isLightVisualizationEnabled(int index)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLightWidgetEnabled(int index)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean areLightWidgetsEthereal()
    {
        return false;
    }

    @Override
    public float getAmbientLightIntensity()
    {
        return 0;
    }

    @Override
    public Vector3 getAmbientLightColor()
    {
        return Vector3.ZERO;
    }

    @Override
    public boolean isEnvironmentMappingEnabled()
    {
        return false;
    }

    @Override
    public Matrix4 getEnvironmentMapMatrix()
    {
        return Matrix4.IDENTITY;
    }

    @Override
    public float getEnvironmentMapFilteringBias()
    {
        return 0;
    }

    @Override
    public ReadonlyLightPrototypeModel getLightPrototype(int i)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Matrix4 getLightMatrix(int i)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector3 getLightCenter(int i)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getBackgroundIntensity()
    {
        return 0;
    }

    @Override
    public Vector3 getBackgroundColor()
    {
        return Vector3.ZERO;
    }

    @Override
    public BackgroundMode getBackgroundMode()
    {
        return BackgroundMode.NONE;
    }

    @Override
    public Vector3 getGroundPlaneColor()
    {
        return Vector3.ZERO;
    }

    @Override
    public boolean isGroundPlaneEnabled()
    {
        return false;
    }

    @Override
    public float getGroundPlaneHeight()
    {
        return 0;
    }

    @Override
    public float getGroundPlaneSize()
    {
        return 0;
    }
}
