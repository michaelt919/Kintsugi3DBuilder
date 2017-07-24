package tetzlaff.ibr.util;//Created by alexk on 7/20/2017.

import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;

/*
I general utilities class.
 */
public class U {
    /*
    this method takes in a double property, and prevents it from reaching outside of its bound.
     */
    /*
    public static DoubleProperty bound(boolean modulus, double min, double max, DoubleProperty property){

       property.addListener((observable, oldValue, newValue) -> {
           if(newValue != null){
               if(newValue.doubleValue() < min){
                   if(modulus)property.setValue(
                           wrap(min, max, newValue.doubleValue()));
                   else property.setValue(min);
               }
               else if(newValue.doubleValue() > max){
                   if(modulus)property.setValue(
                           wrap(min, max, newValue.doubleValue()));
                   else property.setValue(max);
               }
           }
       });
       return property;
    }
    */

    private static final String DOUBLE_REG_EXP = "-?(0|([1-9]\\d*))?(\\.\\d*)?";

    public static TextField wrap(double min, double max, TextField textField){

        textField.setTextFormatter(new TextFormatter<Double>(change -> {
            if(change.isDeleted()) return change;
            String text = change.getControlNewText();
            if(text.isEmpty() || text.equals("-")) return change;
            if(
                      text.matches(DOUBLE_REG_EXP)//any double
                    ){
                double value = Double.valueOf(text);

                if(value < min | value > max){
                    //update text field and reject change
                    textField.setText(Double.toString(
                            wrap(min, max, value)
                    ));
                    textField.selectAll();
                    return null;
                }

                return change;
            }else return null;

        }));

        return textField;
    }

    public static TextField bound(double min, double max, TextField textField){

        textField.setTextFormatter(new TextFormatter<Double>(change -> {
            if(change.isDeleted()) return change;
            String text = change.getControlNewText();
            if(text.isEmpty() || text.equals("-")) return change;
            if(
                    text.matches(DOUBLE_REG_EXP)//any double
                    ){
                double value = Double.valueOf(text);

                if(value < min | value > max){
                    //update text field and reject change
                    textField.setText(Double.toString(
                            bound(min, max, value)
                    ));
                    textField.selectAll();
                    return null;
                }

                return change;
            }else return null;

        }));

        return textField;
    }


    private static double wrap(double min, double max, double value){
        double diff= max - min;
        if(diff == 0) return max;
        //System.out.printf("[%f %f %f]", min, max, value);
        if(value<min) return wrap(min, max, value+diff);
        else if(value>max) return wrap(min, max, value-diff);
        //System.out.println();
        return value;
    }

    private static double bound(double min, double max, double value){
        if(value < min) return min;
        else if(value > max) return max;
        else return value;
    }



    public static void powerBind(DoubleProperty d, DoubleProperty tenToD){

        d.addListener((b,o,n)-> tenToD.set(Math.pow(10, n.doubleValue())));
        tenToD.addListener((b,o,n)-> d.set(Math.log10(n.doubleValue())));

    }



}
