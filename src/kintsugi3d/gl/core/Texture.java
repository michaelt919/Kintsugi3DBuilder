/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
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
 * An interface for an object that can serve as a texture, an image stored in graphics memory.
 * In addition to the required methods, this interface also serves as a placeholder in the type hierarchy,
 * so that a class can explicitly state that it can serve this role in a manner that can be checked at compile-time with loose coupling.
 * Implementations should provide whatever methods are needed to ensure that they can fulfill this role for a specific GL architecture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture is associated with.
 */
public interface Texture<ContextType extends Context<ContextType>> extends Resource, ContextBound<ContextType>
{
    /**
     * Gets the number of mipmap levels in the texture.
     * @return The number of mipmap levels in the texture.
     */
    int getMipmapLevelCount();

    /**
     * Gets the texture's color format, if it is uncompressed.
     * @return The uncompressed color format, or null if the color format is compressed.
     */
    ColorFormat getInternalUncompressedColorFormat();

    /**
     * Gets the texture's compression format, if it is compressed.
     * @return The compression format, or null if the color format is uncompressed.
     */
    CompressionFormat getInternalCompressedColorFormat();

    /**
     * Gets whether the internal format is compressed.
     * @return True if the format is compressed; false if it is uncompressed.
     */
    boolean isInternalFormatCompressed();

    /**
     * Gets the type of the texture (color / depth / stencil).
     * @return The type of the texture.
     */
    TextureType getTextureType();
}
