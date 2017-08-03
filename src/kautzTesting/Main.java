package kautzTesting;//Created by alexk on 7/19/2017.


import javafx.stage.Window;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.FloatStringConverter;
import javafx.util.converter.NumberStringConverter;

public class Main {
    public static void main(String[] args) {

        NumberStringConverter nsc = new NumberStringConverter();
        DoubleStringConverter dsc = new DoubleStringConverter();
        FloatStringConverter  fsc = new FloatStringConverter();

        System.out.println(nsc.toString(0.0025));
        System.out.println(dsc.toString(0.0025));
        System.out.println(fsc.toString(0.0025f));

        System.out.println(fsc.fromString(""));

    }





}
