package tetzlaff.gl.nativebuffer;

import java.nio.ByteBuffer;

public interface NativeVectorBuffer
{
    int getDimensions();
    int getCount();

    Number get(int index, int dimension);
    void set(int index, int dimension, Number value);

    ByteBuffer getBuffer();
    NativeDataType getDataType();
}
