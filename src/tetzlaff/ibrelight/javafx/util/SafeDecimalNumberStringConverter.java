package tetzlaff.ibrelight.javafx.util;//Created by alexk on 8/3/2017.

import javafx.util.StringConverter;
import javafx.util.converter.FloatStringConverter;

public class SafeDecimalNumberStringConverter extends StringConverter<Number>
{
    private final Float defaultValue;
    private final FloatStringConverter fsc = new FloatStringConverter();

    public SafeDecimalNumberStringConverter(Float defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString(Number object)
    {
        return fsc.toString(object.floatValue());
    }

    @Override
    public Float fromString(String string)
    {
        if ("".equals(string))
        {
            return defaultValue;
        }
        try
        {
            return fsc.fromString(string);
        }
        catch (RuntimeException re)
        {
            return defaultValue;
        }
    }
}