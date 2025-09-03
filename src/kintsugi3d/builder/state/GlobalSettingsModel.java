/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import kintsugi3d.builder.preferences.serialization.SettingsModelDeserializer;
import kintsugi3d.builder.preferences.serialization.SettingsModelSerializer;

@JsonSerialize(as = GlobalSettingsModel.class, using = SettingsModelSerializer.class)
@JsonDeserialize(as = GlobalSettingsModel.class, using = SettingsModelDeserializer.class)
public interface GlobalSettingsModel extends ReadonlyGlobalSettingsModel
{
    <T> void set(String name, T value);
    void copyFrom(ReadonlyGlobalSettingsModel other);
    void createSetting(String name, Class<?> type, Object initialValue, boolean serialize);
    default void createBooleanSetting(String name, boolean initialValue)
    {
        createBooleanSetting(name, initialValue, false);
    }

    default void createNumericSetting(String name, Number initialValue)
    {
        createNumericSetting(name, initialValue, false);
    }

    default void createObjectSetting(String name, Object initialValue)
    {
        createObjectSetting(name, initialValue, false);
    }

    default void createBooleanSetting(String name, boolean initialValue, boolean serialize)
    {
        createSetting(name, Boolean.class, initialValue, serialize);
    }

    default void createNumericSetting(String name, Number initialValue, boolean serialize)
    {
        createSetting(name, Number.class, initialValue, serialize);
    }

    default void createObjectSetting(String name, Object initialValue, boolean serialize)
    {
        createSetting(name, initialValue == null ? Object.class : initialValue.getClass(), initialValue, serialize);
    }
}
