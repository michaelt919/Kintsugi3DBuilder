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

package kintsugi3d.builder.resources.project.specular;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.io.specular.WeightImageWriter;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.core.SamplerType;
import kintsugi3d.gl.core.Texture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.function.IntFunction;

public abstract class SpecularMaterialResourcesBase<ContextType extends Context<ContextType>>
    implements SpecularMaterialResources<ContextType>
{
    private static final Logger LOG = LoggerFactory.getLogger(SpecularMaterialResourcesBase.class);

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
        useTextureSafe(program, "diffuseMap", this.getDiffuseMap());
        useTextureSafe(program, "constantMap", this.getConstantMap());
        useTextureSafe(program, "normalMap", this.getNormalMap());
        useTextureSafe(program, "specularMap", this.getSpecularReflectivityMap());
        useTextureSafe(program, "roughnessMap", this.getSpecularRoughnessMap());
        useTextureSafe(program, "occlusionMap", this.getOcclusionMap());
        useTextureSafe(program, "albedoMap", this.getAlbedoMap());
        useTextureSafe(program, "ormMap", this.getORMMap());

        for (var entry : this.getMetadataMaps().entrySet())
        {
            useTextureSafe(program, "metadataMap_" + entry.getKey(), entry.getValue());
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
    public void saveDiffuseMap(String format, File outputDirectory, String filenameOverride)
    {
        try
        {
            if (getDiffuseMap() != null)
            {
                getDiffuseMap().getColorTextureReader().saveToFile(format, new File(outputDirectory,
                    filenameOverride != null ? filenameOverride : String.format("diffuse.%s", format.toLowerCase(Locale.ROOT))));
            }
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving diffuse map.", e);
        }
    }

    @Override
    public void saveNormalMap(String format, File outputDirectory, String filenameOverride)
    {
        try
        {
            if (getNormalMap() != null)
            {
                getNormalMap().getColorTextureReader().saveToFile(format, new File(outputDirectory,
                    filenameOverride != null ? filenameOverride : String.format("normal.%s", format.toLowerCase(Locale.ROOT))));
            }
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving normal map.", e);
        }
    }

    @Override
    public void saveSpecularReflectivityMap(String format, File outputDirectory, String filenameOverride)
    {
        try
        {
            if (getSpecularReflectivityMap() != null)
            {
                getSpecularReflectivityMap().getColorTextureReader().saveToFile(format, new File(outputDirectory,
                    filenameOverride != null ? filenameOverride : String.format("specular.%s", format.toLowerCase(Locale.ROOT))));
            }
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving specular reflectivity map.", e);
        }
    }

    @Override
    public void saveSpecularRoughnessMap(String format, File outputDirectory, String filenameOverride)
    {
        try
        {
            if (getSpecularRoughnessMap() != null)
            {
                getSpecularRoughnessMap().getColorTextureReader().saveToFile(format, new File(outputDirectory,
                    filenameOverride != null ? filenameOverride : String.format("roughness.%s", format.toLowerCase(Locale.ROOT))));
            }
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving specular roughness map.", e);
        }
    }

    @Override
    public void saveConstantMap(String format, File outputDirectory, String filenameOverride)
    {
        try
        {
            if (getConstantMap() != null)
            {
                getConstantMap().getColorTextureReader().saveToFile(format, new File(outputDirectory,
                    filenameOverride != null ? filenameOverride : String.format("constant.%s", format.toLowerCase(Locale.ROOT))));
            }
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving constant map.", e);
        }
    }

    @Override
    public void saveOcclusionMap(String format, File outputDirectory, String filenameOverride)
    {
        try
        {
            if (getOcclusionMap() != null)
            {
                getOcclusionMap().getColorTextureReader().saveToFile(format, new File(outputDirectory,
                    filenameOverride != null ? filenameOverride : String.format("occlusion.%s", format.toLowerCase(Locale.ROOT))));
            }
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving constant map.", e);
        }
    }

    @Override
    public void saveAlbedoMap(String format, File outputDirectory, String filenameOverride)
    {
        try
        {
            if (getAlbedoMap() != null)
            {
                getAlbedoMap().getColorTextureReader().saveToFile(format, new File(outputDirectory,
                    filenameOverride != null ? filenameOverride : String.format("albedo.%s", format.toLowerCase(Locale.ROOT))));
            }
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving albedo map.", e);
        }
    }

    @Override
    public void saveORMMap(String format, File outputDirectory, String filenameOverride)
    {
        try
        {
            if (getORMMap() != null)
            {
                getORMMap().getColorTextureReader().saveToFile(format, new File(outputDirectory,
                    filenameOverride != null ? filenameOverride : String.format("orm.%s", format.toLowerCase(Locale.ROOT))));
            }
        }
        catch (IOException e)
        {
            LOG.error("An error occurred saving ORM map.", e);
        }
    }

    @Override
    public void savePackedWeightMaps(String format, File outputDirectory, IntFunction<String> filenameOverrides)
    {
        if (getBasisWeightResources() != null)
        {
            // Save the packed weight maps for opening in viewer
            try (WeightImageWriter<ContextType> weightImageWriter =
                     new WeightImageWriter<>(getContext(), TextureResolution.of(getBasisWeightResources().weightMaps), 4))
            {
                weightImageWriter.saveImages(this, format, outputDirectory, filenameOverrides);
            }
            catch (IOException e)
            {
                LOG.error("An error occurred saving packed weight maps.", e);
            }
        }
    }

    @Override
    public void saveUnpackedWeightMaps(String format, File outputDirectory, IntFunction<String> filenameOverrides)
    {
        if (getBasisWeightResources() != null)
        {
            // Save the unpacked weight maps for reloading the project in the future
            try (WeightImageWriter<ContextType> weightImageWriter =
                     new WeightImageWriter<>(getContext(), TextureResolution.of(getBasisWeightResources().weightMaps), 1))
            {
                weightImageWriter.saveImages(this, format, outputDirectory, filenameOverrides);
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
            getBasisResources().getBasis().save(outputDirectory, filenameOverride);
        }
    }

    @Override
    public void saveMetadataMaps(String format, File outputDirectory, String filenamePrefix)
    {
        var metadataMaps = getMetadataMaps();
        for (var entry : metadataMaps.entrySet())
        {
            try
            {
                entry.getValue().getColorTextureReader().saveToFile(format, new File(outputDirectory,
                    String.format("%s%s.%s", filenamePrefix, entry.getKey(), format.toLowerCase(Locale.ROOT))));
            }
            catch (IOException e)
            {
                LOG.error("An error occurred saving metadata map: {}", entry.getKey(), e);
            }
        }
    }
}
