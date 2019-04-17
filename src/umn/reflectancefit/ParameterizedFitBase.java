/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.reflectancefit;

import umn.gl.core.*;
import umn.gl.vecmath.Vector2;

/**
 * A general purpose reflectance parameter estimation implementation.
 * Handles the actual draw calls using the graphics context.
 * @param <ContextType> The type of the graphics context to use with a particular instance.
 */
class ParameterizedFitBase<ContextType extends Context<ContextType>>
{
    @SuppressWarnings("PackageVisibleField") final Drawable<ContextType> drawable;
    private final int subdiv;

    ParameterizedFitBase(Drawable<ContextType> drawable, int subdiv)
    {
        this.subdiv = subdiv;
        this.drawable = drawable;
    }

    private void fitSubdiv(Framebuffer<ContextType> framebuffer, int row, int col,
        Texture<ContextType> viewImages, Texture<ContextType> depthImages)
    {
        int subdivWidth = framebuffer.getSize().width / subdiv;
        int subdivHeight = framebuffer.getSize().height / subdiv;

        drawable.program().setTexture("viewImages", viewImages);
        drawable.program().setTexture("depthImages", depthImages);

        drawable.program().setUniform("minTexCoord",
                new Vector2((float)col / (float)subdiv, (float)row / (float)subdiv));

        drawable.program().setUniform("maxTexCoord",
                new Vector2((float)(col+1) / (float)subdiv, (float)(row+1) / (float)subdiv));

        drawable.draw(PrimitiveMode.TRIANGLES, framebuffer, col * subdivWidth, row * subdivHeight, subdivWidth, subdivHeight);
        drawable.getContext().finish();
    }

    void fitImageSpace(Framebuffer<ContextType> framebuffer, Texture<ContextType> viewImages, Texture<ContextType> depthImages,
            SubdivisionRenderingCallback callback)
    {
        if (this.subdiv == 1)
        {
            this.fitSubdiv(framebuffer, 0, 0, viewImages, depthImages);
        }
        else
        {
            for (int row = 0; row < this.subdiv; row++)
            {
                for (int col = 0; col < this.subdiv; col++)
                {
                    this.fitSubdiv(framebuffer, row, col, viewImages, depthImages);
                    callback.execute(row, col);
                }
            }
        }
    }
}
