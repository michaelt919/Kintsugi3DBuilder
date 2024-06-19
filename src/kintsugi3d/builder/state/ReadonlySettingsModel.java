/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import kintsugi3d.builder.preferences.serialization.SettingsModelSerializer;

import java.util.Iterator;

@JsonSerialize(as = SettingsModel.class, using = SettingsModelSerializer.class)
public interface ReadonlySettingsModel
{
    interface Setting
    {
        String getName();
        Class<?> getType();
        Object getValue();
        boolean shouldSerialize();
    }

    Object getObject(String name);
    <T> T get(String name, Class<T> settingType);
    Class<?> getType(String name);
    boolean exists(String name);
    boolean shouldSerialize(String name);
    Iterator<Setting> iterator();

    default boolean existsForGet(String name, Class<?> settingType)
    {
        return exists(name) && settingType.isAssignableFrom(getType(name));
    }

    default boolean existsForSet(String name, Class<?> settingType)
    {
        return exists(name) && getType(name).isAssignableFrom(settingType);
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
