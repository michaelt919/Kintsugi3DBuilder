package tetzlaff.ibr.javafx.util;//Created by alexk on 8/3/2017.

import javafx.util.StringConverter;
import javafx.util.converter.FloatStringConverter;

public class SafeFloatStringConverter extends StringConverter<Number>{
    private final Float defaultValue;
    private FloatStringConverter fsc = new FloatStringConverter();

    public SafeFloatStringConverter(Float defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString(Number object) {
        return fsc.toString(object.floatValue());
    }

    @Override
    public Float fromString(String string) {
        if(string.equals("")) return defaultValue;
        try{
            return fsc.fromString(string);
        }catch (RuntimeException re){
            return defaultValue;
        }
    }
}
