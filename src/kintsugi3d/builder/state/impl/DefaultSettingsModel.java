/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state.impl;

import java.util.Objects;

import kintsugi3d.builder.state.SafeReadonlySettingsModel;

public class DefaultSettingsModel implements SafeReadonlySettingsModel
{
    public static Object getDefault(Class<?> settingType)
    {
        if (Objects.equals(settingType, Boolean.class))
        {
            return Boolean.FALSE;
        }
        else if (Objects.equals(settingType, Byte.class))
        {
            return (byte) 0;
        }
        else if (Objects.equals(settingType, Short.class))
        {
            return (short) 0;
        }
        else if (Objects.equals(settingType, Integer.class))
        {
            return 0;
        }
        else if (Objects.equals(settingType, Long.class))
        {
            return 0L;
        }
        else if (Objects.equals(settingType, Float.class))
        {
            return 0.0f;
        }
        else if (Objects.equals(settingType, Double.class))
        {
            return 0.0;
        }
        else if (Objects.equals(settingType, Number.class))
        {
            return 0;
        }
        else
        {
            return null;
        }
    }

    @Override
    public <T> T get(String name, Class<T> settingType)
    {
        return (T)getDefault(settingType);
    }
}
