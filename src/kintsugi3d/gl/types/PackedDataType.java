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

package kintsugi3d.gl.types;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import kintsugi3d.gl.nativebuffer.NativeDataType;

/**
 * Enumerates standard packed data types where there isn't a one-to-one correspondence between channels and byte groups.
 */
public enum PackedDataType implements AbstractDataType<Iterable<? extends Number>>
{
    /**
     * An 8-bit format containing 3 bits for red, 3 bits for green, and 2 bits for blue.
     */
    BYTE_3_3_2(NativeDataType.PACKED_BYTE, 3, 3, 3, 2, 0),

    /**
     * A 16-bit format containing 5 bits for red, 6 bits for green, and 5 bits for blue.
     */
    SHORT_5_6_5(NativeDataType.PACKED_SHORT, 3, 5, 6, 5, 0),

    /**
     * A 16-bit format containing 5 bits for red, 5 bits for green, 5 bits for blue, and 1 bit for an alpha mask.
     */
    SHORT_5_5_5_1(NativeDataType.PACKED_SHORT, 4, 5, 5, 5, 1),

    /**
     * A 16-bit format containing 4 bits for red, 4 bits for green, 4 bits for blue, and 4 bits for alpha.
     */
    SHORT_4_4_4_4(NativeDataType.PACKED_SHORT, 4, 4, 4, 4, 4),

    /**
     * A 32-bit format containing 10 bits for red, 10 bits for green, 10 bits for blue, and 2 bits for alpha.
     */
    INT_10_10_10_2(NativeDataType.PACKED_INT, 4, 10, 10, 10, 2),

    /**
     * A 32-bit format containing 8 bits for red, 8 bits for green, 8 bits for blue, and 8 bits for alpha.
     */
    INT_8_8_8_8(NativeDataType.PACKED_INT, 4, 8, 8, 8, 8);

    private final NativeDataType nativeDataType;
    private final int componentCount;
    private final int redBits;
    private final int greenBits;
    private final int blueBits;
    private final int alphaBits;

    PackedDataType(NativeDataType nativeDataType, int componentCount, int redBits, int greenBits, int blueBits, int alphaBits)
    {
        this.nativeDataType = nativeDataType;
        this.componentCount = componentCount;
        this.redBits = redBits;
        this.greenBits = greenBits;
        this.blueBits = blueBits;
        this.alphaBits = alphaBits;
    }

    @Override
    public NativeDataType getNativeDataType()
    {
        return nativeDataType;
    }

    @Override
    public int getComponentCount()
    {
        return componentCount;
    }

    @Override
    public int getSizeInBytes()
    {
        return nativeDataType.getSizeInBytes();
    }

    private int getPackedValue(Iterable<? extends Number> components)
    {
        Iterator<? extends Number> componentIterator = components.iterator();
        int packedValue = ((1 << redBits) - 1) & componentIterator.next().intValue();
        if (componentCount >= 2)
        {
            packedValue = (packedValue << greenBits) | (((1 << greenBits) - 1) & componentIterator.next().intValue());

            if (componentCount >= 3)
            {
                packedValue = (packedValue << blueBits) | (((1 << blueBits) - 1) & componentIterator.next().intValue());

                if (componentCount >= 4)
                {
                    packedValue = (packedValue << alphaBits) | ((1 << alphaBits) - 1) & componentIterator.next().intValue();
                }
            }
        }
        return packedValue;
    }

    @Override
    public Consumer<Iterable<? extends Number>> wrapByteBuffer(ByteBuffer baseBuffer)
    {
        Consumer<Number> packedConsumer = AbstractDataTypeFactory.wrapByteBuffer(baseBuffer, nativeDataType);
        return components -> packedConsumer.accept(getPackedValue(components));
    }



    @Override
    public BiConsumer<Integer, Iterable<? extends Number>> wrapIndexedByteBuffer(ByteBuffer baseBuffer)
    {
        BiConsumer<Integer, Number> packedConsumer = AbstractDataTypeFactory.wrapIndexedByteBuffer(baseBuffer, nativeDataType);
        return (index, components) -> packedConsumer.accept(index, getPackedValue(components));
    }

    /**
     * Gets the number of bits allocated for the red channel in this data type.
     * @return The number of bits allocated for red.
     */
    public int getRedBits()
    {
        return redBits;
    }

    /**
     * Gets the number of bits allocated for the green channel in this data type.
     * @return The number of bits allocated for green.
     */
    public int getGreenBits()
    {
        return greenBits;
    }

    /**
     * Gets the number of bits allocated for the blue channel in this data type.
     * @return The number of bits allocated for blue.
     */
    public int getBlueBits()
    {
        return blueBits;
    }

    /**
     * Gets the number of bits allocated for the alpha channel in this data type.
     * @return The number of bits allocated for alpha.
     */
    public int getAlphaBits()
    {
        return alphaBits;
    }
}
