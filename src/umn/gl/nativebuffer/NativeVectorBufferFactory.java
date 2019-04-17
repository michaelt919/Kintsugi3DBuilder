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
 * A factory for instantiating implementations of a NativeVectorBuffer.
 */
public final class NativeVectorBufferFactory 
{
    private static final NativeVectorBufferFactory INSTANCE = new NativeVectorBufferFactory();

    /**
     * Gets a singleton instance of the factory.
     * @return The single instance of the factory.
     */
    public static NativeVectorBufferFactory getInstance()
    {
        return INSTANCE;
    }

    /**
     * Creates an empty buffer with a specified size, dimensionality, and data type.
     * @param dataType The primitive data type of each component of an element in the buffer.
     * @param dimensions The number of components in each element in the buffer.
     * @param count The number of elements in the buffer.
     * @return A newly allocated native buffer.
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
     * Creates an new buffer with a specified size, dimensionality, and data type, and copies data from a raw, unformatted native buffer.
     * @param dataType The primitive data type of each component of an element in the buffer.
     * @param dimensions The number of components in each element in the buffer.
     * @param count The number of elements in the buffer.
     * @param buffer The unformatted buffer from which to copy data.
     * @return A newly allocated native buffer containing the data in the provided unformatted buffer.
     */
    public NativeVectorBuffer createFromExistingBuffer(NativeDataType dataType, int dimensions, int count, ByteBuffer buffer)
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
     * Creates an new buffer with a specified size, dimensionality, and data type, and copies data from an array of Java bytes.
     * @param dataType The primitive data type of each component of an element in the buffer.
     * @param dimensions The number of components in each element in the buffer.
     * @param count The number of elements in the buffer.
     * @param byteArray The byte array from which to copy data.
     * @return A newly allocated native buffer containing the data in the provided byte array.
     */
    public NativeVectorBuffer createFromByteArray(NativeDataType dataType, int dimensions, int count, byte... byteArray)
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
     * Creates an new buffer with a specified size, dimensionality, and data type, and copies data from an array of Java shorts.
     * @param unsigned Whether or not elements in the buffer should be treated as signed or unsigned shorts.
     * @param dimensions The number of components in each element in the buffer.
     * @param count The number of elements in the buffer.
     * @param shortArray The array from which to copy data.
     * @return A newly allocated native buffer containing the data in the provided array.
     */
    public NativeVectorBuffer createFromShortArray(boolean unsigned, int dimensions, int count, short... shortArray)
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
     * Creates an new buffer with a specified size, dimensionality, and data type, and copies data from an array of Java ints.
     * @param unsigned Whether or not elements in the buffer should be treated as signed or unsigned ints.
     * @param dimensions The number of components in each element in the buffer.
     * @param count The number of elements in the buffer.
     * @param intArray The array from which to copy data.
     * @return A newly allocated native buffer containing the data in the provided array.
     */
    public NativeVectorBuffer createFromIntArray(boolean unsigned, int dimensions, int count, int... intArray)
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
     * Creates an new buffer with a specified size, dimensionality, and data type, and copies data from an array of Java floats.
     * @param dimensions The number of components in each element in the buffer.
     * @param count The number of elements in the buffer.
     * @param floatArray The array from which to copy data.
     * @return A newly allocated native buffer containing the data in the provided array.
     */
    public NativeVectorBuffer createFromFloatArray(int dimensions, int count, float... floatArray)
    {
        return new NativeFloatVectorBuffer(dimensions, count, floatArray);
    }

    /**
     * Creates an new buffer with a specified size, dimensionality, and data type, and copies data from an array of Java doubles.
     * @param dimensions The number of components in each element in the buffer.
     * @param count The number of elements in the buffer.
     * @param doubleArray The array from which to copy data.
     * @return A newly allocated native buffer containing the data in the provided array.
     */
    public NativeVectorBuffer createFromDoubleArray(int dimensions, int count, double... doubleArray)
    {
        return new NativeDoubleVectorBuffer(dimensions, count, doubleArray);
    }
}
