/*
 *  Copyright (c) Michael Tetzlaff 2022
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
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.atomic.DoubleAdder;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.core.ColorFormat;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.ibrelight.core.Projection;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.ibrelight.rendering.ImageReconstruction;
import tetzlaff.ibrelight.rendering.resources.IBRResources;
import tetzlaff.ibrelight.rendering.resources.IBRResourcesImageSpace;

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
        Projection defaultProj = resources.getViewSet().getCameraProjection(resources.getViewSet().getCameraProjectionIndex(
            resources.getViewSet().getPrimaryViewIndex()));

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

    public double reconstruct(SpecularResources<ContextType> specularFit, ProgramBuilder<ContextType> programBuilder, boolean reconstructAll,
        String reconstructName, String groundTruthName)
    {
        if (reconstructAll)
        {
            // Create directory for reconstructions from basis functions
            new File(settings.outputDirectory, reconstructName).mkdir();

            if (groundTruthName != null)
            {
                // Create directory for ground truth images with consistent tonemapping
                new File(settings.outputDirectory, groundTruthName).mkdir();
            }
        }

        // Create reconstruction provider
        try (ImageReconstruction<ContextType> reconstruction = new ImageReconstruction<>(
            resources,
            programBuilder,
            resources.getContext().buildFramebufferObject(imageWidth, imageHeight)
                .addColorAttachment(ColorFormat.RGBA32F)
                .addDepthAttachment(),
            program ->
            {
                specularFit.getBasisResources().useWithShaderProgram(program);
                program.setTexture("normalEstimate", specularFit.getNormalMap());
                program.setTexture("specularEstimate", specularFit.getSpecularReflectivityMap());
                program.setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());
                program.setTexture("diffuseEstimate", specularFit.getDiffuseMap());
            }))
        {
            // Use the same view set as for fitting if another wasn't specified for reconstruction.
            ViewSet reconstructionViewSet = settings.getReconstructionViewSet() != null ? settings.getReconstructionViewSet() : resources.getViewSet();

            if (reconstructAll)
            {
                try (PrintStream rmseOut = new PrintStream(new File(new File(settings.outputDirectory, reconstructName), "rmse.txt")))
                // Text file containing error information
                {
                    DoubleAdder totalMSE = new DoubleAdder();
                    DoubleAdder totalPixels = new DoubleAdder();

                    // Run the reconstruction and save the results to file
                    reconstruction.execute(reconstructionViewSet,
                        (k, framebuffer) -> saveImageToFile(reconstructName, k, framebuffer),
                        (k, framebuffer) ->
                        {
                            if (groundTruthName != null)
                            {
                                saveImageToFile(groundTruthName, k, framebuffer);
                            }
                        },
                        (k, rmse) ->
                        {
                            // Log RMSE
                            rmseOut.println(reconstructionViewSet.getImageFileName(0) + ", " + rmse.x);
                            totalMSE.add(rmse.x * rmse.x * rmse.y /* rmse.y = pixel count after masking */);
                            //noinspection SuspiciousNameCombination
                            totalPixels.add(rmse.y /* pixel count after masking */);
                        });

                    // Report average RMSE across all views
                    double avgRMSE = Math.sqrt(totalMSE.doubleValue() / totalPixels.doubleValue());
                    System.out.println("Average RMSE across all views: " + avgRMSE);
                    rmseOut.println("Average, " + avgRMSE);
                    return avgRMSE;
                }
            }
            else
            {
                // Run the reconstruction and save the results to file
                return reconstruction.executeOnce(reconstructionViewSet, 0,
                    framebuffer -> saveImageToFile(reconstructName + ".png", framebuffer),
                    framebuffer ->
                    {
                        if (groundTruthName != null)
                        {
                            saveImageToFile(groundTruthName + ".png", framebuffer);
                        }
                    }).x /* first component contains the actual RMSE */;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return Double.NaN;
        }
    }

    private void saveImageToFile(String directoryName, int k, Framebuffer<ContextType> framebuffer)
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

    private void saveImageToFile(String filename, Framebuffer<ContextType> framebuffer)
    {
        try
        {
            framebuffer.saveColorBufferToFile(0, "PNG",
                new File(settings.outputDirectory, filename));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
