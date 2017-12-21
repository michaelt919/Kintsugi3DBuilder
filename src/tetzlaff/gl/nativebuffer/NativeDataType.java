package tetzlaff.gl.nativebuffer;

public enum NativeDataType 
{
    UNSIGNED_BYTE(1),
    BYTE(1),
    UNSIGNED_SHORT(2),
    SHORT(2),
    UNSIGNED_INT(4),
    INT(4),
    FLOAT(4),
    DOUBLE(8);

    private final int sizeInBytes;

    NativeDataType(int sizeInBytes)
    {
        this.sizeInBytes = sizeInBytes;
    }

    public int getSizeInBytes()
    {
        return sizeInBytes;
    }
}
