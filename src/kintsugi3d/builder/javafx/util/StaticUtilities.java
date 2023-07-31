/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.javafx.util;//Created by alexk on 7/20/2017.

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
