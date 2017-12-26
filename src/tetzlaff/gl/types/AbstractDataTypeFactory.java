package tetzlaff.gl.types;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.function.Consumer;

import tetzlaff.gl.nativebuffer.NativeDataType;

public final class AbstractDataTypeFactory
{
    private static final AbstractDataTypeFactory INSTANCE = new AbstractDataTypeFactory();

    public static AbstractDataTypeFactory getInstance()
    {
        return INSTANCE;
    }

    private AbstractDataTypeFactory()
    {
    }

    private static Consumer<Number> wrapByteBuffer(ByteBuffer baseBuffer, NativeDataType nativeDataType)
    {
        switch(nativeDataType)
        {
            case UNSIGNED_BYTE:
                return component -> baseBuffer.put(component.byteValue());
            case BYTE:
                return component -> baseBuffer.put(component.byteValue());
            case UNSIGNED_SHORT:
                return component -> baseBuffer.asShortBuffer().put(component.shortValue());
            case SHORT:
                return component -> baseBuffer.asShortBuffer().put(component.shortValue());
            case UNSIGNED_INT:
                return component -> baseBuffer.asIntBuffer().put(component.intValue());
            case INT:
                return component -> baseBuffer.asIntBuffer().put(component.intValue());
            case FLOAT:
                return component -> baseBuffer.asFloatBuffer().put(component.floatValue());
            case DOUBLE:
                return component -> baseBuffer.asDoubleBuffer().put(component.doubleValue());
            default:
                throw new UnsupportedOperationException("Unrecognized component data type.");
        }
    }

    private static final class MultiComponentDataType implements AbstractDataType<Iterable<Number>>
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
        public Consumer<Iterable<Number>> wrapByteBuffer(ByteBuffer baseBuffer)
        {
            Consumer<Number> componentConsumer = AbstractDataTypeFactory.wrapByteBuffer(baseBuffer, nativeDataType);

            return highLevelValue ->
            {
                int componentIndex = 0;
                Iterator<Number> iterator = highLevelValue.iterator();
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

    public AbstractDataType<Number> getSingleComponentDataType(NativeDataType nativeType)
    {
        return new SingleComponentDataType(nativeType);
    }

    public AbstractDataType<Iterable<Number>> getMultiComponentDataType(NativeDataType nativeType, int componentCount)
    {
        return new MultiComponentDataType(nativeType, componentCount);
    }
}
