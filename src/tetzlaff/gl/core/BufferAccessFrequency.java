package tetzlaff.gl.core;

/**
 * Enumerates the possibly modes of frequency with which a buffer may be accessed.
 * @author Michael Tetzlaff
 *
 */
public enum BufferAccessFrequency 
{
    /**
     * The buffer will be written to once, and only read from a few times.
     */
    STREAM,

    /**
     * The buffer will be written to once, and read from many times.
     */
    STATIC,

    /**
     * The buffer may be written to or read from many times.
     */
    DYNAMIC
}
