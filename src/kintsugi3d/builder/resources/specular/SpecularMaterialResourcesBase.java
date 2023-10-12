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

package kintsugi3d.builder.resources.specular;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.export.specular.WeightImageCreator;
import kintsugi3d.builder.fit.SpecularFitBase;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.core.SamplerType;
import kintsugi3d.gl.core.Texture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public abstract class SpecularMaterialResourcesBase<ContextType extends Context<ContextType>>
    implements SpecularMaterialResources<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(SpecularMaterialResourcesBase.class);

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

        if (this.getBasisResources() != null)
        {
            this.getBasisResources().useWithShaderProgram(program);
        }

        if (this.getBasisWeightResources() != null)
        {
            this.getBasisWeightResources().useWithShaderProgram(program);
        }
    }

    public void saveDiffuseMap(File outputDirectory)
    {
        try
        {
            if (getDiffuseMap() != null)
            {
                getDiffuseMap().getColorTextureReader().saveToFile("PNG", new File(outputDirectory, "diffuse.png"));
            }
        }
        catch (IOException e)
        {
            log.error("An error occurred saving diffuse map.", e);
        }
    }

    public void saveNormalMap(File outputDirectory)
    {
        try
        {
            if (getNormalMap() != null)
            {
                getNormalMap().getColorTextureReader().saveToFile("PNG", new File(outputDirectory, "normal.png"));
            }
        }
        catch (IOException e)
        {
            log.error("An error occurred saving normal map.", e);
        }
    }

    public void saveSpecularReflectivityMap(File outputDirectory)
    {
        try
        {
            if (getSpecularReflectivityMap() != null)
            {
                getSpecularReflectivityMap().getColorTextureReader().saveToFile("PNG", new File(outputDirectory, "specular.png"));
            }
        }
        catch (IOException e)
        {
            log.error("An error occurred saving specular reflectivity map.", e);
        }
    }

    public void saveSpecularRoughnessMap(File outputDirectory)
    {
        try
        {
            if (getSpecularRoughnessMap() != null)
            {
                getSpecularRoughnessMap().getColorTextureReader().saveToFile("PNG", new File(outputDirectory, "roughness.png"));
            }
        }
        catch (IOException e)
        {
            log.error("An error occurred saving specular roughness map.", e);
        }
    }

    public void saveConstantMap(File outputDirectory)
    {
        try
        {
            if (getConstantMap() != null)
            {
                getConstantMap().getColorTextureReader().saveToFile("PNG", new File(outputDirectory, "constant.png"));
            }
        }
        catch (IOException e)
        {
            log.error("An error occurred saving constant map.", e);
        }
    }

//    public void saveQuadraticMap(File outputDirectory)
//    {
//        try
//        {
//            if (getQuadraticMap() != null)
//            {
//                getQuadraticMap().getColorTextureReader().saveToFile("PNG", new File(outputDirectory, "quadratic.png"));
//            }
//        }
//        catch (IOException e)
//        {
//            log.error("An error occurred saving quadratic map:", e);
//        }
//    }

    public void saveOcclusionMap(File outputDirectory)
    {
        try
        {
            if (getOcclusionMap() != null)
            {
                getOcclusionMap().getColorTextureReader().saveToFile("PNG", new File(outputDirectory, "occlusion.png"));
            }
        }
        catch (IOException e)
        {
            log.error("An error occurred saving constant map.", e);
        }
    }

    public void saveAlbedoMap(File outputDirectory)
    {
        try
        {
            if (getAlbedoMap() != null)
            {
                getAlbedoMap().getColorTextureReader().saveToFile("PNG", new File(outputDirectory, "albedo.png"));
            }
        }
        catch (IOException e)
        {
            log.error("An error occurred saving albedo map.", e);
        }
    }

    public void saveORMMap(File outputDirectory)
    {
        try
        {
            if (getORMMap() != null)
            {
                getORMMap().getColorTextureReader().saveToFile("PNG", new File(outputDirectory, "orm.png"));
            }
        }
        catch (IOException e)
        {
            log.error("An error occurred saving ORM map.", e);
        }
    }

    public void savePackedWeightMaps(File outputDirectory) throws IOException
    {
        // Save the packed weight maps for opening in viewer
        try (WeightImageCreator<ContextType> weightImageCreator =
             new WeightImageCreator<>(getContext(), TextureResolution.of(getBasisWeightResources().weightMaps), 4))
        {
            weightImageCreator.createImages(this, outputDirectory);
        }
        catch (IOException e)
        {
            log.error("An error occurred saving packed weight maps.", e);
        }
    }

    public void saveUnpackedWeightMaps(File outputDirectory) throws IOException
    {
        // Save the unpacked weight maps for reloading the project in the future
        try (WeightImageCreator<ContextType> weightImageCreator =
             new WeightImageCreator<>(getContext(), TextureResolution.of(getBasisWeightResources().weightMaps), 1))
        {
            weightImageCreator.createImages(this, outputDirectory);
        }
        catch (IOException e)
        {
            log.error("An error occurred saving unpacked weight maps.", e);
        }
    }

    /**
     * Saves all resources to the specified output directory
     * @param outputDirectory
     */
    public void saveAll(File outputDirectory) throws IOException
    {
        saveDiffuseMap(outputDirectory);
        saveNormalMap(outputDirectory);
        saveConstantMap(outputDirectory);
        saveOcclusionMap(outputDirectory);
        saveAlbedoMap(outputDirectory);
        saveORMMap(outputDirectory);
        saveSpecularReflectivityMap(outputDirectory);
        saveSpecularRoughnessMap(outputDirectory);
        savePackedWeightMaps(outputDirectory);
        saveUnpackedWeightMaps(outputDirectory);

        // TODO: Save basis functions
    }
}
