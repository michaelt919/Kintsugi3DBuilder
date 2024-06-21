/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.types;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import kintsugi3d.gl.nativebuffer.NativeDataType;

/**
 * An interface that abstracts the concept of a data type in the graphics pipeline.
 * Typically an instance of this interface is created using the AbstractDataTypeFactory
 * @param <HighLevelType> The high-level type (typically Number for single-component or an Iterable of Numbers for multi-component)
 *                        that this data type can store.
 */
public interface AbstractDataType<HighLevelType>
{
    /**
     * Gets the type that will be used to store the components of this data type.
     * @return The native data type.
     */
    NativeDataType getNativeDataType();

    /**
     * Gets the number of components of this data type.
     * @return The number of components.
     */
    int getComponentCount();

    /**
     * Gets the size of this data type in bytes.
     * @return The size  in bytes.
     */
    int getSizeInBytes();

    /**
     * Creates a consumer function that will store a data element of this type in the specified buffer.
     * @param baseBuffer The buffer in which to store data.
     * @return A function which, when invoked on the high-level type associated with this data type  (typically Number single-component
     *         or an Iterable of Numbers for multi-component) will format and store the element in the previously specified buffer.
     */
    Consumer<HighLevelType> wrapByteBuffer(ByteBuffer baseBuffer);

    /**
     * Creates a consumer function that will store a data element of this type at a particular index in the specified buffer.
     * @param baseBuffer The buffer in which to store data.
     * @return A function which, when invoked on the high-level type associated with this data type  (typically Number single-component
     *         or an Iterable of Numbers for multi-component) will format and store the element in the previously specified buffer.
     */
    BiConsumer<Integer, HighLevelType> wrapIndexedByteBuffer(ByteBuffer baseBuffer);
}
