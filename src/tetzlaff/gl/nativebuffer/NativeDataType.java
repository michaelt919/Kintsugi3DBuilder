package tetzlaff.gl.nativebuffer;

/**
 * An enumeration of the possible data types that can be represented in native memory buffers.
 */
public enum NativeDataType 
{
    UNSIGNED_BYTE(1),
    BYTE(1),
    PACKED_BYTE(1, true),
    UNSIGNED_SHORT(2),
    SHORT(2),
    PACKED_SHORT(2, true),
    UNSIGNED_INT(4),
    INT(4),
    PACKED_INT(4, true),
    FLOAT(4),
    DOUBLE(8);

    private final int sizeInBytes;
    private final boolean packed;

    NativeDataType(int sizeInBytes, boolean packed)
    {
        this.sizeInBytes = sizeInBytes;
        this.packed = packed;
    }

    NativeDataType(int sizeInBytes)
    {
        this(sizeInBytes, false);
    }

    public int getSizeInBytes()
    {
        return sizeInBytes;
    }

    public boolean isPacked()
    {
        return packed;
    }
}
