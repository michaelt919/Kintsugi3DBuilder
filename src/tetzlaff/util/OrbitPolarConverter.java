/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.util;//Created by alexk on 7/20/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public class OrbitPolarConverter 
{
    private static final OrbitPolarConverter instance = new OrbitPolarConverter();
    
    public static OrbitPolarConverter getInstance()
    {
        return instance;
    }

    public Matrix4 convertToOrbitMatrix(Vector3 polarCoordinatesDegrees)
    {
        Vector3 polarCoordinatesRadians = polarCoordinatesDegrees.applyOperator(Math::toRadians);

        return Matrix4.rotateZ(polarCoordinatesRadians.z)
                .times(Matrix4.rotateX(polarCoordinatesRadians.y)
                .times(Matrix4.rotateY(polarCoordinatesRadians.x)));
    }
    
    public Vector3 convertToPolarCoordinates(Matrix4 orbitMatrix) 
    {
            double azimuth;//all in radians until very end
            double inclination;
            double twist;

            inclination = Math.asin(orbitMatrix.get(2,1));

            double cosInc = Math.cos(inclination);

            double cosAzimuth = orbitMatrix.get(2,2) / cosInc;
            double sinAzimuth = -orbitMatrix.get(2,0) / cosInc;

            azimuth = Math.atan2(sinAzimuth, cosAzimuth);

            double cosTwist = orbitMatrix.get(1,1) / cosInc;
            double sinTwist = -orbitMatrix.get(0,1) / cosInc;

            twist = Math.atan2(sinTwist, cosTwist);


            return new Vector3(
                (float) Math.toDegrees(azimuth),
                (float) Math.toDegrees(inclination),
                (float) Math.toDegrees(twist)
            );


    }
}
