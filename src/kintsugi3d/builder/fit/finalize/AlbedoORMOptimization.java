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

package kintsugi3d.builder.fit.finalize;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.resources.project.specular.SpecularMaterialResources;
import kintsugi3d.gl.builders.framebuffer.ColorAttachmentSpec;
import kintsugi3d.gl.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

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
        File albedoMapFile = new File(priorSolutionDirectory, "albedo.png");
        Texture2D<ContextType> albedoMap = albedoMapFile.exists() ?
            context.getTextureFactory()
                .build2DColorTextureFromFile(albedoMapFile, true)
                .setLinearFilteringEnabled(true)
                .createTexture()
            : null;

        // Load ORM map and use it as occlusion map (to preserve the occlusion stored in the red channel of ORM)
        File ormMapFile = new File(priorSolutionDirectory, "orm.png");
        this.occlusionMap = ormMapFile.exists() ?
            context.getTextureFactory().build2DColorTextureFromFile(ormMapFile, true)
                .setLinearFilteringEnabled(true)
                .createTexture()
            : null;

        if (albedoMap != null)
        {
            framebuffer =
                context.buildFramebufferObject(albedoMap.getWidth(), albedoMap.getHeight())
                    .addEmptyColorAttachment() // Will copy in albedo map after FBO is created
                    .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8)
                        .setLinearFilteringEnabled(true)) // Will blit in ORM map after FBO is created
                    .createFramebufferObject();
            framebuffer.setColorAttachment(0, albedoMap);
            framebuffer.getColorAttachmentTexture(1).blitScaled(occlusionMap, true);

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

    public void execute(SpecularMaterialResources<ContextType> specularFit)
    {
        // Set up shader program
        estimationProgram.setTexture("diffuseEstimate", specularFit.getDiffuseMap());
        estimationProgram.setTexture("specularEstimate", specularFit.getSpecularReflectivityMap());
        estimationProgram.setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());

        if (specularFit.getConstantMap() != null)
        {
            estimationProgram.setTexture("constantEstimate", specularFit.getConstantMap());
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

    public void saveTextures(File outputDirectory)
    {
        try
        {
            framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", new File(outputDirectory, "albedo.png"));
            framebuffer.getTextureReaderForColorAttachment(1).saveToFile("PNG", new File(outputDirectory, "orm.png"));
        }
        catch (IOException e)
        {
            LOG.error("An error occurred while saving textures:", e);
        }
    }

    private static <ContextType extends Context<ContextType>>
    ProgramObject<ContextType> createProgram(ContextType context, boolean occlusionTextureEnabled) throws IOException
    {
        return context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/estimateAlbedoORM.frag"))
            .define("OCCLUSION_TEXTURE_ENABLED", occlusionTextureEnabled)
            .createProgram();
    }
}
