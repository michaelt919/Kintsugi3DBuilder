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

package kintsugi3d.builder.javafx.util;

import javafx.application.Platform;
import javafx.beans.value.WritableValue;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MultithreadValue<T> implements WritableValue<T>
{
    private T override;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final Object lock = new Object();

    public static <T> MultithreadValue<T> createFromFunctions(Supplier<T> getter, Consumer<T> setter)
    {
        return new MultithreadValue<>(getter, setter);
    }

    MultithreadValue(Supplier<T> getter, Consumer<T> setter)
    {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public T getValue()
    {
        synchronized(lock)
        {
            return override != null ? override : getter.get();
        }
    }

    @Override
    public void setValue(T value)
    {
        synchronized (lock)
        {
            override = value;
        }

        Platform.runLater(() ->
        {
            synchronized(lock)
            {
                if (override != null)
                {
                    try
                    {
                        setter.accept(override);
                    }
                    finally
                    {
                        // Clear the override regardless of whether the setter was successful.
                        // This will effectively reset it if the setter is not supported.
                        override = null;
                    }
                }
            }
        });
    }
}
