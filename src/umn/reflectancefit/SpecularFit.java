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

package umn.reflectancefit;

import umn.gl.core.Context;
import umn.gl.core.Drawable;
import umn.gl.core.Framebuffer;
import umn.gl.core.Texture;
/**
 * The CPU side of a specular reflectance parameter estimation implementation.
 * Most of the real computation is delegated to a graphics shader program.
 * @param <ContextType> The type of the graphics context to use with a particular instance.
 */
class SpecularFit<ContextType extends Context<ContextType>>
{
    private final ParameterizedFitBase<ContextType> base;
    private final Framebuffer<ContextType> framebuffer;

    SpecularFit(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int subdiv)
    {
        this.framebuffer = framebuffer;
        base = new ParameterizedFitBase<>(drawable, subdiv);
    }

    void fitImageSpace(Texture<ContextType> viewImages, Texture<ContextType> depthImages,
            Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, SubdivisionRenderingCallback callback)
    {
        base.drawable.program().setTexture("diffuseEstimate", diffuseEstimate);
        base.drawable.program().setTexture("normalEstimate", normalEstimate);
        base.fitImageSpace(framebuffer, viewImages, depthImages, callback);
    }
}
