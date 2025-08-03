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

package kintsugi3d.builder.resources.project;

import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.fit.SpecularFitFinal;
import kintsugi3d.builder.resources.project.specular.GenericMaterialResources;
import kintsugi3d.builder.resources.project.specular.SpecularMaterialResources;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.core.UniformBuffer;
import kintsugi3d.gl.geometry.GeometryResources;
import kintsugi3d.gl.geometry.ReadonlyVertexGeometry;
import kintsugi3d.gl.geometry.VertexGeometry;
import kintsugi3d.gl.material.*;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.vecmath.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

final class GraphicsResourcesCommon<ContextType extends Context<ContextType>>
{
    private static final Logger log = LoggerFactory.getLogger(GraphicsResourcesCommon.class);
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

    /**
     * Contains the VBOs for positions, tex-coords, normals, and tangents
     */
    private final GeometryResources<ContextType> geometryResources;

    private SpecularMaterialResources<ContextType> specularMaterialResources;

    private final LuminanceMapResources<ContextType> luminanceMapResources;

    private final ContextType context;

    private final ViewSet viewSet;

    private final float[] cameraWeights;

    GraphicsResourcesCommon(ContextType context, ViewSet viewSet, VertexGeometry geometry, TextureLoadOptions loadOptions)
    {
        this.context = context;
        this.viewSet = viewSet;

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
            luminanceMapResources = LuminanceMapResources.createNull(context);
        }

        if (geometry != null)
        {
            geometryResources = geometry.createGraphicsResources(context);

            if (viewSet != null)
            {
                this.cameraWeights = computeCameraWeights(viewSet, geometry);

                this.cameraWeightBuffer = context.createUniformBuffer()
                        .setData(NativeVectorBufferFactory.getInstance().createFromFloatArray(
                                1, viewSet.getCameraPoseCount(), this.cameraWeights));

                Material material = geometry.getMaterial();

                if (viewSet.getSupportingFilesFilePath() != null) // Load texture fit from previous session
                {
                    SpecularMaterialResources<ContextType> loadedFit;
                    try
                    {
                        loadedFit = SpecularFitFinal.loadFromPriorSolution(context, viewSet.getSupportingFilesFilePath());
                    }
                    catch (IOException e)
                    {
                        log.error("An error occured while loading the previous specular fit (textures).", e);
                        loadedFit = SpecularMaterialResources.makeNull(context);
                    }
                    specularMaterialResources = loadedFit;
                }
                else if (material != null && viewSet.getGeometryFile() != null /* need an actual file path to load the textures */)
                {
                    // Load material from OBJ file

                    // Default texture names if not specified by the material
                    String prefix = viewSet.getGeometryFileName().split("\\.")[0];

                    if (material.getDiffuseMap() == null || material.getDiffuseMap().getMapName() == null)
                    {
                        MaterialColorMap diffuseMap = new MaterialColorMap();
                        diffuseMap.setMapName(prefix + ".png");
                        material.setDiffuseMap(diffuseMap);
                    }

                    if (material.getNormalMap() == null || material.getNormalMap().getMapName() == null)
                    {
                        MaterialTextureMap normalMap = new MaterialTextureMap();
                        normalMap.setMapName(prefix + "_normal.png");
                        material.setNormalMap(normalMap);
                    }

                    if (material.getAmbientOcclusionMap() == null || material.getAmbientOcclusionMap().getMapName() == null)
                    {
                        MaterialScalarMap aoMap = new MaterialScalarMap();
                        aoMap.setMapName(prefix + "_occlusion.png");
                        material.setAmbientOcclusionMap(aoMap);
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

                    TextureLoadOptions mtlLoadOptions = new TextureLoadOptions();
                    mtlLoadOptions.setCompressionRequested(loadOptions.isCompressionRequested());
                    mtlLoadOptions.setMipmapsRequested(loadOptions.areMipmapsRequested());
                    this.specularMaterialResources = GenericMaterialResources.wrap(
                        () ->
                        {
                            try
                            {
                                return material.createResources(context, viewSet.getGeometryFile().getParentFile(), mtlLoadOptions);
                            }
                            catch (IOException e)
                            {
                                log.error("An error occured while loading material resources (textures)", e);
                                return MaterialResources.createNull();
                            }
                        });
                }
                else
                {
                    // Neither a texture fit or an OBJ material: material is null
                    this.specularMaterialResources = SpecularMaterialResources.makeNull(context);
                }
            }
            else
            {
                this.cameraWeights = null;
                this.cameraWeightBuffer = null;
                this.specularMaterialResources = SpecularMaterialResources.makeNull(context);
            }
        }
        else
        {
            this.geometryResources = GeometryResources.createNullResources();
            this.cameraWeights = null;
            this.cameraWeightBuffer = null;
            this.specularMaterialResources = SpecularMaterialResources.makeNull(context);
        }
    }

    private static float[] computeCameraWeights(ReadonlyViewSet viewSet, ReadonlyVertexGeometry geometry)
    {
        float[] cameraWeights = new float[viewSet.getCameraPoseCount()];

        Vector3[] viewDirections = IntStream.range(0, viewSet.getCameraPoseCount())
                .mapToObj(i -> viewSet.getCameraPoseInverse(i).getColumn(3).getXYZ()
                        .minus(geometry.getCentroid()).normalized())
                .toArray(Vector3[]::new);

        int[] totals = new int[viewSet.getCameraPoseCount()];
        int targetSampleCount = viewSet.getCameraPoseCount() * 256;
        double densityFactor = Math.sqrt(Math.PI * targetSampleCount);
        int sampleRows = (int)Math.ceil(densityFactor / 2) + 1;

        // Find the view with the greatest distance from any other view.
        // Directions that are further from any view than distance will be ignored in the view weight calculation.
        double maxMinDistance = 0.0;
        for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
        {
            double minDistance = Double.MAX_VALUE;
            for (int j = 0; j < viewSet.getCameraPoseCount(); j++)
            {
                if (i != j)
                {
                    minDistance = Math.min(minDistance, Math.acos(Math.max(-1.0, Math.min(1.0f, viewDirections[i].dot(viewDirections[j])))));
                }
            }
            maxMinDistance = Math.max(maxMinDistance, minDistance);
        }

        int actualSampleCount = 0;

        for (int i = 0; i < sampleRows; i++)
        {
            double r = Math.sin(Math.PI * (double)i / (double)(sampleRows-1));
            int sampleColumns = Math.max(1, (int)Math.ceil(densityFactor * r));

            for (int j = 0; j < sampleColumns; j++)
            {
                Vector3 sampleDirection = new Vector3(
                        (float)(r * Math.cos(2 * Math.PI * (double)j / (double)sampleColumns)),
                        (float) Math.cos(Math.PI * (double)i / (double)(sampleRows-1)),
                        (float)(r * Math.sin(2 * Math.PI * (double)j / (double)sampleColumns)));

                double minDistance = maxMinDistance;
                int minIndex = -1;
                for (int k = 0; k < viewSet.getCameraPoseCount(); k++)
                {
                    double distance = Math.acos(Math.max(-1.0, Math.min(1.0f, sampleDirection.dot(viewDirections[k]))));
                    if (distance < minDistance)
                    {
                        minDistance = distance;
                        minIndex = k;
                    }
                }

                if (minIndex >= 0)
                {
                    totals[minIndex]++;
                }

                actualSampleCount++;
            }
        }

        log.info("View weights:");

        for (int k = 0; k < viewSet.getCameraPoseCount(); k++)
        {
            cameraWeights[k] = (float)totals[k] / (float)actualSampleCount;
            log.info(viewSet.getImageFileName(k) + '\t' + cameraWeights[k]);
        }

        return cameraWeights;
    }

    public ContextType getContext()
    {
        return context;
    }

    public ViewSet getViewSet()
    {
        return viewSet;
    }

    public float getCameraWeight(int index)
    {
        if (this.cameraWeights != null)
        {
            return this.getCameraWeight(index);
        }
        else
        {
            throw new IllegalStateException("Camera weights are unavailable.");
        }
    }

    public List<Float> getCameraWeights()
    {
        if (this.cameraWeights != null)
        {
            return Collections.unmodifiableList(new AbstractList<>()
            {
                @Override
                public int size()
                {
                    return cameraWeights.length;
                }

                @Override
                public Float get(int index)
                {
                    return cameraWeights[index];
                }
            });
        }
        else
        {
            throw new IllegalStateException("Camera weights are unavailable.");
        }
    }

    /**
     *
     * @return Vertex buffers for positions, tex coords, normals, tangents
     */
    public GeometryResources<ContextType> getGeometryResources()
    {
        return geometryResources;
    }

    /**
     * Diffuse, normal, specular, roughness maps
     */
    public SpecularMaterialResources<ContextType> getSpecularMaterialResources()
    {
        return specularMaterialResources;
    }

    public LuminanceMapResources<ContextType> getLuminanceMapResources()
    {
        return luminanceMapResources;
    }

    /**
     * Refresh the luminance map textures using the current values in the view set.
     */
    public void updateLuminanceMap()
    {
        luminanceMapResources.update(viewSet.hasCustomLuminanceEncoding() ? viewSet.getLuminanceEncoding() : null);
    }

    /**
     * Refresh the light data in the uniform buffers using the current values in the view set.
     */
    public void updateLightData()
    {
        // Store the light positions in a uniform buffer
        if (lightPositionBuffer != null && getViewSet().getLightPositionData() != null)
        {
            // Create the uniform buffer
            lightPositionBuffer.setData(getViewSet().getLightPositionData());
        }

        // Store the light positions in a uniform buffer
        if (lightIntensityBuffer != null && getViewSet().getLightIntensityData() != null)
        {
            // Create the uniform buffer
            lightIntensityBuffer.setData(getViewSet().getLightIntensityData());
        }
    }

    public void replaceSpecularMaterialResources(SpecularMaterialResources<ContextType> specularMaterialResources)
    {
        this.specularMaterialResources.close();

        this.specularMaterialResources = specularMaterialResources == null ? SpecularMaterialResources.makeNull(context) : specularMaterialResources;
    }

    /**
     * Gets a shader program builder with the following preprocessor defines automatically injected based on the
     * characteristics of this instance:
     * <ul>
     *     <li>CAMERA_POSE_COUNT</li>
     *     <li>LIGHT_COUNT</li>
     *     <li>LUMINANCE_MAP_ENABLED</li>
     *     <li>INVERSE_LUMINANCE_MAP_ENABLED</li>
     *     <li>INFINITE_LIGHT_SOURCES</li>
     *     <li>IMAGE_BASED_RENDERING_ENABLED</li>
     *     <li>DIFFUSE_TEXTURE_ENABLED</li>
     *     <li>SPECULAR_TEXTURE_ENABLED</li>
     *     <li>ROUGHNESS_TEXTURE_ENABLED</li>
     *     <li>NORMAL_TEXTURE_ENABLED</li>
     * </ul>
     *
     * @return A program builder with all of the above preprocessor defines specified, ready to have the
     * vertex and fragment shaders added as well as any additional application-specific preprocessor definitions.
     */
    public ProgramBuilder<ContextType> getShaderProgramBuilder()
    {
        boolean basisEnabled = specularMaterialResources.getBasisResources() != null
            && specularMaterialResources.getBasisWeightResources() != null;

        ProgramBuilder<ContextType> builder = context.getShaderProgramBuilder()
            .define("CAMERA_POSE_COUNT", viewSet.getCameraPoseCount())
            .define("LIGHT_COUNT", viewSet.getLightCount())
            .define("INFINITE_LIGHT_SOURCES", viewSet.areLightSourcesInfinite())
            .define("LUMINANCE_MAP_ENABLED", luminanceMapResources != null && luminanceMapResources.getLuminanceMap() != null)
            .define("INVERSE_LUMINANCE_MAP_ENABLED", luminanceMapResources != null && luminanceMapResources.getInverseLuminanceMap() != null)
            .define("DIFFUSE_TEXTURE_ENABLED", specularMaterialResources.getDiffuseMap() != null)
            .define("NORMAL_TEXTURE_ENABLED", specularMaterialResources.getNormalMap() != null)
            .define("SPECULAR_TEXTURE_ENABLED", specularMaterialResources.getSpecularReflectivityMap() != null)
            .define("ROUGHNESS_TEXTURE_ENABLED", specularMaterialResources.getSpecularRoughnessMap() != null)
            .define("OCCLUSION_TEXTURE_ENABLED", specularMaterialResources.getOcclusionMap() != null)
            .define("ALBEDO_TEXTURE_ENABLED", specularMaterialResources.getAlbedoMap() != null)
            .define("ORM_TEXTURE_ENABLED", specularMaterialResources.getORMMap() != null)
            .define("BASIS_ENABLED", basisEnabled);

        if (basisEnabled)
        {
            builder
                .define("BASIS_COUNT", specularMaterialResources.getBasisResources().getBasisCount())
                .define("BASIS_RESOLUTION", specularMaterialResources.getBasisResources().getBasisResolution());
        }

        return builder;


    }

    public void setupShaderProgram(Program<ContextType> program)
    {
        if (this.cameraWeightBuffer != null)
        {
            program.setUniformBuffer("CameraWeights", this.cameraWeightBuffer);
        }

        if (this.cameraPoseBuffer != null)
        {
            program.setUniformBuffer("CameraPoses", this.cameraPoseBuffer);
        }

        if (this.lightPositionBuffer != null && this.lightIntensityBuffer != null && this.lightIndexBuffer != null)
        {
            program.setUniformBuffer("LightPositions", this.lightPositionBuffer);
            program.setUniformBuffer("LightIntensities", this.lightIntensityBuffer);
            program.setUniformBuffer("LightIndices", this.lightIndexBuffer);
        }

        getLuminanceMapResources().setupShaderProgram(program);
        getSpecularMaterialResources().setupShaderProgram(program);

        program.setUniform("occlusionBias", viewSet.getProjectSettings().getFloat("occlusionBias"));
        program.setUniform("edgeProximityMargin", viewSet.getProjectSettings().getFloat("edgeProximityMargin"));
    }

    public void close()
    {
        this.cameraWeightBuffer.close();
        this.cameraPoseBuffer.close();
        this.lightPositionBuffer.close();
        this.lightIntensityBuffer.close();
        this.lightIndexBuffer.close();
        this.geometryResources.close();
        this.specularMaterialResources.close();
        this.luminanceMapResources.close();
    }
}
