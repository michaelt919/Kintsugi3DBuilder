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

        return Matrix4.rotateZ(polarCoordinatesRadians.z).times(
                Matrix4.rotateX(polarCoordinatesRadians.y).times(
                        Matrix4.rotateY(polarCoordinatesRadians.x)
                )
        );

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
                    ((float) Math.toDegrees(azimuth)),
                    ((float) Math.toDegrees(inclination)),
                    ((float) Math.toDegrees(twist))
            );


    }
}
