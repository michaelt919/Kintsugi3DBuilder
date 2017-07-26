package tetzlaff.ibr.gui2.other;//Created by alexk on 7/20/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public class OrbitPolarConverter implements Converter<Matrix4, Vector3>{
    public static final OrbitPolarConverter self = new OrbitPolarConverter();

    @Override
    public Matrix4 convertLeft(Vector3 from) {

        from = from.applyOperator(Math::toRadians);

        return Matrix4.rotateZ(from.z).times(
                Matrix4.rotateX(from.y).times(
                        Matrix4.rotateY(from.x)
                )
        );

    }
    @Override
    public Vector3 convertRight(Matrix4 from) {
            double azimuth;//all in radians until very end
            double inclination;
            double twist;

            inclination = Math.asin(from.get(2,1));

            double cosInc = Math.cos(inclination);

            double cosAzimuth = from.get(2,2) / cosInc;
            double sinAzimuth = -from.get(2,0) / cosInc;

            azimuth = Math.atan2(sinAzimuth, cosAzimuth);

            double cosTwist = from.get(1,1) / cosInc;
            double sinTwist = -from.get(0,1) / cosInc;

            twist = Math.atan2(sinTwist, cosTwist);


            return new Vector3(
                    ((float) Math.toDegrees(azimuth)),
                    ((float) Math.toDegrees(inclination)),
                    ((float) Math.toDegrees(twist))
            );


    }
}
