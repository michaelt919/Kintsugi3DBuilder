package tetzlaff.util;//Created by alexk on 7/27/2017.

import javafx.util.StringConverter;

public class SafeNumberStringConverterPow10 extends StringConverter<Number>{
    private final SafeNumberStringConverter safeConverter;

    public SafeNumberStringConverterPow10(Number defaultValue) {
        safeConverter = new SafeNumberStringConverter(defaultValue);
    }

    @Override
    public String toString(Number object) {
        Double to10 = Math2.pow10(object.doubleValue());
        return safeConverter.toString(to10);
    }

    @Override
    public Number fromString(String string) {
        Number raw = safeConverter.fromString(string);
        return Math.log10(raw.doubleValue());
    }
}
