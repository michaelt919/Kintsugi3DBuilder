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

package tetzlaff.gl.types;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.function.Consumer;

import tetzlaff.gl.nativebuffer.NativeDataType;

public enum PackedDataType implements AbstractDataType<Iterable<? extends Number>>
{
    BYTE_3_3_2(NativeDataType.PACKED_BYTE, 3, 3, 3, 2, 0),
    SHORT_5_6_5(NativeDataType.PACKED_SHORT, 3, 5, 6, 5, 0),
    SHORT_5_5_5_1(NativeDataType.PACKED_SHORT, 4, 5, 5, 5, 1),
    SHORT_4_4_4_4(NativeDataType.PACKED_SHORT, 4, 4, 4, 4, 4),
    INT_10_10_10_2(NativeDataType.PACKED_INT, 4, 10, 10, 10, 2),
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

    @Override
    public Consumer<Iterable<? extends Number>> wrapByteBuffer(ByteBuffer baseBuffer)
    {
        Consumer<Number> packedConsumer = AbstractDataTypeFactory.wrapByteBuffer(baseBuffer, nativeDataType);

        return components ->
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

            packedConsumer.accept(packedValue);
        };
    }

    public int getRedBits()
    {
        return redBits;
    }

    public int getGreenBits()
    {
        return greenBits;
    }

    public int getBlueBits()
    {
        return blueBits;
    }

    public int getAlphaBits()
    {
        return alphaBits;
    }
}
