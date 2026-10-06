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

package kintsugi3d.builder.resources.project.specular;

import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.fit.decomposition.ReadonlyBasisResources;
import kintsugi3d.builder.fit.decomposition.ReadonlyBasisWeightResources;
import kintsugi3d.gl.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public abstract class ReadonlyTextureResourcesBase<ContextType extends Context<ContextType>>
    implements ReadonlyTextureResources<ContextType>
{
    protected static final Logger LOG = LoggerFactory.getLogger(ReadonlyTextureResourcesBase.class);

    private static <ContextType extends Context<ContextType>> void useTextureSafe(
        Program<ContextType> program, String textureName, Texture<ContextType> texture)
    {
        if (texture == null)
        {
            program.setTexture(textureName, program.getContext().getTextureFactory().getNullTexture(SamplerType.FLOAT_2D));
        }
        else
        {
            program.setTexture(textureName, texture);
        }
    }

    @Override
    public void setupShaderProgram(Program<ContextType> program)
    {
        for (var entry : this.getTextures().entrySet())
        {
            useTextureSafe(program,
                // Sanitize texture names when being used as shader variables:
                // replace one or more non-alphanumeric characters with underscore.
                String.format("tex_%s", entry.getKey().name.replaceAll("[^A-Za-z0-9]+", "_")),
                entry.getValue());
        }

        if (this.getBasisResources() != null)
        {
            this.getBasisResources().useWithShaderProgram(program);
        }

        if (this.getBasisWeightResources() != null)
        {
            this.getBasisWeightResources().useWithShaderProgram(program);
        }
    }

    @Override
    public void saveTexture(String texName, String format, File outputDirectory, String filenameOverride)
    {
        try
        {
            ReadonlyTexture2D<ContextType> tex = getTexture(texName);
            if (tex != null)
            {
                tex.getColorTextureReader().saveToFile(format, new File(outputDirectory,
                    filenameOverride != null ? filenameOverride : BasisResources.getBasisFunctionsFilename()));
            }
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving texture: {}", texName, e);
        }
    }

    @Override
    public void savePackedWeightMaps(String format, File outputDirectory, String filenamePrefix)
    {
        ReadonlyBasisWeightResources<ContextType> weightResources = getBasisWeightResources();
        if (weightResources != null)
        {
            weightResources.savePacked(TextureResources.WEIGHTS_PER_CHANNEL_PACKED_IMAGE, format, outputDirectory,
                i -> TextureResources.getPackedWeightMapFilename(i, format, filenamePrefix));
        }
    }

    @Override
    public void saveUnpackedWeightMaps(String format, File outputDirectory, String filenamePrefix)
    {
        ReadonlyBasisWeightResources<ContextType> weightResources = getBasisWeightResources();
        if (weightResources != null)
        {
            weightResources.saveUnpacked(format, outputDirectory, filenamePrefix);
        }
    }

    @Override
    public void saveUnpackedWeightMaps(String format, File outputDirectory)
    {
        ReadonlyBasisWeightResources<ContextType> weightResources = getBasisWeightResources();
        if (weightResources != null)
        {
            weightResources.saveUnpacked(format, outputDirectory);
        }
    }

    @Override
    public void saveBasisFunctions(File outputDirectory, String filenameOverride)
    {
        ReadonlyBasisResources<ContextType> basisResources = getBasisResources();
        if (basisResources != null)
        {
            basisResources.save(outputDirectory, filenameOverride);
        }
    }
}
