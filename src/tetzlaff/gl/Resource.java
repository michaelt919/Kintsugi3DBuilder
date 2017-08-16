package tetzlaff.gl;

import tetzlaff.gl.exceptions.GLException;

/**
 * A simple interface for a GL resource which needs to be memory-managed manually.
 * This is necessary because 
 * @author Michael Tetzlaff
 *
 */
public interface Resource extends AutoCloseable
{
    /**
     * Deletes all graphics resources associated with this object.
     * Any usage of this object after calling this method will cause undefined results.
     */
    void close() throws GLException;
}
