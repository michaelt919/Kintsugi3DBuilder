/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.state;

public interface SafeReadonlySettingsModel
{
    <T> T get(String name, Class<T> settingType);

    default Object getObject(String name)
    {
        return get(name, Object.class);
    }

    default boolean getBoolean(String name)
    {
        return get(name, Boolean.class);
    }

    default byte getByte(String name)
    {
        return get(name, Number.class).byteValue();
    }

    default short getShort(String name)
    {
        return get(name, Number.class).shortValue();
    }

    default int getInt(String name)
    {
        return get(name, Number.class).intValue();
    }

    default long getLong(String name)
    {
        return get(name, Number.class).longValue();
    }

    default float getFloat(String name)
    {
        return get(name, Number.class).floatValue();
    }

    default double getDouble(String name)
    {
        return get(name, Number.class).doubleValue();
    }
}
