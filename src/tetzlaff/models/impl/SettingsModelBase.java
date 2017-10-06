package tetzlaff.models.impl;

import java.util.NoSuchElementException;

import tetzlaff.models.SettingsModel;

public abstract class SettingsModelBase implements SettingsModel
{
    protected abstract Object getUnchecked(String name);
    protected abstract void setUnchecked(String name, Object value);

    @Override
    public Object get(String name)
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
