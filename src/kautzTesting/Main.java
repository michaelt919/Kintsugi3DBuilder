package kautzTesting;//Created by alexk on 7/19/2017.

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector4;

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

        System.out.println(
                //("44".matches("-?[0([1-9]\\d*)]?(\\.\\d*)?"))
                ("44".matches("(0|([1-9]\\d*))"))
        );

    }



}
