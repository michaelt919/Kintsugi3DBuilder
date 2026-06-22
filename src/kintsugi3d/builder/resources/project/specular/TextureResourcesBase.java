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

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.fit.decomposition.BasisImageCreator;
import kintsugi3d.builder.io.specular.WeightImageWriter;
import kintsugi3d.builder.javafx.core.ExceptionHandling;
import kintsugi3d.gl.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;

public abstract class TextureResourcesBase<ContextType extends Context<ContextType>>
    implements TextureResources<ContextType>
{
    private static final Logger LOG = LoggerFactory.getLogger(TextureResourcesBase.class);

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
            Texture2D<ContextType> tex = getTexture(texName);
            if (tex != null)
            {
                tex.getColorTextureReader().saveToFile(format, new File(outputDirectory,
                    filenameOverride != null ? filenameOverride : TextureResources.getBasisFunctionsFilename()));
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
        if (getBasisWeightResources() != null)
        {
            // Save the packed weight maps for opening in viewer
            try (WeightImageWriter<ContextType> weightImageWriter =
                 new WeightImageWriter<>(getContext(), TextureResolution.of(getBasisWeightResources().weightMaps), WEIGHTS_PER_PACKED_CHANNEL))
            {
                int fileCount = (getBasisResources().getBasisCount() + WEIGHTS_PER_PACKED_CHANNEL - 1) / WEIGHTS_PER_PACKED_CHANNEL;
                weightImageWriter.saveImages(this, format, outputDirectory,
                    IntStream.range(0, fileCount)
                        .mapToObj(i -> TextureResources.getPackedWeightMapFilename(i, format, filenamePrefix))
                        .toArray(String[]::new));
            }
            catch (IOException e)
            {
                LOG.error("An error occurred saving packed weight maps.", e);
            }
        }
    }

    @Override
    public void saveUnpackedWeightMaps(String format, File outputDirectory, String filenamePrefix)
    {
        if (getBasisWeightResources() != null)
        {
            // Save the unpacked weight maps for reloading the project in the future
            try (WeightImageWriter<ContextType> weightImageWriter =
                     new WeightImageWriter<>(getContext(), TextureResolution.of(getBasisWeightResources().weightMaps), 1))
            {
                weightImageWriter.saveImages(this, format, outputDirectory,
                    IntStream.range(0, getBasisResources().getBasisCount())
                    .mapToObj(i -> TextureResources.getUnpackedWeightMapFilename(i, format, filenamePrefix))
                    .toArray(String[]::new));
            }
            catch (IOException e)
            {
                LOG.error("An error occurred saving unpacked weight maps.", e);
            }
        }
    }

    @Override
    public void saveBasisFunctions(File outputDirectory, String filenameOverride)
    {
        if (getBasisResources() != null)
        {
            getBasisResources().getBasis().save(outputDirectory,
                filenameOverride != null ? filenameOverride : TextureResources.getBasisFunctionsFilename());

            // Save basis image visualization for cards
            try (BasisImageCreator<ContextType> basisImageCreator =
                     new BasisImageCreator<>(getContext(), getBasisResources().getBasisResolution()))
            {
                ViewSet viewSet = Global.state().getIOModel().getLoadedViewSet();
                basisImageCreator.createImages(this, viewSet.getThumbnailImageDirectory());
            }
            catch (IOException e)
            {
                ExceptionHandling.error("Error saving basis image thumbnails", e);
            }
        }
    }

    @Override
    public void deleteBasisMaterial(int materialIndex)
    {
        if (getBasisResources() != null && getBasisWeightResources() != null)
        {
            // Delete the basis materials themselves and the corresponding weight maps
            getBasisResources().deleteBasisMaterial(materialIndex);
            getBasisWeightResources().deleteWeightMap(materialIndex);

            try
            {
                ViewSet viewSet = Global.state().getIOModel().getLoadedViewSet();
                File supportingFilesDir = viewSet.getSupportingFilesDirectory();

                // Refresh thumbnails since names will have shifted (brute force but fine since this shouldn't take long)
                new BasisImageCreator<>(getContext(), 2 * getBasisResources().getBasisResolution() + 1)
                    .createImages(this, viewSet.getThumbnailImageDirectory());

                // Save basis functions and weight maps to prevent inconsistency with thumbnails if the user forgets to save manually.
                saveBasisFunctions(supportingFilesDir);
                saveUnpackedWeightMaps("PNG", supportingFilesDir);
            }
            catch (IOException e)
            {
                LOG.error("An filesystem error occurred while deleting the basis material.", e);
            }
        }
    }
}
