/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.ColorFormat;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.ibrelight.core.Projection;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.ibrelight.rendering.ImageReconstruction;

public class FinalReconstruction<ContextType extends Context<ContextType>>
{
    private final IBRResources<ContextType> resources;
    private final SpecularFitSettings settings;

    private final int imageWidth;
    private final int imageHeight;

    public FinalReconstruction(IBRResources<ContextType> resources, SpecularFitSettings settings)
    {
        this.resources = resources;
        this.settings = settings;

        // Calculate reasonable image resolution for reconstructed images (supplemental output)
        Projection defaultProj = resources.viewSet.getCameraProjection(resources.viewSet.getCameraProjectionIndex(
            resources.viewSet.getPrimaryViewIndex()));

        if (defaultProj.getAspectRatio() < 1.0)
        {
            imageWidth = settings.width;
            imageHeight = Math.round(imageWidth / defaultProj.getAspectRatio());
        }
        else
        {
            imageHeight = settings.height;
            imageWidth = Math.round(imageHeight * defaultProj.getAspectRatio());
        }
    }

    public void reconstruct(SpecularFit<ContextType> specularFit, ProgramBuilder<ContextType> programBuilder, String directoryName)
    {
        // Create directory for reconstructions from basis functions
        new File(settings.outputDirectory, directoryName).mkdir();

        // Create reconstruction provider
        try (ImageReconstruction<ContextType> reconstruction = new ImageReconstruction<>(
            resources,
            programBuilder, //getImageReconstructionProgramBuilder(programFactory),
            resources.context.buildFramebufferObject(imageWidth, imageHeight)
                .addColorAttachment(ColorFormat.RGBA32F)
                .addDepthAttachment(),
            program ->
            {
                specularFit.basisResources.useWithShaderProgram(program);
                program.setTexture("normalEstimate", specularFit.getNormalMap());
                program.setTexture("specularEstimate", specularFit.getSpecularReflectivityMap());
                program.setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());
                program.setTexture("diffuseEstimate", specularFit.getDiffuseMap());
            }))
        {
            // Run the reconstruction and save the results to file
            reconstruction.execute((k, framebuffer) -> saveReconstructionToFile(directoryName, k, framebuffer));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private void saveReconstructionToFile(String directoryName, int k, Framebuffer<ContextType> framebuffer)
    {
        try
        {
            String filename = String.format("%04d.png", k);
            framebuffer.saveColorBufferToFile(0, "PNG",
                new File(new File(settings.outputDirectory, directoryName), filename));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
