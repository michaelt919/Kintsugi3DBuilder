package tetzlaff.ibr.util;//Created by alexk on 7/20/2017.

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/*
I general utilities class.
 */
public class StaticHouse {
//    this method takes in a double property, and prevents it from reaching outside of its bound.
    public static <H extends Property<Number>> H wrap(double min, double max, H property){
       property.addListener((observable, oldValue, newValue) -> {
          if(newValue != null && (newValue.doubleValue() < min || newValue.doubleValue() > max)){
              property.setValue(wrap(min, max, newValue.doubleValue()));
          }
       });
       return property;
    }

    public static <H extends Property<Number>> H bound(double min, double max, H property){
       property.addListener((observable, oldValue, newValue) -> {
          if(newValue != null && (newValue.doubleValue() < min || newValue.doubleValue() > max)){
              property.setValue(bound(min, max, newValue.doubleValue()));
          }
       });
       return property;
    }



    private static final String DOUBLE_REG_EXP = "-?(0|([1-9]\\d{0,7}))?(\\.\\d*)?";

    public static TextField wrap(double min, double max, TextField textField){
        cleanInput(textField);
        textField.focusedProperty().addListener((ob,o,n)->{
            if(o && !n){
                try {
                    double value = Double.valueOf(textField.getText());
                    textField.setText(Double.toString(wrap(min, max, value)));
//                    System.out.println("Set text to " + Double.toString(wrap(min, max, value)));
                }catch (NumberFormatException nfe){
                    //do nothing
                }
            }
        });

        return textField;
    }

    public static TextField bound(double min, double max, TextField textField){

        cleanInput(textField);

        textField.focusedProperty().addListener((ob,o,n)->{
            if(o && !n){
                try {
                    double value = Double.valueOf(textField.getText());
                    textField.setText(Double.toString(bound(min, max, value)));
//                    System.out.println("Set text to " + Double.toString(wrap(min, max, value)));
                }catch (NumberFormatException nfe){
                    //do nothing
                }
            }
        });

        return textField;
    }

    public static TextField cleanInput(TextField textField){
        textField.setTextFormatter(new TextFormatter<Double>(change -> {
            if(change.isDeleted() && !change.isReplaced()) return change;
            String text = change.getControlNewText();
            if(text.isEmpty() || text.equals("-") || text.matches(DOUBLE_REG_EXP)) return change;
            else return null;
        }));
        return textField;
    }


    private static double wrap(double min, double max, double value){
        double diff= max - min;
        if(diff == 0) return max;
        //System.out.printf("[%f %f %f]", min, max, value);
        while (value<min) value+=diff;
        while (value>max) value-=diff;
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

    public static void naturalClose(Window window){
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }


}
