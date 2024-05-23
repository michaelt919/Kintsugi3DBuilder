/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.core;

/**
 * An interface for a buffer object that can serve as an "index buffer" or "element array buffer" in conjunction with one or more vertex buffer objects (VBOs).
 * This buffer specifies the order in which vertices in VBOs should be processed during a draw call.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the index buffer object is associated with.
 */
public interface IndexBuffer<ContextType extends Context<ContextType>> extends Resource, ContextBound<ContextType>
{
    /**
     * Gets the number of indices in the buffer.
     * @return The number of indices in the buffer.
     */
    int count();

    /**
     * Sets the data of the index buffer from an array.
     * @param data The data to store in the index buffer.
     * @return The same index buffer to support chained operations.
     */
    IndexBuffer<ContextType> setData(int... data);
}
