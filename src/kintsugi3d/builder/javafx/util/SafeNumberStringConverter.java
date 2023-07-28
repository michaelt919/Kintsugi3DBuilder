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

package kintsugi3d.builder.javafx.util;//Created by alexk on 7/23/2017.

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
