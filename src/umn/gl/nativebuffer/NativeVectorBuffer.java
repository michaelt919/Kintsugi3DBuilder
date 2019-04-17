/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.nativebuffer;

import java.nio.ByteBuffer;

/**
 * An interface for representing a buffer containing data intended to be passed to a graphics context memory buffer.
 * In order to work seamlessly with wrapped native libraries, this data must be stored in "native memory" rather than in traditional Java arrays.
 */
public interface NativeVectorBuffer
{
    /**
     * Gets the number of "dimensions", or components, in a single "element" within this buffer.
     * @return The number of dimensions per element.
     */
    int getDimensions();

    /**
     * The number of elements in the buffer.
     * @return
     */
    int getCount();

    /**
     * Gets a single component of a single element within the buffer.
     * @param index The index of the element to query.
     * @param dimension The dimension within the element to retrieve.
     * @return The specified component of the specified element.
     */
    Number get(int index, int dimension);

    /**
     * Sets a single component of a single element within the buffer.
     * @param index The index of the element to modify.
     * @param dimension The dimension within the element to set.
     * @param value The new value of the specified component of the specified element.
     */
    void set(int index, int dimension, Number value);

    /**
     * Gets the entire buffer as a native memory buffer.
     * @return A native memory buffer containing the data.
     */
    ByteBuffer getBuffer();

    /**
     * Gets the data type being represented in this buffer.
     * @return
     */
    NativeDataType getDataType();
}
