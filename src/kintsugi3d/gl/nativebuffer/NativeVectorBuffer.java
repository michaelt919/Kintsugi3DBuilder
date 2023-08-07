/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.gl.nativebuffer;

/**
 * Abstract data type for representing a list of vectors to be packed in a buffer.
 * The internal format and precision of each vector is determined by the implementation.
 * Every vector in the list has the same number of dimensions, which is the number of elements associated with a single vector.
 * Typically, concrete instances of this interface are created using a NativeVectorBufferFactory.
 * @author Michael Tetzlaff
 *
 */
public interface NativeVectorBuffer extends ReadonlyNativeVectorBuffer
{
    /**
     * Sets the value of a particular dimension of a particular vector element.
     * @param index The index of the element to retrieve.
     * @param dimension The dimension within the vector to retrieve.
     * @param value The value to set the dimension of the vector to.
     */
    void set(int index, int dimension, Number value);

}
