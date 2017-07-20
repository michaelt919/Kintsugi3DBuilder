package kautzTesting;//Created by alexk on 7/19/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector4;

public class Main {
    public static void main(String[] args) {
        Matrix4 r1 = MoreMatrixMath.ZXYRotate(0.1, 0.2, 0.3);

        double[] zxy = MoreMatrixMath.getZXY(r1);

        MoreMatrixMath.round(zxy, 6);

        //System.out.println(MoreMatrixMath.toString(r1));
        System.out.println(MoreMatrixMath.toString(zxy));

    }
}
