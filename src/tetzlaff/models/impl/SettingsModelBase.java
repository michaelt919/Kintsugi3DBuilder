/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.models.impl;

import java.util.NoSuchElementException;

import tetzlaff.models.SettingsModel;

public abstract class SettingsModelBase implements SettingsModel
{
    protected abstract Object getUnchecked(String name);
    protected abstract void setUnchecked(String name, Object value);

    @Override
    public Object getObject(String name)
    {
        if (this.exists(name))
        {
            return this.getUnchecked(name);
        }
        else
        {
            throw new NoSuchElementException("No setting called \"" + name + " exists");
        }
    }

    @Override
    public <T> T get(String name, Class<T> settingType)
    {
        if (this.exists(name))
        {
            Object value = this.getUnchecked(name);
            if (settingType.isInstance(value))
            {
                return (T) value;
            }
            else if (value == null && settingType.isAssignableFrom(this.getType(name)))
            {
                return null;
            }
        }

        throw new NoSuchElementException("No setting called \"" + name + " exists that can be cast to type " + settingType);
    }

    @Override
    public void set(String name, Object value)
    {
        if (this.exists(name))
        {
            if (value == null || this.getType(name).isInstance(value))
            {
                this.setUnchecked(name, value);
            }
            else
            {
                throw new NoSuchElementException("No setting called \"" + name + " exists that can be assigned from type " + value.getClass());
            }
        }
        else
        {
            throw new NoSuchElementException("No setting called \"" + name + " exists that can be assigned from type " + value.getClass());
        }
    }
}
