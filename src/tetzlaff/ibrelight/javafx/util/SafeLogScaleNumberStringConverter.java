package tetzlaff.ibrelight.javafx.util;//Created by alexk on 7/27/2017.

import javafx.util.StringConverter;

public class SafeLogScaleNumberStringConverter extends StringConverter<Number>
{
    private final SafeNumberStringConverter base;
    private final Number defaultValue;

    public SafeLogScaleNumberStringConverter(Number defaultValue)
    {
        this.defaultValue = Math.log10(defaultValue.doubleValue());
        base = new SafeNumberStringConverter(this.defaultValue);
    }

    @Override
    public String toString(Number object)
    {
        Double to10 = Math.pow(10, object.doubleValue());
        return base.toString(to10);
    }

    @Override
    public Number fromString(String string)
    {
        Number raw = base.fromString(string);
        if (raw.doubleValue() == 0.0)
        {
            return defaultValue;
        }
        return Math.log10(raw.doubleValue());
    }
}
