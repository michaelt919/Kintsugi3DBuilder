package kautzTesting;//Created by alexk on 7/19/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public class Main {
    public static void main(String[] args) {
//        Matrix4 r1 = MoreMatrixMath.ZXYRotate(0.1, 0.2, 0.3);
//
//        double[] zxy = MoreMatrixMath.getZXY(r1);
//
//        MoreMatrixMath.round(zxy, 6);
//
//        //System.out.println(MoreMatrixMath.toString(r1));
//        System.out.println(MoreMatrixMath.toString(zxy));

//        DoubleProperty d1 = new SimpleDoubleProperty(5);
//        DoubleProperty d2 = new SimpleDoubleProperty(10);
//
//        DoubleProperty s = new SimpleDoubleProperty(-1);
//
//        s.bind(d1.multiply(d2));
//
//        d2.setValue(3);
//
//        System.out.println(s.getValue());
//
//        Property<Double> bob = new SimpleObjectProperty<>();

//        System.out.println(
//                //("44".matches("-?[0([1-9]\\d*)]?(\\.\\d*)?"))
//                ("44".matches("(0|([1-9]\\d*))"))
//        );

//        for (int i = 0; i < 100; i++) {
//            Vector3 vector3 = new Vector3((float) random360(), (float) random180(), (float) random360());
//            System.out.print("Before:\t");
//            System.out.print(MoreMatrixMath.toString(vector3));
//            System.out.print("After:\t");
//            System.out.print(MoreMatrixMath.toString(
//                    OrbitPolarConverter.self.convertRight(
//                            OrbitPolarConverter.self.convertLeft(
//                                    vector3
//                            )
//                    )
//            ));
//        }
//
//    }
//
//    private static double random180(){
//        return (Math.random()*180.0)-90.0;
//    }
//
//    private static double random360(){
//        return (Math.random()*360.0)-180.0;
//    }



        String s = MoreMatrixMath.toString(

                 Matrix4.lookAt(new Vector3(0,0,1),
                new Vector3(0,0,0),
                new Vector3(0,1,0))

        );

        System.out.println(s);

    }


}
