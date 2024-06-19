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

package kintsugi3d.builder.state.impl;

import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.builder.state.ExtendedCameraModel;
import kintsugi3d.util.OrbitPolarConverter;

public abstract class ExtendedCameraModelBase implements ExtendedCameraModel
{
    @Override
    public Matrix4 getLookMatrix()
    {
        return Matrix4.lookAt(
            new Vector3(0, 0, getDistance()),
            Vector3.ZERO,
            new Vector3(0, 1, 0)
        ).times(getOrbit().times(
            Matrix4.translate(getTarget().negated())
        ));
    }

    @Override
    public void setLookMatrix(Matrix4 lookMatrix)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Matrix4 getOrbit()
    {
        Vector3 polar = new Vector3(getAzimuth(), getInclination(), getTwist());
        return OrbitPolarConverter.getInstance().convertToOrbitMatrix(polar);
    }

    @Override
    public void setOrbit(Matrix4 orbit)
    {
        Vector3 polar = OrbitPolarConverter.getInstance().convertToPolarCoordinates(orbit);
        setAzimuth(polar.x);
        setInclination(polar.y);
        setTwist(polar.z);
    }

    @Override
    public float getDistance()
    {
        return (float) Math.pow(10, getLog10Distance());
    }

    @Override
    public void setDistance(float distance)
    {
        setLog10Distance((float)Math.log10(distance));
    }
}
