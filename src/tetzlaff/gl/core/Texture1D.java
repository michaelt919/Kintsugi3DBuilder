/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.core;

/**
 * An interface for a one-dimensional texture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture is associated with.
 */
public interface Texture1D<ContextType extends Context<ContextType>> extends Texture<ContextType>, FramebufferAttachment<ContextType>
{
    /**
     * Gets the width of the texture.
     * @return The width of the texture.
     */
    int getWidth();

    void setTextureWrap(TextureWrapMode wrap);
}
