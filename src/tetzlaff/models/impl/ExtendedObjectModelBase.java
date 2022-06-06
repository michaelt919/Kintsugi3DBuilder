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

package tetzlaff.models.impl;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.ExtendedObjectModel;
import tetzlaff.util.OrbitPolarConverter;

public abstract class ExtendedObjectModelBase implements ExtendedObjectModel
{
    @Override
    public Matrix4 getTransformationMatrix()
    {
        return getOrbit().times(Matrix4.translate(getCenter().negated()));
    }

    @Override
    public void setTransformationMatrix(Matrix4 transformationMatrix)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Matrix4 getOrbit()
    {
        // Intentionally swapping y and x components
        Vector3 polar = new Vector3(getRotationY(), getRotationX(), getRotationZ());
        return OrbitPolarConverter.getInstance().convertToOrbitMatrix(polar);
    }

    @Override
    public void setOrbit(Matrix4 orbit)
    {
        Vector3 polar = OrbitPolarConverter.getInstance().convertToPolarCoordinates(orbit);

        // Intentionally swapping y and x components
        setRotationY(polar.x);
        setRotationX(polar.y);
        setRotationZ(polar.z);
    }
}
