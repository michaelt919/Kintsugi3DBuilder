package tetzlaff.ibrelight.javafx.internal;//Created by alexk on 7/31/2017.

import java.util.*;
import java.util.Map.Entry;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import tetzlaff.models.impl.SettingsModelBase;

public class SettingsModelImpl extends SettingsModelBase
{
    private interface TypedProperty<T> extends Property<T>
    {
        Class<? extends T> getType();
    }

    private static class TypedPropertyGenericImpl<T> implements TypedProperty<T>
    {
        private final Property<T> base;
        private final Class<T> type;

        TypedPropertyGenericImpl(Class<T> type, Property<T> base)
        {
            this.base = base;
            this.type = type;
        }

        @Override
        public Class<T> getType()
        {
            return this.type;
        }

        @Override
        public void bind(ObservableValue<? extends T> observable)
        {
            base.bind(observable);
        }

        @Override
        public void unbind()
        {
            base.unbind();
        }

        @Override
        public boolean isBound()
        {
            return base.isBound();
        }

        @Override
        public void bindBidirectional(Property<T> other)
        {
            base.bindBidirectional(other);
        }

        @Override
        public void unbindBidirectional(Property<T> other)
        {
            base.unbindBidirectional(other);
        }

        @Override
        public Object getBean()
        {
            return base.getBean();
        }

        @Override
        public String getName()
        {
            return base.getName();
        }

        @Override
        public void addListener(ChangeListener<? super T> listener)
        {
            base.addListener(listener);
        }

        @Override
        public void removeListener(ChangeListener<? super T> listener)
        {
            base.removeListener(listener);
        }

        @Override
        public T getValue()
        {
            return base.getValue();
        }

        @Override
        public void addListener(InvalidationListener listener)
        {
            base.addListener(listener);
        }

        @Override
        public void removeListener(InvalidationListener listener)
        {
            base.removeListener(listener);
        }

        @Override
        public void setValue(T value)
        {
            base.setValue(value);
        }
    }

    private static class TypedPropertyNonGenericImpl implements TypedProperty<Object>
    {
        private final ObjectProperty<Object> base;
        private final Class<?> type;

        TypedPropertyNonGenericImpl(Class<?> type, Object initialValue)
        {
            this.base = new SimpleObjectProperty<>(initialValue);
            this.type = type;
        }

        TypedPropertyNonGenericImpl(Object initialValue)
        {
            this(initialValue.getClass(), initialValue);
        }

        @Override
        public Class<?> getType()
        {
            return this.type;
        }

        @Override
        public void bind(ObservableValue<?> observable)
        {
            base.bind(observable);
        }

        @Override
        public void unbind()
        {
            base.unbind();
        }

        @Override
        public boolean isBound()
        {
            return base.isBound();
        }

        @Override
        public void bindBidirectional(Property<Object> other)
        {
            base.bindBidirectional(other);
        }

        @Override
        public void unbindBidirectional(Property<Object> other)
        {
            base.unbindBidirectional(other);
        }

        @Override
        public Object getBean()
        {
            return base.getBean();
        }

        @Override
        public String getName()
        {
            return base.getName();
        }

        @Override
        public void addListener(ChangeListener<? super Object> listener)
        {
            base.addListener(listener);
        }

        @Override
        public void removeListener(ChangeListener<? super Object> listener)
        {
            base.removeListener(listener);
        }

        @Override
        public Object getValue()
        {
            return base.getValue();
        }

        @Override
        public void addListener(InvalidationListener listener)
        {
            base.addListener(listener);
        }

        @Override
        public void removeListener(InvalidationListener listener)
        {
            base.removeListener(listener);
        }

        @Override
        public void setValue(Object value)
        {
            base.setValue(value);
        }
    }

    private final Map<String, TypedProperty<?>> settingsMap = new HashMap<>(32);

    private static class SettingImpl implements Setting
    {
        private final Entry<String, TypedProperty<?>> nextEntry;

        SettingImpl(Entry<String, TypedProperty<?>> nextEntry)
        {
            this.nextEntry = nextEntry;
        }

        @Override
        public String getName()
        {
            return nextEntry.getKey();
        }

        @Override
        public Class<?> getType()
        {
            return nextEntry.getValue().getType();
        }

        @Override
        public Object getValue()
        {
            return nextEntry.getValue().getValue();
        }
    }

    @Override
    protected Object getUnchecked(String name)
    {
        return settingsMap.get(name).getValue();
    }

    @Override
    protected void setUnchecked(String name, Object value)
    {
        ((Property<Object>)settingsMap.get(name)).setValue(value);
    }

    @Override
    public Class<?> getType(String name)
    {
        if (this.exists(name))
        {
            return settingsMap.get(name).getType();
        }
        else
        {
            throw new NoSuchElementException("No setting called \"" + name + " exists");
        }
    }

    @Override
    public boolean exists(String name)
    {
        return settingsMap.containsKey(name);
    }

    @Override
    public Iterator<Setting> iterator()
    {
        return new Iterator<Setting>()
        {
            private final Iterator<Entry<String, TypedProperty<?>>> innerIterator = settingsMap.entrySet().iterator();

            @Override
            public boolean hasNext()
            {
                return innerIterator.hasNext();
            }

            @Override
            public Setting next()
            {
                Entry<String, TypedProperty<?>> nextEntry = innerIterator.next();

                return new SettingImpl(nextEntry);
            }
        };
    }

    public Property<Boolean> getBooleanProperty(String name)
    {
        return getObjectProperty(name, Boolean.class);
    }

    public Property<Number> getNumericProperty(String name)
    {
        return getObjectProperty(name, Number.class);
    }

    public <T> Property<T> getObjectProperty(String name, Class<T> settingType)
    {
        if (settingsMap.containsKey(name))
        {
            TypedProperty<?> entry = settingsMap.get(name);
            if (Objects.equals(settingType, entry.getType()))
            {
                return (Property<T>) entry;
            }
        }

        throw new NoSuchElementException("No setting called \"" + name + " exists of type " + settingType);
    }

    private void createObjectSettingInternal(String name, Class<?> settingType, Object initialValue)
    {
        if(settingsMap.containsKey(name))
        {
            throw new IllegalArgumentException("The setting to be created already exists.");
        }
        else
        {
            settingsMap.put(name, new TypedPropertyNonGenericImpl(settingType, initialValue));
        }
    }

    public void createBooleanSetting(String name, boolean initialValue)
    {
        createObjectSettingInternal(name, Boolean.class, initialValue);
    }

    public void createNumericSetting(String name, Number initialValue)
    {

        createObjectSettingInternal(name, Number.class, initialValue);
    }

    public void createObjectSetting(String name, Object initialValue)
    {
        createObjectSettingInternal(name, initialValue.getClass(), initialValue);
    }

    public <T> void createObjectSetting(String name, Class<T> settingType, T initialValue)
    {
        createObjectSettingInternal(name, initialValue.getClass(), initialValue);
    }

    public <T> void createSettingFromProperty(String name, Class<T> settingType, Property<T> property)
    {
        if (settingsMap.containsKey(name))
        {
            throw new IllegalArgumentException("The setting to be created already exists.");
        }
        else if (property == null)
        {
            throw new IllegalArgumentException("The parameter \"property\" may not be null.");
        }
        else
        {
            settingsMap.put(name, new TypedPropertyGenericImpl<>(settingType, property));
        }
    }
}