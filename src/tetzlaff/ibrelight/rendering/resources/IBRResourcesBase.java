/*
 *  Copyright (c) Michael Tetzlaff 2023
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.rendering.resources;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.gl.material.Material;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.types.AbstractDataTypeFactory;
import tetzlaff.gl.vecmath.IntVector3;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.ReadonlyLoadOptionsModel;
import tetzlaff.ibrelight.core.ViewSet;

abstract class IBRResourcesBase<ContextType extends Context<ContextType>> implements IBRResources<ContextType>
{
    /**
     * A GPU buffer containing light source positions, used only for reflectance fields and illumination-dependent rendering (ignored for light
     * fields).
     * Assumed by convention to be in camera space.
     */
    public final UniformBuffer<ContextType> lightPositionBuffer;

    /**
     * A GPU buffer containing light source intensities, used only for reflectance fields and illumination-dependent rendering (ignored for light
     * fields).
     */
    public final UniformBuffer<ContextType> lightIntensityBuffer;

    /**
     * A GPU buffer containing for every view an index designating the light source position and intensity that should be used for each view.
     */
    public final UniformBuffer<ContextType> lightIndexBuffer;

    /**
     * A diffuse texture map, if one exists.
     */
    public final Texture2D<ContextType> diffuseTexture;

    /**
     * A normal map, if one exists.
     */
    public final Texture2D<ContextType> normalTexture;

    /**
     * A specular reflectivity map, if one exists.
     */
    public final Texture2D<ContextType> specularTexture;

    /**
     * A specular roughness map, if one exists.
     */
    public final Texture2D<ContextType> roughnessTexture;

    /**
     * A GPU buffer containing the weights associated with all the views (determined by the distance from other views).
     */
    public final UniformBuffer<ContextType> cameraWeightBuffer;

    private final ContextType context;
    private final ViewSet viewSet;

    protected final float[] cameraWeights;

    protected IBRResourcesBase(ContextType context, ViewSet viewSet, float[] cameraWeights, Material material,
        ReadonlyLoadOptionsModel loadOptions, LoadingMonitor loadingMonitor)
        throws IOException
    {
        this.context = context;
        this.viewSet = viewSet;
        this.cameraWeights = cameraWeights;

        if (cameraWeights != null)
        {
            cameraWeightBuffer = context.createUniformBuffer()
                .setData(NativeVectorBufferFactory.getInstance().createFromFloatArray(1, viewSet.getCameraPoseCount(), cameraWeights));
        }
        else
        {
            this.cameraWeightBuffer = null;
        }

        // Store the light positions in a uniform buffer
        if (viewSet != null && viewSet.getLightPositionData() != null)
        {
            // Create the uniform buffer
            lightPositionBuffer = context.createUniformBuffer().setData(viewSet.getLightPositionData());
        }
        else
        {
            lightPositionBuffer = null;
        }

        // Store the light intensities in a uniform buffer
        if (viewSet != null && viewSet.getLightIntensityData() != null)
        {
            // Create the uniform buffer
            lightIntensityBuffer = context.createUniformBuffer().setData(viewSet.getLightIntensityData());
        }
        else
        {
            lightIntensityBuffer = null;
        }

        // Store the light indices in a uniform buffer
        if (viewSet != null && viewSet.getLightIndexData() != null)
        {
            lightIndexBuffer = context.createUniformBuffer().setData(viewSet.getLightIndexData());
        }
        else
        {
            lightIndexBuffer = null;
        }

        String diffuseTextureName = null;
        String normalTextureName = null;
        String specularTextureName = null;
        String roughnessTextureName = null;

        if (material != null)
        {
            if (material.getDiffuseMap() != null)
            {
                diffuseTextureName = material.getDiffuseMap().getMapName();
            }

            if (material.getNormalMap() != null)
            {
                normalTextureName = material.getNormalMap().getMapName();
            }

            if (material.getSpecularMap() != null)
            {
                specularTextureName = material.getSpecularMap().getMapName();
            }

            if (material.getRoughnessMap() != null)
            {
                roughnessTextureName = material.getRoughnessMap().getMapName();
            }
        }

        // TODO Use more information from the material.  Currently just pulling texture names.
        if (viewSet != null && viewSet.getGeometryFile() != null)
        {
            String prefix = viewSet.getGeometryFileName().split("\\.")[0];
            diffuseTextureName = diffuseTextureName != null ? diffuseTextureName : prefix + "_Kd.png";
            normalTextureName = normalTextureName != null ? normalTextureName : prefix + "_norm.png";
            specularTextureName = specularTextureName != null ? specularTextureName : prefix + "_Ks.png";
            roughnessTextureName = roughnessTextureName != null ? roughnessTextureName : prefix + "_Pr.png";

            File diffuseFile = new File(viewSet.getGeometryFile().getParentFile(), diffuseTextureName);
            File normalFile = new File(viewSet.getGeometryFile().getParentFile(), normalTextureName);
            File specularFile = new File(viewSet.getGeometryFile().getParentFile(), specularTextureName);
            File roughnessFile = new File(viewSet.getGeometryFile().getParentFile(), roughnessTextureName);

            if (diffuseFile.exists())
            {
                System.out.println("Diffuse texture found.");
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
                    .setLinearFilteringEnabled(true)
                    .createTexture();
            }
            else
            {
                diffuseTexture = null;
            }

            if (normalFile.exists())
            {
                System.out.println("Normal texture found.");
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
                    .setLinearFilteringEnabled(true)
                    .createTexture();
            }
            else
            {
                normalTexture = null;
            }

            if (specularFile.exists())
            {
                System.out.println("Specular texture found.");
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
                    .setLinearFilteringEnabled(true)
                    .createTexture();
            }
            else
            {
                specularTexture = null;
            }

            if (roughnessFile.exists())
            {
                System.out.println("Roughness texture found.");
                ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> roughnessTextureBuilder;

                roughnessTextureBuilder =
                    context.getTextureFactory().build2DColorTextureFromFile(roughnessFile, true,
                        AbstractDataTypeFactory.getInstance().getMultiComponentDataType(NativeDataType.UNSIGNED_BYTE, 3),
                        color -> new IntVector3(
                            (int) Math.max(0, Math.min(255, Math.round(
                                (Math.max(-15.0, Math.min(15.0, (color.getRed() - color.getGreen()) * 30.0 / 255.0)) + 16.0) * 255.0 / 31.0))),
                            color.getGreen(),
                            (int) Math.max(0, Math.min(255, Math.round(
                                (Math.max(-15.0, Math.min(15.0, (color.getBlue() - color.getGreen()) * 30.0 / 255.0)) + 16.0) * 255.0 / 31.0)))));
                roughnessTextureBuilder.setInternalFormat(ColorFormat.RGB8);

                roughnessTexture = roughnessTextureBuilder
                    .setMipmapsEnabled(loadOptions.areMipmapsRequested())
                    .setLinearFilteringEnabled(true)
                    .createTexture();
            }
            else
            {
                roughnessTexture = null;
            }
        }
        else
        {
            diffuseTexture = null;
            normalTexture = null;
            specularTexture = null;
            roughnessTexture = null;
        }
    }

    @Override
    public ContextType getContext()
    {
        return context;
    }

    @Override
    public ViewSet getViewSet()
    {
        return viewSet;
    }

    @Override
    public float getCameraWeight(int index)
    {
        if (this.cameraWeights != null)
        {
            return this.cameraWeights[index];
        }
        else
        {
            throw new IllegalStateException("Camera weights are unavailable.");
        }
    }

    @Override
    public void close()
    {
        if (this.cameraWeightBuffer != null)
        {
            this.cameraWeightBuffer.close();
        }

        if (this.lightPositionBuffer != null)
        {
            this.lightPositionBuffer.close();
        }

        if (this.lightIntensityBuffer != null)
        {
            this.lightIntensityBuffer.close();
        }

        if (this.lightIndexBuffer != null)
        {
            this.lightIndexBuffer.close();
        }

        if (this.diffuseTexture != null)
        {
            this.diffuseTexture.close();
        }

        if (this.normalTexture != null)
        {
            this.normalTexture.close();
        }

        if (this.specularTexture != null)
        {
            this.specularTexture.close();
        }

        if (this.roughnessTexture != null)
        {
            this.roughnessTexture.close();
        }
    }
}
