package tetzlaff.ibrelight.javafx.util;//Created by alexk on 7/23/2017.

import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;

public class SafeNumberStringConverter extends StringConverter<Number>
{
    private final NumberStringConverter base = new NumberStringConverter();
    private final Number defaultValue;

    public SafeNumberStringConverter(Number defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString(Number object)
    {
        return base.toString(object);
    }

    @Override
    public Number fromString(String string)
    {
        if (string != null && string.isEmpty())
        {
            return defaultValue;
        }
        try
        {
            return base.fromString(string);
        }
        catch (RuntimeException re)
        {
            return defaultValue;
        }
    }
}
