/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit.roughness;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.BasisWeightResources;
import kintsugi3d.gl.builders.framebuffer.ColorAttachmentSpec;
import kintsugi3d.gl.builders.framebuffer.FramebufferObjectBuilder;
import kintsugi3d.gl.core.ColorFormat;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.gl.core.Texture2D;

public class RoughnessOptimizationSimple<ContextType extends Context<ContextType>> extends RoughnessOptimizationBase<ContextType>
{
    private final FramebufferObject<ContextType> specularTexFramebuffer;

    public RoughnessOptimizationSimple(BasisResources<ContextType> basisResources,
        BasisWeightResources<ContextType> weightResources, TextureResolution settings)
        throws IOException
    {
        super(basisResources);
        setInputWeights(weightResources);

        // Framebuffer for fitting and storing the specular parameter estimates (specular Fresnel color and roughness)
        specularTexFramebuffer = basisResources.getContext().buildFramebufferObject(
                settings.width, settings.height)
            .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8).setLinearFilteringEnabled(true))
            .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8).setLinearFilteringEnabled(true))
            .createFramebufferObject();
    }

    public RoughnessOptimizationSimple(BasisResources<ContextType> basisResources, File priorSolutionDirectory)
        throws IOException
    {
        super(basisResources);

        // Load specular and roughness maps from files
        Texture2D<ContextType> specularTex = basisResources.getContext().getTextureFactory()
            .build2DColorTextureFromFile(new File(priorSolutionDirectory, "specular.png"), true)
            .setLinearFilteringEnabled(true)
            .createTexture();
        Texture2D<ContextType> roughnessTex = basisResources.getContext().getTextureFactory()
            .build2DColorTextureFromFile(new File(priorSolutionDirectory, "roughness.png"), true)
            .setLinearFilteringEnabled(true)
            .createTexture();

        // Framebuffer for fitting and storing the specular parameter estimates (specular Fresnel color and roughness)
        FramebufferObjectBuilder<ContextType> fboBuilder = basisResources.getContext().buildFramebufferObject(
                roughnessTex.getWidth(), roughnessTex.getHeight());

        boolean needsBlit;
        if (specularTex.getWidth() != roughnessTex.getWidth() || specularTex.getHeight() != roughnessTex.getHeight())
        {
            // Will need to blit to match resolution
            fboBuilder.addColorAttachment(ColorFormat.RGBA8);
            needsBlit = true;
        }
        else
        {
            fboBuilder.addEmptyColorAttachment();
            needsBlit = false;
        }

        fboBuilder.addEmptyColorAttachment(); // Will later attach roughnessTex that has already been loaded

        specularTexFramebuffer = fboBuilder.createFramebufferObject();

        if (needsBlit)
        {
            specularTexFramebuffer.getColorAttachmentTexture(0).blitScaled(specularTex, true);
        }
        else
        {
            specularTexFramebuffer.setColorAttachment(0, specularTex);
        }

        specularTexFramebuffer.setColorAttachment(1, roughnessTex);
    }

    @Override
    protected FramebufferObject<ContextType> getFramebuffer()
    {
        return specularTexFramebuffer;
    }

    @Override
    public void close()
    {
        super.close();
        specularTexFramebuffer.close();
    }
}
