/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.core;

/**
 * An interface for an object that can serve as a texture, an image stored in graphics memory.
 * This is an empty interface that simply serves as a placeholder in the type hierarchy,
 * so that a class can explicitly state that it can serve this role in a manner that can be checked at compile-time with loose coupling.
 * Implementations should provide whatever methods are needed to ensure that they can fulfill this role for a specific GL architecture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture is associated with.
 */
public interface Texture<ContextType extends Context<ContextType>> extends Resource, Contextual<ContextType>
{
    int getMipmapLevelCount();
    ColorFormat getInternalUncompressedColorFormat();
    CompressionFormat getInternalCompressedColorFormat();
    boolean isInternalFormatCompressed();
    TextureType getTextureType();
}
