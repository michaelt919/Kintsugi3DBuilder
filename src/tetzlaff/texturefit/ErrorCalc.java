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

class ErrorCalc<ContextType extends Context<ContextType>>
{
    private final ParameterizedFitBase<ContextType> base;

    ErrorCalc(Drawable<ContextType> drawable, int viewCount, int subdiv)
    {
        base = new ParameterizedFitBase<>(drawable, viewCount, subdiv);
    }

    void fitImageSpace(Framebuffer<ContextType> framebuffer, Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages,
        Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, Texture<ContextType> specularEstimate, Texture<ContextType> roughnessEstimate,
        Texture<ContextType> errorTexture, SubdivisionRenderingCallback callback) throws IOException
    {
        base.drawable.program().setTexture("diffuseEstimate", diffuseEstimate);
        base.drawable.program().setTexture("normalEstimate", normalEstimate);
        base.drawable.program().setTexture("specularEstimate", specularEstimate);
        base.drawable.program().setTexture("roughnessEstimate", roughnessEstimate);
        base.drawable.program().setTexture("errorTexture", errorTexture);
        base.fitImageSpace(framebuffer, viewImages, depthImages, shadowImages, callback);
    }

    void fitTextureSpace(Framebuffer<ContextType> framebuffer, File preprocessDirectory,
        Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, Texture<ContextType> specularEstimate, Texture<ContextType> roughnessEstimate,
        Texture<ContextType> errorTexture, SubdivisionRenderingCallback callback) throws IOException
    {
        base.drawable.program().setTexture("diffuseEstimate", diffuseEstimate);
        base.drawable.program().setTexture("normalEstimate", normalEstimate);
        base.drawable.program().setTexture("specularEstimate", specularEstimate);
        base.drawable.program().setTexture("roughnessEstimate", roughnessEstimate);
        base.drawable.program().setTexture("errorTexture", errorTexture);
        base.fitTextureSpace(framebuffer, preprocessDirectory, callback);
    }
}
