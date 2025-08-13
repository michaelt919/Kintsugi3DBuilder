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

package kintsugi3d.builder.fit;

import java.io.File;
import java.io.IOException;
import java.util.*;

import kintsugi3d.builder.core.Projection;
import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.fit.settings.ReconstructionSettings;
import kintsugi3d.builder.metrics.ColorAppearanceRMSE;
import kintsugi3d.builder.rendering.ImageReconstruction;
import kintsugi3d.builder.rendering.ReconstructionView;
import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FinalReconstruction<ContextType extends Context<ContextType>>
{
    private static final Logger LOG = LoggerFactory.getLogger(FinalReconstruction.class);
    private final ReadonlyIBRResources<ContextType> resources;
    private final ReconstructionSettings reconstructionSettings;

    private final int imageWidth;
    private final int imageHeight;

    public FinalReconstruction(ReadonlyIBRResources<ContextType> resources, TextureResolution textureResolution, ReconstructionSettings reconstructionSettings)
    {
        this.resources = resources;
        this.reconstructionSettings = reconstructionSettings;

        // Calculate reasonable image resolution for reconstructed images (supplemental output)
        Projection defaultProj = resources.getViewSet().getCameraProjection(resources.getViewSet().getCameraProjectionIndex(0));
        if (defaultProj.getAspectRatio() < 1.0)
        {
            imageWidth = textureResolution.width;
            imageHeight = Math.round(imageWidth / defaultProj.getAspectRatio());
        }
        else
        {
            imageHeight = textureResolution.height;
            imageWidth = Math.round(imageHeight * defaultProj.getAspectRatio());
        }
    }

    public List<Map<String, ColorAppearanceRMSE>> reconstruct(SpecularMaterialResources<ContextType> specularFit,
            Map<String, ProgramBuilder<ContextType>> reconstructionProgramBuilders,
            ProgramBuilder<ContextType> incidentRadianceProgramBuilder,
            File debugDirectory, File groundTruthDirectory)
    {
        if (debugDirectory != null)
        {
            for (String reconstructionType : reconstructionProgramBuilders.keySet())
            {
                // Create directory for reconstructions from basis functions
                new File(debugDirectory, reconstructionType).mkdirs();
            }
        }

        if (groundTruthDirectory != null)
        {
            groundTruthDirectory.mkdirs();
        }

        // Use the same view set as for fitting if another wasn't specified for reconstruction.
        ReadonlyViewSet reconstructionViewSet;
        if (reconstructionSettings.getReconstructionViewSet() != null)
        {
            reconstructionViewSet = reconstructionSettings.getReconstructionViewSet();
        }
        else
        {
            reconstructionViewSet = resources.getViewSet();
        }

        try (@SuppressWarnings("MismatchedQueryAndUpdateOfCollection") ResourceMap<String, ProgramObject<ContextType>> programMap
                = new ResourceMap<>(reconstructionProgramBuilders.size());
            ImageReconstruction<ContextType> reconstruction = new ImageReconstruction<>(
                reconstructionViewSet,
                builder -> builder
                    .addColorAttachment(ColorFormat.RGBA32F)
                    .addDepthAttachment(),
                builder -> builder
                    .addColorAttachment(ColorFormat.RGBA32F)
                    .addDepthAttachment(),
                incidentRadianceProgramBuilder, resources))
        {
            Map<String, Drawable<ContextType>> drawableMap = new HashMap<>(reconstructionProgramBuilders.size());

            for (var entry : reconstructionProgramBuilders.entrySet())
            {
                ProgramObject<ContextType> program = entry.getValue().createProgram();
                programMap.put(entry.getKey(), program);
                resources.setupShaderProgram(program);

                specularFit.getBasisResources().useWithShaderProgram(program);
                specularFit.getBasisWeightResources().useWithShaderProgram(program);
                program.setTexture("normalMap", specularFit.getNormalMap());
                program.setTexture("specularEstimate", specularFit.getSpecularReflectivityMap());
                program.setTexture("roughnessMap", specularFit.getSpecularRoughnessMap());
                program.setTexture("diffuseMap", specularFit.getDiffuseMap());

                if (specularFit.getConstantMap() != null)
                {
                    program.setTexture("constantMap", specularFit.getConstantMap());
                }

                drawableMap.put(entry.getKey(), resources.createDrawable(program));
            }

            List<Map<String, ColorAppearanceRMSE>> rmseOut = new ArrayList<>(reconstructionViewSet.getCameraPoseCount());

            // Run the reconstruction and save the results to file
            for (ReconstructionView<ContextType> view : reconstruction)
            {
                LOG.info("View {}:", view.getIndex());
                // Allocate hash map for the current view
                rmseOut.add(new HashMap<>(drawableMap.size()));

                for (var entry : drawableMap.entrySet())
                {
                    ColorAppearanceRMSE rmse = view.reconstruct(entry.getValue());

                    if (debugDirectory != null)
                    {
                        saveImageToFile(new File(debugDirectory, entry.getKey()), view.getIndex(), view.getReconstructionFramebuffer());
                    }

                    // Record RMSE
                    rmseOut.get(view.getIndex()).put(entry.getKey(), rmse);

                    LOG.info("{}: \nencoded ground truth = {}\nnormalized sRGB = {}\nnormalized linear = {}",
                        entry.getKey(), rmse.getEncodedGroundTruth(), rmse.getNormalizedSRGB(), rmse.getNormalizedLinear());
                }
            }

            return rmseOut;
        }
        // NoSuchElementException thrown if there's an error reading a ground truth image from the view set
        catch (IOException | NoSuchElementException e)
        {
            LOG.error("An error occurred during reconstruction:", e);
            return null;
        }
    }

    private void saveImageToFile(File outputDirectory, int k, Framebuffer<ContextType> framebuffer)
    {
        try
        {
            String filename = String.format("%04d.png", k);
            framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", new File(outputDirectory, filename));
        }
        catch (IOException e)
        {
            LOG.error("An error occurred while saving image:", e);
        }
    }

    private void saveImageToFile(File outputDirectory, String filename, Framebuffer<ContextType> framebuffer)
    {
        try
        {
            framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", new File(outputDirectory, filename));
        }
        catch (IOException e)
        {
            LOG.error("An error occurred while saving image:", e);
        }
    }
}
