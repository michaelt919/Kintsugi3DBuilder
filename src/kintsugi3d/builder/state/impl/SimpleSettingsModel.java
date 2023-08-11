/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.state.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class SimpleSettingsModel extends SettingsModelBase
{
    private final Map<String, Setting> settingMap = new HashMap<>(32);

    @Override
    public Class<?> getType(String name)
    {
        if (exists(name))
        {
            return settingMap.get(name).getType();
        }

        throw new NoSuchElementException("No setting called \"" + name + " exists");
    }

    @Override
    public boolean exists(String name)
    {
        return settingMap.containsKey(name);
    }

    @Override
    public boolean shouldSerialize(String name)
    {
        if (exists(name))
        {
            return settingMap.get(name).shouldSerialize();
        }

        return false;
    }

    @Override
    public Iterator<Setting> iterator()
    {
        return settingMap.values().iterator();
    }

    @Override
    protected Object getUnchecked(String name)
    {
        return settingMap.get(name).getValue();
    }

    @Override
    protected void setUnchecked(String name, Object value)
    {
        Setting old = settingMap.get(name);
        settingMap.put(name, new Setting()
        {
            @Override
            public String getName()
            {
                return name;
            }

            @Override
            public Class<?> getType()
            {
                return value.getClass();
            }

            @Override
            public Object getValue()
            {
                return value;
            }

            @Override
            public boolean shouldSerialize()
            {
                return old.shouldSerialize();
            }
        });
    }

    //TODO: Maybe extract these create*Setting functions to SettingsModel? Or should components NOT have the ability to make new keys?
    public void createBooleanSetting(String name, boolean initialValue, boolean serialize)
    {
        createSetting(name, Boolean.class, initialValue, serialize);
    }

    public void createNumericSetting(String name, Number initialValue, boolean serialize)
    {
        createSetting(name, Number.class, initialValue, serialize);
    }

    public void createObjectSetting(String name, Object initialValue, boolean serialize)
    {
        createSetting(name, initialValue.getClass(), initialValue, serialize);
    }

    private void createSetting(String name, Class<?> type, Object initialValue, boolean serialize)
    {
        settingMap.put(name, new Setting()
        {
            @Override
            public String getName()
            {
                return name;
            }

            @Override
            public Class<?> getType()
            {
                return type;
            }

            @Override
            public Object getValue()
            {
                return initialValue;
            }

            @Override
            public boolean shouldSerialize()
            {
                return serialize;
            }
        });
    }
}
