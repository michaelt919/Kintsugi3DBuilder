package tetzlaff.ibr.javafx.util;//Created by alexk on 7/31/2017.

import javafx.util.StringConverter;

public class SafeIntegerStringConverter extends StringConverter<Integer>
{
    private final SafeNumberStringConverter snsc;

    public SafeIntegerStringConverter(Integer defaultValue)
    {
        snsc = new SafeNumberStringConverter(defaultValue);
    }

    @Override
    public String toString(Integer object)
    {
        return object.toString();
    }

    @Override
    public Integer fromString(String string)
    {
        return snsc.fromString(string).intValue();
    }
}
