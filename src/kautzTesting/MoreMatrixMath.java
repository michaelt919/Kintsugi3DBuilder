package kautzTesting;//Created by alexk on 7/19/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public class MoreMatrixMath {
    public static String toString(Matrix4 in){
        StringBuilder s = new StringBuilder();
        for (int r = 0; r < 4; r++) {
            s.append('[')
                    .append(in.get(r, 0)).append('\t')
                    .append(in.get(r, 1)).append('\t')
                    .append(in.get(r, 2)).append('\t')
                    .append(in.get(r, 3)).append(']')
                    .append('\n');
        }
        return s.toString();
    }
    public static Matrix4 ZXYRotate(double z, double x, double y){
        return Matrix4.rotateZ(z).times(
                Matrix4.rotateX(x).times(
                        Matrix4.rotateY(y)
                )
        );
    }
    public static double[] getZXY(Matrix4 orbit){
        double[] out = new double[3];

        double x = Math.asin(orbit.get(2,1));

        double cosX = Math.cos(x);

        double z = Math.acos( orbit.get(1,1) / cosX);

        double y = Math.acos( orbit.get(2,2) / cosX);

        out[0] = z;
        out[1] = x;
        out[2] = y;
        return out;
    }
    public static String toString(double[] in){
        StringBuilder s = new StringBuilder();
        s.append('[');
        for (int i = 0; i < in.length-1; i++) {
            s.append(in[i]).append(',');
        }
        s.append(in[in.length-1]).append(']').append('\n');
        return s.toString();
    }
    public static void round(double[] in, int places){
        for (int i = 0; i < in.length; i++) {
            double factor = Math.pow(10, places);
            in[i] = (Math.round(in[i]*factor))/factor;
        }
    }
    public static String toString(Vector3 in){
        StringBuilder s = new StringBuilder();
        s.append("[ ").append(in.x).append(", ").append(in.y).append(", ").append(in.z).append("]\n");
        return s.toString();
    }
}
