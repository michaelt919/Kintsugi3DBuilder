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

package tetzlaff.texturefit;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.Texture;
import tetzlaff.texturefit.ParameterizedFitBase.SubdivisionRenderingCallback;

class DiffuseFit<ContextType extends Context<ContextType>>
{
    private final ParameterizedFitBase<ContextType> base;
    private final Framebuffer<ContextType> framebuffer;

    DiffuseFit(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int viewCount, int subdiv)
    {
        this.framebuffer = framebuffer;
        base = new ParameterizedFitBase<>(drawable, viewCount, subdiv);
    }

    void fitImageSpace(Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages, SubdivisionRenderingCallback callback)
        throws IOException
    {
        base.fitImageSpace(framebuffer, viewImages, depthImages, shadowImages, callback);
    }

    void fitTextureSpace(File preprocessDirectory,  SubdivisionRenderingCallback callback) throws IOException
    {
        base.fitTextureSpace(framebuffer, preprocessDirectory, callback);
    }
}
