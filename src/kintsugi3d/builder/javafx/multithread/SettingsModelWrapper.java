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

package kintsugi3d.builder.javafx.multithread;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javafx.beans.value.WritableValue;
import kintsugi3d.builder.javafx.util.MultithreadValue;
import kintsugi3d.builder.state.SettingsModel;
import kintsugi3d.builder.state.impl.SettingsModelBase;

public class SettingsModelWrapper extends SettingsModelBase
{
    private final Map<String, MultithreadValue<Object>> settings = new HashMap<>(32);
    private final SettingsModel baseModel;

    public SettingsModelWrapper(SettingsModel baseModel)
    {
        this.baseModel = baseModel;
    }

    private WritableValue<Object> initSetting(String name)
    {
        MultithreadValue<Object> multithreadValue =
            MultithreadValue.createFromFunctions(() -> baseModel.getObject(name),  value -> baseModel.set(name, value));
        settings.put(name, multithreadValue);
        return multithreadValue;
    }

    @Override
    protected Object getUnchecked(String name)
    {
        if (settings.containsKey(name))
        {
            return settings.get(name).getValue();
        }
        else
        {
            return initSetting(name).getValue();
        }
    }

    @Override
    protected void setUnchecked(String name, Object value)
    {
        if (settings.containsKey(name))
        {
            settings.get(name).setValue(value);
        }
        else
        {
            initSetting(name).setValue(value);
        }
    }

    @Override
    public Class<?> getType(String name)
    {
        return baseModel.getType(name);
    }

    @Override
    public boolean exists(String name)
    {
        return baseModel.exists(name);
    }

    @Override
    public boolean shouldSerialize(String name)
    {
        if (exists(name))
        {
            return baseModel.shouldSerialize(name);
        }

        return false;
    }

    protected Setting getSetting(String settingName)
    {
        return new Setting()
        {
            @Override
            public String getName()
            {
                return settingName;
            }

            @Override
            public Class<?> getType()
            {
                return SettingsModelWrapper.this.getType(settingName);
            }

            @Override
            public Object getValue()
            {
                return getObject(settingName);
            }

            @Override
            public boolean shouldSerialize()
            {
                return SettingsModelWrapper.this.shouldSerialize(settingName);
            }
        };
    }

    @Override
    public Iterator<Setting> iterator()
    {
        // Before creating iterator, init all settings in the base model
        for (Iterator<Setting> it = baseModel.iterator(); it.hasNext();)
        {
            initSetting(it.next().getName());
        }

        return new Iterator<Setting>()
        {
            private final Iterator<String> base = settings.keySet().iterator();

            @Override
            public boolean hasNext()
            {
                return base.hasNext();
            }

            @Override
            public Setting next()
            {
                return getSetting(base.next());
            }
        };
    }

    @Override
    public void createSetting(String name, Class<?> type, Object initialValue, boolean serialize)
    {
        baseModel.createSetting(name, type, initialValue, serialize);
        initSetting(name);
    }
}
