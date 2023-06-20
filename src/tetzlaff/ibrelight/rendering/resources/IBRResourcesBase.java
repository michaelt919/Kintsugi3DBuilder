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
import tetzlaff.gl.material.*;
import tetzlaff.gl.material.MaterialResources.MaterialLoadOptions;
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
     * A GPU buffer containing the camera poses defining the transformation from object space to camera space for each view.
     * These are necessary to determine view vectors, and for performing projective texture mapping with an image-space implementation.
     */
    public final UniformBuffer<ContextType> cameraPoseBuffer;

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
     * A GPU buffer containing the weights associated with all the views (determined by the distance from other views).
     */
    public final UniformBuffer<ContextType> cameraWeightBuffer;

    private LuminanceMapResources<ContextType> luminanceMapResources;

    private MaterialResources<ContextType> materialResources;

    private final ContextType context;

    private final ViewSet viewSet;

    private final float[] cameraWeights;

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

        // Store the poses in a uniform buffer
        if (viewSet != null && viewSet.getCameraPoseData() != null)
        {
            // Create the uniform buffer
            cameraPoseBuffer = context.createUniformBuffer().setData(viewSet.getCameraPoseData());
        }
        else
        {
            cameraPoseBuffer = null;
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

        // Luminance map texture
        if (viewSet != null && viewSet.hasCustomLuminanceEncoding())
        {
            luminanceMapResources = viewSet.getLuminanceEncoding().createResources(context);
        }
        else
        {
            luminanceMapResources = null;
        }

        if (viewSet != null && viewSet.getGeometryFile() != null)
        {
            if (material != null)
            {
                // Default texture names if not specified by the material
                String prefix = viewSet.getGeometryFileName().split("\\.")[0];

                if (material.getDiffuseMap() == null || material.getDiffuseMap().getMapName() == null)
                {
                    MaterialColorMap diffuseMap = new MaterialColorMap();
                    diffuseMap.setMapName(prefix + "_Kd.png");
                    material.setDiffuseMap(diffuseMap);
                }

                if (material.getNormalMap() == null || material.getNormalMap().getMapName() == null)
                {
                    MaterialTextureMap normalMap = new MaterialTextureMap();
                    normalMap.setMapName(prefix + "_norm.png");
                    material.setNormalMap(normalMap);
                }

                if (material.getSpecularMap() == null || material.getSpecularMap().getMapName() == null)
                {
                    MaterialColorMap specularMap = new MaterialColorMap();
                    specularMap.setMapName(prefix + "_Ks.png");
                    material.setSpecularMap(specularMap);
                }

                if (material.getRoughnessMap() == null || material.getRoughnessMap().getMapName() == null)
                {
                    MaterialScalarMap roughnessMap = new MaterialScalarMap();
                    roughnessMap.setMapName(prefix + "_Pr.png");
                    material.setRoughnessMap(roughnessMap);
                }

                MaterialLoadOptions mtlLoadOptions = new MaterialLoadOptions();
                mtlLoadOptions.setCompressionRequested(loadOptions.isCompressionRequested());
                mtlLoadOptions.setMipmapsRequested(loadOptions.areMipmapsRequested());
                materialResources = material.createResources(context, viewSet.getGeometryFile().getParentFile(), mtlLoadOptions);
            }
            else
            {
                materialResources = MaterialResources.createNull();
            }
        }
        else
        {
            materialResources = MaterialResources.createNull();
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

    public LuminanceMapResources<ContextType> getLuminanceMapResources()
    {
        return luminanceMapResources;
    }

    /**
     * Diffuse, normal, specular, roughness maps
     */
    public MaterialResources<ContextType> getMaterialResources()
    {
        return materialResources;
    }

    /**
     * Refresh the luminance map textures using the current values in the view set.
     */
    public void updateLuminanceMap()
    {
        luminanceMapResources.update(viewSet.hasCustomLuminanceEncoding() ? viewSet.getLuminanceEncoding() : null);
    }

    @Override
    public void close()
    {
        if (this.cameraWeightBuffer != null)
        {
            this.cameraWeightBuffer.close();
        }

        if (this.cameraPoseBuffer != null)
        {
            this.cameraPoseBuffer.close();
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

        if (this.materialResources != null)
        {
            this.materialResources.close();
            this.materialResources = null;
        }

        if (this.luminanceMapResources != null)
        {
            this.luminanceMapResources.close();
            this.luminanceMapResources = null;
        }
    }
}
