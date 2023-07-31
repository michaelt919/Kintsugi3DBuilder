/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.core.*;
import kintsugi3d.builder.core.TextureFitSettings;

public final class AlbedoORMOptimization<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private static final Logger log = LoggerFactory.getLogger(AlbedoORMOptimization.class);

    // Estimation program
    private ProgramObject<ContextType> estimationProgram;

    // Framebuffer for storing the diffuse solution
    private FramebufferObject<ContextType> framebuffer;

    private VertexBuffer<ContextType> rect;
    private final Drawable<ContextType> drawable;
    private final Texture2D<ContextType> occlusionMap;

    public static <ContextType extends Context<ContextType>> AlbedoORMOptimization<ContextType> createWithOcclusion(
        Texture2D<ContextType> occlusionMap, TextureFitSettings settings)
        throws FileNotFoundException
    {
        return new AlbedoORMOptimization<>(occlusionMap.getContext(), occlusionMap, settings);
    }

    public static <ContextType extends Context<ContextType>> AlbedoORMOptimization<ContextType> createWithoutOcclusion(
        ContextType context, TextureFitSettings settings)
        throws FileNotFoundException
    {
        return new AlbedoORMOptimization<>(context, null, settings);
    }

    private AlbedoORMOptimization(ContextType context, Texture2D<ContextType> occlusionMap, TextureFitSettings settings)
        throws FileNotFoundException
    {
        // Graphics context
        this.occlusionMap = occlusionMap;
        estimationProgram = createProgram(context, occlusionMap != null);
        framebuffer = context.buildFramebufferObject(settings.width, settings.height)
            .addColorAttachment(ColorFormat.RGBA32F) // total albedo
            .addColorAttachment(ColorFormat.RGBA32F) // ORM
            .createFramebufferObject();

        // Create basic rectangle vertex buffer
        rect = context.createRectangle();
        drawable = context.createDrawable(estimationProgram);
        drawable.setDefaultPrimitiveMode(PrimitiveMode.TRIANGLE_FAN);
        drawable.addVertexBuffer("position", rect);

        estimationProgram.setUniform("gamma", settings.gamma);
    }

    public void execute(SpecularResources<ContextType> specularFit)
    {
        // Set up shader program
        estimationProgram.setTexture("diffuseEstimate", specularFit.getDiffuseMap());
        estimationProgram.setTexture("specularEstimate", specularFit.getSpecularReflectivityMap());
        estimationProgram.setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());

        if (occlusionMap != null)
        {
            estimationProgram.setTexture("occlusionTexture", occlusionMap);
        }

        // Estimate albedo and roughness; passthrough occlusion if it is present
        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
        framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
        framebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
        drawable.draw(framebuffer);
    }

    @Override
    public void close()
    {
        if (estimationProgram != null)
        {
            estimationProgram.close();
            estimationProgram = null;
        }

        if (framebuffer != null)
        {
            framebuffer.close();
            framebuffer = null;
        }

        if (rect != null)
        {
            rect.close();
            rect = null;
        }
    }

    public Texture2D<ContextType> getAlbedoMap()
    {
        return framebuffer.getColorAttachmentTexture(0);
    }

    public Texture2D<ContextType> getORMMap()
    {
        return framebuffer.getColorAttachmentTexture(1);
    }

    public void saveTextures(File outputDirectory)
    {
        try
        {
            framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", new File(outputDirectory, "albedo.png"));
            framebuffer.getTextureReaderForColorAttachment(1).saveToFile("PNG", new File(outputDirectory, "orm.png"));
        }
        catch (IOException e)
        {
            log.error("An error occurred while saving textures:", e);
        }
    }

    private static <ContextType extends Context<ContextType>>
    ProgramObject<ContextType> createProgram(ContextType context, boolean occlusionTextureEnabled) throws FileNotFoundException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/estimateAlbedoORM.frag"))
            .define("OCCLUSION_TEXTURE_ENABLED", occlusionTextureEnabled)
            .createProgram();
    }
}
