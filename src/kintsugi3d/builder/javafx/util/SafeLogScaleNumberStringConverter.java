/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.util;//Created by alexk on 7/27/2017.

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
