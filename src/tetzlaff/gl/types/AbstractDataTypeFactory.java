/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.types;

import java.nio.*;
import java.util.Iterator;
import java.util.function.Consumer;

import tetzlaff.gl.nativebuffer.NativeDataType;

/**
 * A singleton factory object that can create concrete instances of the AbstractDataType interface.
 */
public final class AbstractDataTypeFactory
{
    private static final AbstractDataTypeFactory INSTANCE = new AbstractDataTypeFactory();

    /**
     * Gets the singleton instance.
     * @return The singleton instance.
     */
    public static AbstractDataTypeFactory getInstance()
    {
        return INSTANCE;
    }

    private AbstractDataTypeFactory()
    {
    }

    static Consumer<Number> wrapByteBuffer(ByteBuffer baseBuffer, NativeDataType nativeDataType)
    {
        switch(nativeDataType)
        {
            case UNSIGNED_BYTE:
            case BYTE:
            case PACKED_BYTE:
                ByteBuffer byteBuffer = baseBuffer.slice();
                return component -> byteBuffer.put(component.byteValue());
            case UNSIGNED_SHORT:
            case SHORT:
            case PACKED_SHORT:
                ShortBuffer shortBuffer = baseBuffer.asShortBuffer();
                return component -> shortBuffer.put(component.shortValue());
            case UNSIGNED_INT:
            case INT:
            case PACKED_INT:
                IntBuffer intBuffer = baseBuffer.asIntBuffer();
                return component -> intBuffer.put(component.intValue());
            case FLOAT:
                FloatBuffer floatBuffer = baseBuffer.asFloatBuffer();
                return component -> floatBuffer.put(component.floatValue());
            case DOUBLE:
                DoubleBuffer doubleBuffer = baseBuffer.asDoubleBuffer();
                return component -> doubleBuffer.put(component.doubleValue());
            default:
                throw new UnsupportedOperationException("Unrecognized component data type.");
        }
    }

    private static final class MultiComponentDataType implements AbstractDataType<Iterable<? extends Number>>
    {
        private final NativeDataType nativeDataType;
        private final int componentCount;

        private MultiComponentDataType(NativeDataType nativeDataType, int componentCount)
        {
            this.nativeDataType = nativeDataType;
            this.componentCount = componentCount;
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
            return nativeDataType.getSizeInBytes() * componentCount;
        }

        @Override
        public Consumer<Iterable<? extends Number>> wrapByteBuffer(ByteBuffer baseBuffer)
        {
            Consumer<Number> componentConsumer = AbstractDataTypeFactory.wrapByteBuffer(baseBuffer, nativeDataType);

            return highLevelValue ->
            {
                int componentIndex = 0;
                Iterator<? extends Number> iterator = highLevelValue.iterator();
                while (iterator.hasNext() && componentIndex < this.componentCount)
                {
                    componentConsumer.accept(iterator.next());
                    componentIndex++;
                }

                while(componentIndex < this.componentCount)
                {
                    componentConsumer.accept(0);
                    componentIndex++;
                }
            };
        }
    }

    private static final class SingleComponentDataType implements AbstractDataType<Number>
    {
        private final NativeDataType nativeDataType;

        private SingleComponentDataType(NativeDataType nativeDataType)
        {
            this.nativeDataType = nativeDataType;
        }

        @Override
        public NativeDataType getNativeDataType()
        {
            return nativeDataType;
        }

        @Override
        public int getComponentCount()
        {
            return 1;
        }

        @Override
        public int getSizeInBytes()
        {
            return nativeDataType.getSizeInBytes();
        }

        @Override
        public Consumer<Number> wrapByteBuffer(ByteBuffer baseBuffer)
        {
            return AbstractDataTypeFactory.wrapByteBuffer(baseBuffer, nativeDataType);
        }
    }

    /**
     * Creates an AbstractDataType for an element that has only a single component.
     * @param nativeType The internal type of each element.
     * @return An AbstractDataType representing a single component of the specified internal type.
     */
    public AbstractDataType<Number> getSingleComponentDataType(NativeDataType nativeType)
    {
        return new SingleComponentDataType(nativeType);
    }

    /**
     * Creates an AbstractDataType for an element that has multiple components.
     * @param nativeType The internal type of each component.
     * @param componentCount The number of components.
     * @return An AbstractDataType representing a vector with the specified number of components of the specified internal type.
     */
    public AbstractDataType<Iterable<? extends Number>> getMultiComponentDataType(NativeDataType nativeType, int componentCount)
    {
        return new MultiComponentDataType(nativeType, componentCount);
    }
}
