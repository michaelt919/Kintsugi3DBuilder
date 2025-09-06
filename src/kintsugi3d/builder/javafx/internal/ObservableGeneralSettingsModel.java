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

package kintsugi3d.builder.javafx.internal;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import kintsugi3d.builder.javafx.util.SafeNumberStringConverter;
import kintsugi3d.builder.javafx.util.StaticUtilities;
import kintsugi3d.builder.state.GeneralSettingsModelBase;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

public class ObservableGeneralSettingsModel extends GeneralSettingsModelBase
{
    private static class TypedProperty implements Property<Object>
    {
        protected final ObjectProperty<Object> base;
        protected final Class<?> type;
        protected final boolean serialize;

        private TypedProperty(Object initialValue, Class<?> type, boolean serialize)
        {
            this.base = new SimpleObjectProperty<>(initialValue);
            this.type = type;
            this.serialize = serialize;
        }

        public Class<?> getType()
        {
            return this.type;
        }

        public boolean shouldSerialize()
        {
            return serialize;
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

    private final Map<String, TypedProperty> settingsMap = new HashMap<>(32);

    private static class ObservableSetting implements Setting
    {
        private final Entry<String, TypedProperty> nextEntry;

        ObservableSetting(Entry<String, TypedProperty> nextEntry)
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

        @Override
        public boolean shouldSerialize()
        {
            return nextEntry.getValue().shouldSerialize();
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
        settingsMap.get(name).setValue(value);
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
    public boolean shouldSerialize(String name)
    {
        if (exists(name))
        {
            return settingsMap.get(name).shouldSerialize();
        }

        return false;
    }

    @Override
    public Iterator<Setting> iterator()
    {
        return new Iterator<>()
        {
            private final Iterator<Entry<String, TypedProperty>> innerIterator = settingsMap.entrySet().iterator();

            @Override
            public boolean hasNext()
            {
                return innerIterator.hasNext();
            }

            @Override
            public Setting next()
            {
                Entry<String, TypedProperty> nextEntry = innerIterator.next();

                return new ObservableSetting(nextEntry);
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

    @SuppressWarnings("unchecked")
    public <T> Property<T> getObjectProperty(String name, Class<T> settingType)
    {
        if (settingsMap.containsKey(name))
        {
            TypedProperty entry = settingsMap.get(name);
            if (Objects.equals(settingType, entry.getType()))
            {
                return (Property<T>) entry;
            }
        }

        throw new NoSuchElementException("No setting called \"" + name + " exists of type " + settingType);
    }

    @Override
    public void createSetting(String name, Class<?> settingType, Object initialValue, boolean serialize)
    {
        if(settingsMap.containsKey(name))
        {
            // Set value to new initialValue if it already exists.
            // Note: this will not change whether serialize was set.
            TypedProperty entry = settingsMap.get(name);
            if (Objects.equals(settingType, entry.getType()))
            {
                setUnchecked(name, initialValue);
            }
        }
        else
        {
            settingsMap.put(name, new TypedProperty(initialValue, settingType, serialize));
        }
    }

    public void bindEpsilonSetting(TextField textField, String settingName)
    {
        StaticUtilities.makeClampedNumeric(0, 1, textField);
        SafeNumberStringConverter converter = new SafeNumberStringConverter(getFloat(settingName), "0.###E0");
        textField.setText(converter.toString(getFloat(settingName)));
        textField.textProperty().bindBidirectional(
            getNumericProperty(settingName),
            converter);
    }

    public void bindNormalizedSetting(TextField textField, String settingName)
    {
        StaticUtilities.makeClampedNumeric(0, 1, textField);
        SafeNumberStringConverter converter = new SafeNumberStringConverter(getFloat(settingName));
        textField.setText(converter.toString(getFloat(settingName)));
        textField.textProperty().bindBidirectional(
            getNumericProperty(settingName),
            converter);
    }

    public void bindNonNegativeIntegerSetting(TextField textField, String settingName, int maxValue)
    {
        StaticUtilities.makeClampedInteger(0, maxValue, textField);
        SafeNumberStringConverter converter = new SafeNumberStringConverter(getInt(settingName));
        textField.setText(converter.toString(getInt(settingName)));
        textField.textProperty().bindBidirectional(
            getNumericProperty(settingName),
            converter);
    }

    public void bindBooleanSetting(CheckBox checkBox, String settingName)
    {
        checkBox.setSelected(getBoolean(settingName));
        checkBox.selectedProperty().bindBidirectional(
            getBooleanProperty(settingName));
    }

    public <T> void bindNumericComboBox(ComboBox<T> comboBox, String settingName,
        Function<Number, T> choiceConstructor, Function<T, Number> extractNumeric)
    {
        // Manually bind resolution both ways as a combo box.
        comboBox.valueProperty().addListener(
            (obs, oldValue, newValue) ->
                set(settingName, extractNumeric.apply(newValue)));
        getNumericProperty(settingName).addListener(
            (obs, oldValue, newValue) ->
                comboBox.setValue(choiceConstructor.apply(newValue)));
        comboBox.setValue(choiceConstructor.apply(get(settingName, Number.class)));
    }

    public <T> void bindTextComboBox(ComboBox<T> comboBox, String settingName,
        Function<String, T> choiceConstructor, Function<T, String> extractText)
    {
        // Manually bind resolution both ways as a combo box.
        comboBox.valueProperty().addListener(
            (obs, oldValue, newValue) ->
                set(settingName, extractText.apply(newValue)));
        getObjectProperty(settingName, String.class).addListener(
            (obs, oldValue, newValue) ->
                comboBox.setValue(choiceConstructor.apply(newValue)));
        comboBox.setValue(choiceConstructor.apply(settingName));
    }

    public void bindTextComboBox(ComboBox<String> comboBox, String settingName)
    {
        bindTextComboBox(comboBox, settingName, Function.identity(), Function.identity());
    }
}