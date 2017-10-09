package tetzlaff.models;

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
