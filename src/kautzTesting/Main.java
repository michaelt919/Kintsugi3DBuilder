package kautzTesting;//Created by alexk on 7/19/2017.


import javafx.stage.Window;

public class Main {
    public static void main(String[] args) {

        System.out.println("Double max:\t" + Double.MAX_VALUE);
        System.out.println("Float max:\t" + Float.MAX_VALUE);
        System.out.println("Float min:\t" + Float.MIN_VALUE);
        System.out.println("Double min:\t" + Double.MIN_VALUE);

        Double big = 7.0*Math.pow(10.0,50.0);

        System.out.println("Big double value: " + big.doubleValue());
        System.out.println("Big float value: " + big.floatValue());
        System.out.println("Big int value: " + big.intValue());

        Boolean bob = true;

        House house = new House();

        int i;
        char c = 'g';
        try {
            i = house.chartoint(c);
        } catch (Exception e) {
            i = -1;
        }

        System.out.println("i: " + i);

    }





}
