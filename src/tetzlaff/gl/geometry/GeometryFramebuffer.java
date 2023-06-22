/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.geometry;

import java.io.File;
import java.io.FileNotFoundException;

import tetzlaff.gl.core.*;

public class GeometryFramebuffer<ContextType extends Context<ContextType>> implements GeometryTextures<ContextType>
{
    private final FramebufferObject<ContextType> fbo;

    /**
     *
     * @param geometry
     * @param width
     * @param height
     * @throws FileNotFoundException Thrown if there's trouble loading the geomBuffers shader for rendering to the framebuffer
     */
    GeometryFramebuffer(GeometryResources<ContextType> geometry, int width, int height) throws FileNotFoundException
    {
        // Use a shader program to initialize the framebuffer once
        try(Program<ContextType> program = geometry.context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/common/geomBuffers.frag"))
            .createProgram())
        {
            Drawable<ContextType> drawable = geometry.createDrawable(program);

            fbo = geometry.context.buildFramebufferObject(width, height)
                .addColorAttachment(ColorFormat.RGB32F) // position
                .addColorAttachment(ColorFormat.RGB32F) // normal
                .addColorAttachment(ColorFormat.RGB32F) // tangent
                .createFramebufferObject();

            drawable.draw(fbo);
        }
    }

    public Framebuffer<ContextType> getFramebuffer()
    {
        return fbo;
    }

    @Override
    public Texture<ContextType> getPositionTexture()
    {
        return fbo.getColorAttachmentTexture(0);
    }

    @Override
    public Texture<ContextType> getNormalTexture()
    {
        return fbo.getColorAttachmentTexture(1);
    }

    @Override
    public Texture<ContextType> getTangentTexture()
    {
        return fbo.getColorAttachmentTexture(2);
    }

    @Override
    public void close()
    {
        if (this.fbo != null)
        {
            this.fbo.close();
        }
    }

}
