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

package tetzlaff.gl.core;

import tetzlaff.gl.nativebuffer.NativeVectorBuffer;

/**
 * An interface for a vertex buffer object that can provide data to be used for rendering.
 * A vertex buffer should a series of vertex attributes that can be organized into "primitives" for rendering.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the index buffer object is associated with.
 */
public interface VertexBuffer<ContextType extends Context<ContextType>> extends Resource, ContextBound<ContextType>
{
    /**
     * Gets the number of vertices in the vertex buffer.
     * @return The number of vertices.
     */
    int count();

    /**
     * Sets the content of the vertex buffer from a memory buffer with a defined format (an array of vectors).
     * @param data The buffer containing the vertex data.
     * @param normalize Whether or not each vertex should be automatically normalized.
     * @return The calling object.
     */
    VertexBuffer<ContextType> setData(NativeVectorBuffer data, boolean normalize);

    /**
     * Sets the content of the vertex buffer from a memory buffer with a defined format (an array of vectors).
     * @param data The buffer containing the vertex data.
     * @return The calling object.
     */
    default VertexBuffer<ContextType> setData(NativeVectorBuffer  data)
    {
        return this.setData(data, false);
    }
}
