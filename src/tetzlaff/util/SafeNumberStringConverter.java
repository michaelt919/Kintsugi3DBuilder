package tetzlaff.util;//Created by alexk on 7/23/2017.

import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;

public class SafeNumberStringConverter extends StringConverter<Number>{
    private final NumberStringConverter nsc = new NumberStringConverter();
    private final Number defaultValue;

    public SafeNumberStringConverter(Number defaultValue) {
        this.defaultValue = defaultValue;
    }


    public String toString(Number object) {
        return nsc.toString(object);
    }

    @Override
    public Number fromString(String string) {
        if(string.equals(""))return defaultValue;
        try {
            return nsc.fromString(string);
        } catch (RuntimeException re){
            return defaultValue;
        }
    }
}
