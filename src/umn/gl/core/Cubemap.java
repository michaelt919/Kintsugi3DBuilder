/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.core;

public interface Cubemap <ContextType extends Context<ContextType>> extends Texture<ContextType>
{
    /**
     * Gets the length in pixels along a side of one of the cubemap's faces.
     * @return The size of a face.
     */
    int getFaceSize();

    void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT);

    FramebufferAttachment<ContextType> getFaceAsFramebufferAttachment(CubemapFace face);
}