package tetzlaff.util;//Created by alexk on 7/27/2017.

import javafx.util.StringConverter;

public class SafeNumberStringConverterPow10 extends StringConverter<Number>{
    private final SafeNumberStringConverter safeConverter;
    private Number def;
    public SafeNumberStringConverterPow10(Number defaultValue) {
        def = Math.log10(defaultValue.doubleValue());
        safeConverter = new SafeNumberStringConverter(def);
    }

    @Override
    public String toString(Number object) {
        Double to10 = Math.pow(10, (object.doubleValue()));
        return safeConverter.toString(to10);
    }

    @Override
    public Number fromString(String string) {
        Number raw = safeConverter.fromString(string);
        if(raw.doubleValue() == 0.0) return def;
        return Math.log10(raw.doubleValue());
    }
}
