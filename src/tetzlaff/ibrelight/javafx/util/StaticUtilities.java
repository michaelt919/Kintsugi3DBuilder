package tetzlaff.ibrelight.javafx.util;//Created by alexk on 7/20/2017.

import java.util.Objects;
import java.util.regex.Pattern;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/*
I general utilities class.
 */
public final class StaticUtilities
{
    private static final Pattern NUMERIC_REGEX = Pattern.compile("-?(0|([1-9]\\d{0,7}))?(\\.\\d*)?");

    private StaticUtilities()
    {
    }

    //    this method takes in a double property, and prevents it from reaching outside of its bound.
    public static <H extends Property<Number>> H wrapAround(double min, double max, H property)
    {
        property.addListener((observable, oldValue, newValue) ->
        {
            if (newValue != null && (newValue.doubleValue() < min || newValue.doubleValue() > max))
            {
                property.setValue(wrapAround(min, max, newValue.doubleValue()));
            }
        });
        return property;
    }

    public static <H extends Property<Number>> H clamp(double min, double max, H property)
    {
        property.addListener((observable, oldValue, newValue) ->
        {
            if (newValue != null && (newValue.doubleValue() < min || newValue.doubleValue() > max))
            {
                property.setValue(clamp(min, max, newValue.doubleValue()));
            }
        });
        return property;
    }


    public static void makeWrapAroundNumeric(double min, double max, TextField textField)
    {
        makeNumeric(textField);
        textField.focusedProperty().addListener((ob, o, n) ->
        {
            if (o && !n)
            {
                try
                {
                    double value = Double.valueOf(textField.getText());
                    textField.setText(Double.toString(wrapAround(min, max, value)));
//                    System.out.println("Set text to " + Double.toString(wrapAround(min, max, value)));
                }
                catch (NumberFormatException nfe)
                {
                    //do nothing
                }
            }
        });
    }

    public static void makeClampedNumeric(double min, double max, TextField textField)
    {
        makeNumeric(textField);
        textField.focusedProperty().addListener((ob, o, n) ->
        {
            if (o && !n)
            {
                try
                {
                    double value = Double.valueOf(textField.getText());
                    textField.setText(Double.toString(clamp(min, max, value)));
//                    System.out.println("Set text to " + Double.toString(wrapAround(min, max, value)));
                }
                catch (NumberFormatException nfe)
                {
                    //do nothing
                }
            }
        });
    }

    public static void makeNumeric(TextField textField)
    {
        textField.setTextFormatter(new TextFormatter<Double>(change ->
        {
            if (change.isDeleted() && !change.isReplaced())
            {
                return change;
            }
            String text = change.getControlNewText();
            if (text.isEmpty() || Objects.equals("-", text) || NUMERIC_REGEX.matcher(text).matches())
            {
                return change;
            }
            else
            {
                return null;
            }
        }));
    }

    public static void bindLogScaleToLinear(DoubleProperty logScaleProperty, DoubleProperty linearScaleProperty)
    {
        logScaleProperty.addListener((b, o, n) -> linearScaleProperty.set(Math.pow(10, n.doubleValue())));
        linearScaleProperty.addListener((b, o, n) -> logScaleProperty.set(Math.log10(n.doubleValue())));
    }

    private static double wrapAround(double min, double max, double value)
    {
        double diff = max - min;
        if (diff == 0)
        {
            return max;
        }
        //System.out.printf("[%f %f %f]", min, max, value);
        double sum = value;
        while (sum < min)
        {
            sum += diff;
        }
        while (sum > max)
        {
            sum -= diff;
        }
        //System.out.println();
        return sum;
    }

    private static double clamp(double min, double max, double value)
    {
        if (value < min)
        {
            return min;
        }
        else if (value > max)
        {
            return max;
        }
        else
        {
            return value;
        }
    }
}
