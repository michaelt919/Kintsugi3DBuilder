package tetzlaff.gl.types;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import tetzlaff.gl.nativebuffer.NativeDataType;

public interface AbstractDataType<HighLevelType>
{
    NativeDataType getNativeDataType();
    int getComponentCount();
    int getSizeInBytes();
    Consumer<HighLevelType> wrapByteBuffer(ByteBuffer baseBuffer);
}
