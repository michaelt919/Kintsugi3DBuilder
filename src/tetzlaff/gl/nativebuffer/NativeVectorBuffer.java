package tetzlaff.gl.nativebuffer;

import java.nio.ByteBuffer;

/**
 * An interface for representing a buffer containing data intended to be passed to a graphics context memory buffer.
 * In order to work seamlessly with wrapped native libraries, this data must be stored in "native memory" rather than in traditional Java arrays.
 */
public interface NativeVectorBuffer
{
    /**
     * Gets the number of "dimensions", or components, in a single "element" within this buffer.
     * @return The number of dimensions per element.
     */
    int getDimensions();

    /**
     * The number of elements in the buffer.
     * @return
     */
    int getCount();

    /**
     * Gets a single component of a single element within the buffer.
     * @param index The index of the element to query.
     * @param dimension The dimension within the element to retrieve.
     * @return The specified component of the specified element.
     */
    Number get(int index, int dimension);

    /**
     * Sets a single component of a single element within the buffer.
     * @param index The index of the element to modify.
     * @param dimension The dimension within the element to set.
     * @param value The new value of the specified component of the specified element.
     */
    void set(int index, int dimension, Number value);

    /**
     * Gets the entire buffer as a native memory buffer.
     * @return A native memory buffer containing the data.
     */
    ByteBuffer getBuffer();

    /**
     * Gets the data type being represented in this buffer.
     * @return
     */
    NativeDataType getDataType();
}
