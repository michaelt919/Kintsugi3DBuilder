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

package kintsugi3d.gl.material;

import kintsugi3d.builder.resources.specular.SpecularResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.core.*;
import kintsugi3d.util.ImageFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

// TODO Use more information from the material.  Currently just pulling texture names.
// TODO use glTF instead of / in addition to OBJ material?
public class GenericMaterialResources<ContextType extends Context<ContextType>> implements SpecularResources<ContextType>
{
    private static final Logger log = LoggerFactory.getLogger(GenericMaterialResources.class);

    private final ContextType context;

    private Texture2D<ContextType> albedoTexture;

    private Texture2D<ContextType> normalTexture;

    private Texture2D<ContextType> specularTexture;

    private Texture2D<ContextType> roughnessTexture;

    private Texture2D<ContextType> occlusionTexture;

    @Override
    public ContextType getContext()
    {
        return context;
    }

    /**
     * A albedo texture map, if one exists.
     */
    @Override
    public Texture2D<ContextType> getAlbedoMap()
    {
        return albedoTexture;
    }

    /**
     * A normal map, if one exists.
     */
    @Override
    public Texture2D<ContextType> getNormalMap()
    {
        return normalTexture;
    }

    /**
     * A specular reflectivity map, if one exists.
     */
    @Override
    public Texture2D<ContextType> getSpecularReflectivityMap()
    {
        return specularTexture;
    }

    /**
     * A specular roughness map, if one exists.
     */
    @Override
    public Texture2D<ContextType> getSpecularRoughnessMap()
    {
        return roughnessTexture;
    }

    /**
     * An ambient occlusion map, if one exists.
     */
    @Override
    public Texture2D<ContextType> getOcclusionMap()
    {
        return occlusionTexture;
    }

    public static <ContextType extends Context<ContextType>> GenericMaterialResources<ContextType> createNull()
    {
        return new GenericMaterialResources<>();
    }

    GenericMaterialResources()
    {
        albedoTexture = null;
        normalTexture = null;
        specularTexture = null;
        roughnessTexture = null;
        occlusionTexture = null;
        context = null;
    }

    GenericMaterialResources(ContextType context, ReadonlyGenericMaterial material, File textureDirectory, TextureLoadOptions loadOptions) throws IOException
    {
        this.context = context;

        ImageFinder finder = ImageFinder.getInstance();

        try
        {
            File diffuseFile = finder.findImageFile(new File(textureDirectory, material.getDiffuseMap().getMapName()));
            log.info("Diffuse texture found.");
            albedoTexture = context.getTextureFactory().build2DColorTextureFromFile(diffuseFile, true)
                .setInternalFormat(ColorFormat.RGB8)
                .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                .setLinearFilteringEnabled(loadOptions.isLinearFilteringRequested())
                .createTexture();
        }
        catch (FileNotFoundException e)
        {
            albedoTexture = null;
        }

        try
        {
            File normalFile = finder.findImageFile(new File(textureDirectory, material.getNormalMap().getMapName()));
            log.info("Normal texture found.");
            normalTexture = context.getTextureFactory().build2DColorTextureFromFile(normalFile, true)
                .setInternalFormat(ColorFormat.RG8)
                .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                .setLinearFilteringEnabled(loadOptions.isLinearFilteringRequested())
                .createTexture();
        }
        catch (FileNotFoundException e)
        {
            normalTexture = null;
        }

        try
        {
            File specularFile = finder.findImageFile(new File(textureDirectory, material.getSpecularMap().getMapName()));
            log.info("Specular texture found.");
            specularTexture = context.getTextureFactory().build2DColorTextureFromFile(specularFile, true)
                .setInternalFormat(ColorFormat.RGB8)
                .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                .setLinearFilteringEnabled(loadOptions.isLinearFilteringRequested())
                .createTexture();
        }
        catch(FileNotFoundException e)
        {
            specularTexture = null;
        }

        try
        {
            File roughnessFile = finder.findImageFile(new File(textureDirectory, material.getRoughnessMap().getMapName()));
            log.info("Roughness texture found.");
            roughnessTexture = context.getTextureFactory().build2DColorTextureFromFile(roughnessFile, true)
                .setInternalFormat(ColorFormat.RGB8)
                .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                .setLinearFilteringEnabled(loadOptions.isLinearFilteringRequested())
                .createTexture();
        }
        catch (FileNotFoundException e)
        {
            roughnessTexture = null;
        }

        try
        {
            File occlusionFile = finder.findImageFile(new File(textureDirectory, material.getAmbientOcclusionMap().getMapName()));
            log.info("Occlusion texture found.");
            occlusionTexture = context.getTextureFactory().build2DColorTextureFromFile(occlusionFile, true)
                .setInternalFormat(ColorFormat.R8)
                .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                .setLinearFilteringEnabled(loadOptions.isLinearFilteringRequested())
                .createTexture();
        }
        catch (FileNotFoundException e)
        {
            occlusionTexture = null;
        }
    }

    public void setupShaderProgram(Program<ContextType> program)
    {
        if (this.normalTexture == null)
        {
            program.setTexture("normalMap", program.getContext().getTextureFactory().getNullTexture(SamplerType.FLOAT_2D));
        }
        else
        {
            program.setTexture("normalMap", this.normalTexture);
        }

        if (this.albedoTexture == null)
        {
            program.setTexture("diffuseMap", program.getContext().getTextureFactory().getNullTexture(SamplerType.FLOAT_2D));
        }
        else
        {
            program.setTexture("diffuseMap", this.albedoTexture);
        }

        if (this.specularTexture == null)
        {
            program.setTexture("specularMap", program.getContext().getTextureFactory().getNullTexture(SamplerType.FLOAT_2D));
        }
        else
        {
            program.setTexture("specularMap", this.specularTexture);
        }

        if (this.roughnessTexture == null)
        {
            program.setTexture("roughnessMap", program.getContext().getTextureFactory().getNullTexture(SamplerType.FLOAT_2D));
        }
        else
        {
            program.setTexture("roughnessMap", this.roughnessTexture);
        }

        if (this.occlusionTexture == null)
        {
            program.setTexture("occlusionMap", program.getContext().getTextureFactory().getNullTexture(SamplerType.FLOAT_2D));
        }
        else
        {
            program.setTexture("occlusionMap", this.occlusionTexture);
        }
    }

    @Override
    public void close()
    {
        if (this.albedoTexture != null)
        {
            this.albedoTexture.close();
            this.albedoTexture = null;
        }

        if (this.normalTexture != null)
        {
            this.normalTexture.close();
            this.normalTexture = null;
        }

        if (this.specularTexture != null)
        {
            this.specularTexture.close();
            this.specularTexture = null;
        }

        if (this.roughnessTexture != null)
        {
            this.roughnessTexture.close();
            this.roughnessTexture = null;
        }

        if (this.occlusionTexture != null)
        {
            this.occlusionTexture.close();
            this.occlusionTexture = null;
        }
    }
}
