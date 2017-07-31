package kautzTesting;//Created by alexk on 7/19/2017.

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;

public class Main {
    public static void main(String[] args) {

        BooleanProperty startedFalse = new SimpleBooleanProperty(false);
        BooleanProperty startedTrue = new SimpleBooleanProperty(true);

        startedFalse.bindBidirectional(startedTrue);

        System.out.println("F .b T winner: " + startedTrue.getValue() + startedFalse.getValue());


    }


}
