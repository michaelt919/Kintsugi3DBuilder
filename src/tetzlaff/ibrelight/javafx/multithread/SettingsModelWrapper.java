package tetzlaff.ibrelight.javafx.multithread;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javafx.beans.value.WritableValue;
import tetzlaff.ibrelight.javafx.util.MultithreadValue;
import tetzlaff.models.SettingsModel;
import tetzlaff.models.impl.SettingsModelBase;

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
        };
    }

    @Override
    public Iterator<Setting> iterator()
    {
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
}
