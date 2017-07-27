package tetzlaff.util;//Created by alexk on 7/23/2017.

import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;

public class SafeNumberStringConverter extends StringConverter<Number>{
    private final NumberStringConverter nsc = new NumberStringConverter();
    private final Number defaultValue;

    public SafeNumberStringConverter(Number defaultValue) {
        this.defaultValue = Math.log10(defaultValue.doubleValue());
    }

    /**
     * Converts the object provided into its string form.
     * Format of the returned string is defined by the specific converter.
     *
     * @param object
     * @return a string representation of the object passed in.
     */
    @Override
    public String toString(Number object) {
        return nsc.toString(object);
    }

    /**
     * Converts the string provided into an object defined by the specific converter.
     * Format of the string and type of the resulting object is defined by the specific converter.
     *
     * @param string
     * @return an object representation of the string passed in.
     */
    @Override
    public Number fromString(String string) {
        try {
            return nsc.fromString(string);
        } catch (RuntimeException re){
            return defaultValue;
        }
    }
}
