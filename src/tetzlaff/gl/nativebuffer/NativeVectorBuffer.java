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

package tetzlaff.gl.nativebuffer;

import java.nio.ByteBuffer;

/**
 * Abstract data type for representing a list of vectors to be packed in a buffer.
 * The internal format and precision of each vector is determined by the implementation.
 * Every vector in the list has the same number of dimensions, which is the number of elements associated with a single vector.
 * Typically, concrete instances of this interface are created using a NativeVectorBufferFactory.
 * @author Michael Tetzlaff
 *
 */
public interface NativeVectorBuffer
{
    /**
     * Gets the number of dimensions in each vector.
     * @return The number of dimensions.
     */
    int getDimensions();

    /**
     * Gets the number of elements in the buffer.
     * @return The number of elements.
     */
    int getCount();

    /**
     * Gets the value of a particular dimension of a particular vector element.
     * @param index The index of the element to retrieve.
     * @param dimension The dimension within the vector to retrieve.
     * @return The value of the dimension of the vector element.
     */
    Number get(int index, int dimension);

    /**
     * Sets the value of a particular dimension of a particular vector element.
     * @param index The index of the element to retrieve.
     * @param dimension The dimension within the vector to retrieve.
     * @param value The value to set the dimension of the vector to.
     */
    void set(int index, int dimension, Number value);

    /**
     * Gets the native buffer containing this vector buffer which can be used by the GL.
     * @return
     */
    ByteBuffer getBuffer();

    /**
     * Gets the underlying data type associated with each component of a vector in this buffer.
     * @return The data type
     */
    NativeDataType getDataType();
}
