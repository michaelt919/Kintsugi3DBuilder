package tetzlaff.models;

import java.util.Iterator;

public interface ReadonlySettingsModel
{
    interface Setting
    {
        String getName();
        Class<?> getType();
        Object getValue();
    }

    Object getObject(String name);
    <T> T get(String name, Class<T> settingType);
    Class<?> getType(String name);
    boolean exists(String name);
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
