/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit.finalize;

import kintsugi3d.builder.core.StandardTexture;
import kintsugi3d.builder.core.TextureDetails;
import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.gl.builders.framebuffer.ColorAttachmentSpec;
import kintsugi3d.gl.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class AlbedoORMOptimization<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private static final Logger LOG = LoggerFactory.getLogger(AlbedoORMOptimization.class);

    // Estimation program
    private ProgramObject<ContextType> estimationProgram;

    // Framebuffer for storing the diffuse solution
    private FramebufferObject<ContextType> framebuffer;

    private VertexBuffer<ContextType> rect;
    private final Drawable<ContextType> drawable;
    private final Texture2D<ContextType> occlusionMap;

    public static <ContextType extends Context<ContextType>> AlbedoORMOptimization<ContextType> createWithOcclusion(
        Texture2D<ContextType> occlusionMap, TextureResolution settings)
        throws IOException
    {
        return new AlbedoORMOptimization<>(occlusionMap.getContext(), occlusionMap, settings);
    }

    public static <ContextType extends Context<ContextType>> AlbedoORMOptimization<ContextType> createWithoutOcclusion(
        ContextType context, TextureResolution resolution)
        throws IOException
    {
        return new AlbedoORMOptimization<>(context, null, resolution);
    }

    private AlbedoORMOptimization(ContextType context, Texture2D<ContextType> occlusionMap, TextureResolution settings)
        throws IOException
    {
        this.occlusionMap = occlusionMap;
        estimationProgram = createProgram(context, occlusionMap != null);
        framebuffer = context.buildFramebufferObject(settings.width, settings.height)
            .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8)
                .setLinearFilteringEnabled(true)) // total albedo
            .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8)
                .setLinearFilteringEnabled(true)) // ORM
            .createFramebufferObject();

        // Create basic rectangle vertex buffer
        rect = context.createRectangle();
        drawable = context.createDrawable(estimationProgram);
        drawable.setDefaultPrimitiveMode(PrimitiveMode.TRIANGLE_FAN);
        drawable.addVertexBuffer("position", rect);
    }

    public static <ContextType extends Context<ContextType>> AlbedoORMOptimization<ContextType> loadFromPriorSolution(
        ContextType context, File priorSolutionDirectory) throws IOException
    {
        return new AlbedoORMOptimization<>(context, priorSolutionDirectory);
    }

    private AlbedoORMOptimization(ContextType context, File priorSolutionDirectory)
        throws IOException
    {
        Texture2D<ContextType> albedoMap = TextureResources.loadTexture(StandardTexture.ALBEDO, priorSolutionDirectory, context);

        if (TextureResources.getTextureFile(StandardTexture.OCCLUSION, priorSolutionDirectory).exists())
        {
            // Load occlusion map if it exists (to pack it into the red channel of ORM)
            this.occlusionMap = TextureResources.loadTexture(StandardTexture.OCCLUSION, priorSolutionDirectory, context);
        }
        else
        {
            this.occlusionMap = null;
        }

        if (albedoMap != null)
        {
            framebuffer =
                context.buildFramebufferObject(albedoMap.getWidth(), albedoMap.getHeight())
                    .addEmptyColorAttachment() // Will copy in albedo map after FBO is created
                    .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8)
                        .setLinearFilteringEnabled(true)) // Will blit in ORM map after FBO is created
                    .createFramebufferObject();
            framebuffer.setColorAttachment(0, albedoMap);

            if (occlusionMap != null)
            {
                framebuffer.getColorAttachmentTexture(1).blitScaled(occlusionMap, true);
            }

            estimationProgram = createProgram(context, occlusionMap != null);

            // Create basic rectangle vertex buffer
            rect = context.createRectangle();
            drawable = context.createDrawable(estimationProgram);
            drawable.setDefaultPrimitiveMode(PrimitiveMode.TRIANGLE_FAN);
            drawable.addVertexBuffer("position", rect);
        }
        else
        {
            drawable = null;
        }
    }

    public void execute(TextureResources<ContextType> specularFit)
    {
        // Set up shader program
        estimationProgram.setTexture("diffuseEstimate", specularFit.getTexture(StandardTexture.DIFFUSE_COLOR));
        estimationProgram.setTexture("tex_specular", specularFit.getTexture(StandardTexture.SPECULAR_COLOR));
        estimationProgram.setTexture("roughnessEstimate", specularFit.getTexture(StandardTexture.ROUGHNESS));

        if (specularFit.getTexture("constant") != null)
        {
            estimationProgram.setTexture("constantEstimate", specularFit.getTexture("constant"));
        }

        if (occlusionMap != null)
        {
            estimationProgram.setTexture("occlusionTexture", occlusionMap);
        }

        // Estimate albedo and roughness; passthrough occlusion if it is present
        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
        framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
        drawable.draw(framebuffer);
    }

    @Override
    public void close()
    {
        if (estimationProgram != null)
        {
            estimationProgram.close();
        }

        if (drawable != null)
        {
            drawable.close();
        }

        if (framebuffer != null)
        {
            framebuffer.close();
        }

        if (rect != null)
        {
            rect.close();
        }

        if (occlusionMap != null)
        {
            occlusionMap.close();
        }
    }

    public Texture2D<ContextType> getAlbedoMap()
    {
        return framebuffer == null ? null : framebuffer.getColorAttachmentTexture(0);
    }

    public Texture2D<ContextType> getORMMap()
    {
        return framebuffer == null ? null : framebuffer.getColorAttachmentTexture(1);
    }

    public int getTextureCount()
    {
        return getStandardTextures().size();
    }

    public Map<StandardTexture, Texture2D<ContextType>> getStandardTextures()
    {
        Map<StandardTexture, Texture2D<ContextType>> textures = new EnumMap<>(StandardTexture.class);
        if (framebuffer != null)
        {
            textures.putAll(Map.of(
                StandardTexture.ALBEDO, framebuffer.getColorAttachmentTexture(0),
                StandardTexture.ORM, framebuffer.getColorAttachmentTexture(1)));
        }

        if (occlusionMap != null)
        {
            textures.put(StandardTexture.OCCLUSION, occlusionMap);
        }

        return Collections.unmodifiableMap(textures);
    }

    public Map<TextureDetails, Texture2D<ContextType>> getTextures()
    {
        return StandardTexture.convertEnumMapToObjectMap(getStandardTextures());
    }

    private static <ContextType extends Context<ContextType>>
    ProgramObject<ContextType> createProgram(ContextType context, boolean occlusionTextureEnabled) throws IOException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/estimateAlbedoORM.frag"))
            .define("TEXTURE_OCCLUSION", occlusionTextureEnabled)
            .createProgram();
    }
}
