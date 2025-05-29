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

/**
 * An interface for a cubemap texture.
 * @author Michael Tetzlaff
 * @param <ContextType> The type of the GL context that the texture is associated with.
 */
public interface Cubemap <ContextType extends Context<ContextType>> extends Texture<ContextType>
{
    /**
     * Gets the length in pixels along a side of one of the cubemap's faces.
     * @return The size of a face.
     */
    int getFaceSize();

    /**
     * Sets the wrap mode for the texture.
     * @param wrapS The horiozontal wrap mode.
     * @param wrapT The vertical wrap mode.
     */
    void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT);

    /**
     * Gets a framebuffer attachment that can be used to render into a particular face of the cubemap.
     * @param face The face for which to retrieve the framebuffer attachment.
     * @return The face as a framebuffer attachment.
     */
    FramebufferAttachment<ContextType> getFaceAsFramebufferAttachment(CubemapFace face);
}
