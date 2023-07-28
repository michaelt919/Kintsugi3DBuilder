/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.gl.material;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.util.ImageFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

// TODO Use more information from the material.  Currently just pulling texture names.
// TODO use glTF instead of / in addition to OBJ material?
public class MaterialResources<ContextType extends Context<ContextType>> implements Resource
{
    private static final Logger log = LoggerFactory.getLogger(MaterialResources.class);
    private Texture2D<ContextType> diffuseTexture;

    private Texture2D<ContextType> normalTexture;

    private Texture2D<ContextType> specularTexture;

    private Texture2D<ContextType> roughnessTexture;

    private Texture2D<ContextType> occlusionTexture;

    /**
         * A diffuse texture map, if one exists.
         */
    public Texture2D<ContextType> getDiffuseTexture()
    {
        return diffuseTexture;
    }

    /**
     * A normal map, if one exists.
     */
    public Texture2D<ContextType> getNormalTexture()
    {
        return normalTexture;
    }

    /**
     * A specular reflectivity map, if one exists.
     */
    public Texture2D<ContextType> getSpecularTexture()
    {
        return specularTexture;
    }

    /**
     * A specular roughness map, if one exists.
     */
    public Texture2D<ContextType> getRoughnessTexture()
    {
        return roughnessTexture;
    }

    /**
     * An ambient occlusion map, if one exists.
     */
    public Texture2D<ContextType> getOcclusionTexture()
    {
        return occlusionTexture;
    }

    public static <ContextType extends Context<ContextType>>  MaterialResources<ContextType> createNull()
    {
        return new MaterialResources<>();
    }

    MaterialResources()
    {
        diffuseTexture = null;
        normalTexture = null;
        specularTexture = null;
        roughnessTexture = null;
        occlusionTexture = null;
    }

    MaterialResources(ContextType context, ReadonlyMaterial material, File textureDirectory, TextureLoadOptions loadOptions) throws IOException
    {
        ImageFinder finder = ImageFinder.getInstance();

        try
        {
            File diffuseFile = finder.findImageFile(new File(textureDirectory, material.getDiffuseMap().getMapName()));

            log.info("Diffuse texture found.");
            ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> diffuseTextureBuilder =
                    context.getTextureFactory().build2DColorTextureFromFile(diffuseFile, true);

            if (loadOptions.isCompressionRequested())
            {
                diffuseTextureBuilder.setInternalFormat(CompressionFormat.RGB_4BPP);
            }
            else
            {
                diffuseTextureBuilder.setInternalFormat(ColorFormat.RGB8);
            }

            diffuseTexture = diffuseTextureBuilder
                    .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                    .setLinearFilteringEnabled(loadOptions.isLinearFilteringRequested())
                    .createTexture();
        }
        catch (FileNotFoundException e)
        {
            diffuseTexture = null;
        }

        try
        {
            File normalFile = finder.findImageFile(new File(textureDirectory, material.getNormalMap().getMapName()));
            log.info("Normal texture found.");
            ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> normalTextureBuilder =
                    context.getTextureFactory().build2DColorTextureFromFile(normalFile, true);

            if (loadOptions.isCompressionRequested())
            {
                normalTextureBuilder.setInternalFormat(CompressionFormat.RED_4BPP_GREEN_4BPP);
            }
            else
            {
                normalTextureBuilder.setInternalFormat(ColorFormat.RG8);
            }

            normalTexture = normalTextureBuilder
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
            ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> specularTextureBuilder =
                    context.getTextureFactory().build2DColorTextureFromFile(specularFile, true);
            if (loadOptions.isCompressionRequested())
            {
                specularTextureBuilder.setInternalFormat(CompressionFormat.RGB_4BPP);
            }
            else
            {
                specularTextureBuilder.setInternalFormat(ColorFormat.RGB8);
            }

            specularTexture = specularTextureBuilder
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
            ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> roughnessTextureBuilder
                = context.getTextureFactory().build2DColorTextureFromFile(roughnessFile, true);
            if (loadOptions.isCompressionRequested())
            {
                roughnessTextureBuilder.setInternalFormat(CompressionFormat.RGB_4BPP);
            }
            else
            {
                roughnessTextureBuilder.setInternalFormat(ColorFormat.RGB8);
            }

            roughnessTexture = roughnessTextureBuilder
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
            ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> occlusionTextureBuilder
                = context.getTextureFactory().build2DColorTextureFromFile(occlusionFile, true);
            if (loadOptions.isCompressionRequested())
            {
                occlusionTextureBuilder.setInternalFormat(CompressionFormat.RED_4BPP);
            }
            else
            {
                occlusionTextureBuilder.setInternalFormat(ColorFormat.R8);
            }

            occlusionTexture = occlusionTextureBuilder
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

        if (this.diffuseTexture == null)
        {
            program.setTexture("diffuseMap", program.getContext().getTextureFactory().getNullTexture(SamplerType.FLOAT_2D));
        }
        else
        {
            program.setTexture("diffuseMap", this.diffuseTexture);
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
        if (this.diffuseTexture != null)
        {
            this.diffuseTexture.close();
            this.diffuseTexture = null;
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
