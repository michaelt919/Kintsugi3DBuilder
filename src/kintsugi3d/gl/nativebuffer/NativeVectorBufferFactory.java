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

import java.nio.ByteBuffer;

/**
 * A singleton factory object for creating NativeVectorBuffer instances.
 */
public final class NativeVectorBufferFactory 
{
    private static final NativeVectorBufferFactory INSTANCE = new NativeVectorBufferFactory();

    /**
     * Gets the singleton instance.
     * @return The singleton instance.
     */
    public static NativeVectorBufferFactory getInstance()
    {
        return INSTANCE;
    }

    /**
     * Creates a new empty buffer with the specified number of dimensions and elements.
     * @param dataType The underlying type of the data in the buffer.
     * @param dimensions The number of dimensions in each vector element.
     * @param count The number of vector elements in the list.
     */
    public NativeVectorBuffer createEmpty(NativeDataType dataType, int dimensions, int count)
    {
        switch(dataType)
        {
        case BYTE: return new NativeByteVectorBuffer(dimensions, count);
        case UNSIGNED_BYTE: return new NativeUnsignedByteVectorBuffer(dimensions, count);
        case SHORT: return new NativeShortVectorBuffer(dimensions, count);
        case UNSIGNED_SHORT: return new NativeUnsignedShortVectorBuffer(dimensions, count);
        case INT: return new NativeIntVectorBuffer(dimensions, count);
        case UNSIGNED_INT: return new NativeUnsignedIntVectorBuffer(dimensions, count);
        case FLOAT: return new NativeFloatVectorBuffer(dimensions, count);
        case DOUBLE: return new NativeDoubleVectorBuffer(dimensions, count);
        default: throw new IllegalArgumentException("Unrecognized data type " + dataType + '.');
        }
    }

    /**
     * Creates a new vector buffer with the specified number of dimensions and elements, using a pre-existing buffer as storage.
     * Any data already in the buffer will persist.
     * A runtime exception will be thrown if the buffer is not big enough.
     * @param dataType The underlying type of the data in the buffer.
     * @param dimensions The number of dimensions in each vector element.
     * @param count The number of elements in the buffer.
     * @param buffer The buffer to use as storage.
     */
    public ReadonlyNativeVectorBuffer createFromExistingBuffer(NativeDataType dataType, int dimensions, int count, ByteBuffer buffer)
    {
        switch(dataType)
        {
        case BYTE: return new NativeByteVectorBuffer(dimensions, count, buffer);
        case UNSIGNED_BYTE: return new NativeUnsignedByteVectorBuffer(dimensions, count, buffer);
        case SHORT: return new NativeShortVectorBuffer(dimensions, count, buffer);
        case UNSIGNED_SHORT: return new NativeUnsignedShortVectorBuffer(dimensions, count, buffer);
        case INT: return new NativeIntVectorBuffer(dimensions, count, buffer);
        case UNSIGNED_INT: return new NativeUnsignedIntVectorBuffer(dimensions, count, buffer);
        case FLOAT: return new NativeFloatVectorBuffer(dimensions, count, buffer);
        case DOUBLE: return new NativeDoubleVectorBuffer(dimensions, count, buffer);
        default: throw new IllegalArgumentException("Unrecognized data type " + dataType + '.');
        }
    }

    /**
     * Creates a new vector buffer with the specified number of dimensions and elements, and an array of initial values to write to the buffer.
     * @param dataType The underlying type of the data in the buffer.
     * @param dimensions The number of dimensions in each vector element.
     * @param count The number of vector elements in the buffer.
     * @param byteArray The elements to use to initialize the vector buffer.
     */
    public ReadonlyNativeVectorBuffer createFromByteArray(NativeDataType dataType, int dimensions, int count, byte... byteArray)
    {
        switch(dataType)
        {
        case BYTE: return new NativeByteVectorBuffer(dimensions, count, byteArray);
        case UNSIGNED_BYTE: return new NativeUnsignedByteVectorBuffer(dimensions, count, byteArray);
        case SHORT: return new NativeShortVectorBuffer(dimensions, count, byteArray);
        case UNSIGNED_SHORT: return new NativeUnsignedShortVectorBuffer(dimensions, count, byteArray);
        case INT: return new NativeIntVectorBuffer(dimensions, count, byteArray);
        case UNSIGNED_INT: return new NativeUnsignedIntVectorBuffer(dimensions, count, byteArray);
        case FLOAT: return new NativeFloatVectorBuffer(dimensions, count, byteArray);
        case DOUBLE: return new NativeDoubleVectorBuffer(dimensions, count, byteArray);
        default: throw new IllegalArgumentException("Unrecognized data type " + dataType + '.');
        }
    }

    /**
     * Creates a new vector buffer with the specified number of dimensions and elements, and an array of initial values to write to the buffer.
     * Each dimension of every vector is to be represented as an 16-bit integer.
     * @param unsigned True if the elements in the buffer should be interpreted as unsigned; false otherwise.
     * @param dimensions The number of dimensions in each vector element.
     * @param count The number of vector elements in the buffer.
     * @param shortArray The elements to use to initialize the vector buffer.
     */
    public ReadonlyNativeVectorBuffer createFromShortArray(boolean unsigned, int dimensions, int count, short... shortArray)
    {
        if (!unsigned)
        {
            return new NativeShortVectorBuffer(dimensions, count, shortArray);
        }
        else
        {
            return new NativeUnsignedShortVectorBuffer(dimensions, count, shortArray);
        }
    }

    /**
     * Creates a new vector buffer with the specified number of dimensions and elements, and an array of initial values to write to the buffer.
     * Each dimension of every vector is to be represented as an 32-bit integer.
     * @param unsigned True if the elements in the buffer should be interpreted as unsigned; false otherwise.
     * @param dimensions The number of dimensions in each vector element.
     * @param count The number of vector elements in the buffer.
     * @param intArray The elements to use to initialize the vector buffer.
     */
    public ReadonlyNativeVectorBuffer createFromIntArray(boolean unsigned, int dimensions, int count, int... intArray)
    {
        if (!unsigned)
        {
            return new NativeIntVectorBuffer(dimensions, count, intArray);
        }
        else
        {
            return new NativeUnsignedIntVectorBuffer(dimensions, count, intArray);
        }
    }

    /**
     * Creates a new vector buffer with the specified number of dimensions and elements, and an array of initial values to write to the buffer.
     * Each dimension of every vector is to be represented as an 32-bit floating-point number.
     * @param dimensions The number of dimensions in each vector element.
     * @param count The number of vector elements in the buffer.
     * @param floatArray The elements to use to initialize the vector buffer.
     */
    public ReadonlyNativeVectorBuffer createFromFloatArray(int dimensions, int count, float... floatArray)
    {
        return new NativeFloatVectorBuffer(dimensions, count, floatArray);
    }

    /**
     * Creates a new vector buffer with the specified number of dimensions and elements, and an array of initial values to write to the buffer.
     * Each dimension of every vector is to be represented as an 64-bit floating-point number.
     * @param dimensions The number of dimensions in each vector element.
     * @param count The number of vector elements in the buffer.
     * @param doubleArray The elements to use to initialize the vector buffer.
     */
    public ReadonlyNativeVectorBuffer createFromDoubleArray(int dimensions, int count, double... doubleArray)
    {
        return new NativeDoubleVectorBuffer(dimensions, count, doubleArray);
    }
}
