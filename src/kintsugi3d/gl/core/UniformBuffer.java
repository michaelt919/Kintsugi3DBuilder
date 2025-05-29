/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.core;

import java.nio.ByteBuffer;

import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;

/**
 * An interface for a uniform buffer object that can provide data to be used in conjunction with a shader program.
 * A uniform buffer should contain data that does not vary between primitives in a single draw call.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the index buffer object is associated with.
 */
public interface UniformBuffer<ContextType extends Context<ContextType>> extends Resource, ContextBound<ContextType>
{
    /**
     * Sets the content of the uniform buffer from a raw ByteBuffer.
     * @param data The raw buffer containing the uniform data.
     * @return The calling object.
     */
    UniformBuffer<ContextType> setData(ByteBuffer data);

    /**
     * Sets the content of the uniform buffer from a memory buffer with a defined format (an array of vectors).
     * @param data The buffer containing the uniform data.
     * @return The calling object.
     */
    UniformBuffer<ContextType> setData(ReadonlyNativeVectorBuffer data);
}
