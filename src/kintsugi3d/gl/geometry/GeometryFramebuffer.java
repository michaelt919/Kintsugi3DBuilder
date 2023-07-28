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

package kintsugi3d.gl.geometry;

import java.io.File;
import java.io.FileNotFoundException;

import kintsugi3d.gl.core.*;

public class GeometryFramebuffer<ContextType extends Context<ContextType>> implements GeometryTextures<ContextType> {
    private final FramebufferObject<ContextType> fbo;
    private final ContextType context;

    public static <ContextType extends Context<ContextType>> GeometryFramebuffer<ContextType> createEmpty(
        ContextType contextType, int width, int height)
    {
        return new GeometryFramebuffer<>(contextType, width, height);
    }

    /**
     * Creates an empty framebuffer for storing geometry textures
     * @param context
     * @param width
     * @param height
     */
    GeometryFramebuffer(ContextType context, int width, int height)
    {
        this.context = context;

        this.fbo = context.buildFramebufferObject(width, height)
            .addColorAttachment(ColorFormat.RGB32F) // position
            .addColorAttachment(ColorFormat.RGB32F) // normal
            .addColorAttachment(ColorFormat.RGB32F) // tangent
            .createFramebufferObject();
    }

    /**
     * Creates a framebuffer containing geometry textures rendered from a particular geometry instance
     * @param geometry
     * @param width
     * @param height
     * @throws FileNotFoundException Thrown if there's trouble loading the geomBuffers shader for rendering to the framebuffer
     */
    GeometryFramebuffer(GeometryResources<ContextType> geometry, int width, int height) throws FileNotFoundException
    {
        this(geometry.context, width, height);

        // Use a shader program to initialize the framebuffer once
        try(ProgramObject<ContextType> program = geometry.context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texspace.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/common/geomBuffers.frag"))
            .createProgram())
        {
            Drawable<ContextType> drawable = geometry.createDrawable(program);
            drawable.draw(fbo);
        }
        catch (FileNotFoundException e)
        {
            // Make sure there isn't a memory leak if it can't find the shaders.
            this.fbo.close();

            // Re-throw the exception.
            throw e;
        }
    }

    public Framebuffer<ContextType> getFramebuffer()
    {
        return fbo;
    }

    @Override
    public Texture2D<ContextType> getPositionTexture()
    {
        return fbo.getColorAttachmentTexture(0);
    }

    @Override
    public Texture2D<ContextType> getNormalTexture()
    {
        return fbo.getColorAttachmentTexture(1);
    }

    @Override
    public Texture2D<ContextType> getTangentTexture()
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

    @Override
    public ContextType getContext()
    {
        return context;
    }

    @Override
    public int getWidth()
    {
        return fbo.getSize().width;
    }

    @Override
    public int getHeight()
    {
        return fbo.getSize().height;
    }

    @Override
    public void setupShaderProgram(Program<ContextType> program)
    {
        program.setTexture("positionTex", getPositionTexture());
        program.setTexture("normalTex", getNormalTexture());
        program.setTexture("tangentTex", getTangentTexture());
    }

    @Override
    public GeometryTextures<ContextType> createViewportCopy(int x, int y, int viewportWidth, int viewportHeight)
    {
        GeometryFramebuffer<ContextType> viewportFBO = GeometryFramebuffer.createEmpty(context, viewportWidth, viewportHeight);

        // Copy positions
        viewportFBO.getFramebuffer().blitColorAttachmentFromFramebuffer(0,
            this.getFramebuffer().getViewport(x, y, viewportWidth, viewportHeight), 0);

        // Copy normals
        viewportFBO.getFramebuffer().blitColorAttachmentFromFramebuffer(1,
            this.getFramebuffer().getViewport(x, y, viewportWidth, viewportHeight), 1);

        // Copy tangents
        viewportFBO.getFramebuffer().blitColorAttachmentFromFramebuffer(2,
            this.getFramebuffer().getViewport(x, y, viewportWidth, viewportHeight), 2);

        // Return the new resource
        return viewportFBO;
    }
}
