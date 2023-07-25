/*
 *  Copyright (c) Zhangchi (Josh) Lyu, Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.PTMfit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.ibrelight.core.TextureFitSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.ibrelight.core.Projection;
import tetzlaff.ibrelight.rendering.ImageReconstruction;
import tetzlaff.ibrelight.rendering.resources.IBRResourcesImageSpace;

public class PTMReconstruction <ContextType extends Context<ContextType>> implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PTMReconstruction.class);
    private final IBRResourcesImageSpace<ContextType> resources;
    private final TextureFitSettings settings;
    private final int imageWidth;
    private final int imageHeight;
    public final Texture3D<ContextType> weightMaps;


    public PTMReconstruction(IBRResourcesImageSpace<ContextType> resources,TextureFitSettings settings) {
        this.settings = settings;
        this.resources=resources;
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
        ContextType context= resources.getContext();
        weightMaps = context.getTextureFactory().build2DColorTextureArray(settings.width, settings.height, 10)
                .setInternalFormat(ColorFormat.RGB32F)
                .setLinearFilteringEnabled(true)
                .setMipmapsEnabled(false)
                .createTexture();
    }

    public void reconstruct(PTMsolution solutions,ProgramBuilder<ContextType> programBuilder, File outputParentDirectory, String directoryName){
        new File(outputParentDirectory, directoryName).mkdir();
        try (
            ImageReconstruction<ContextType> reconstruction = new ImageReconstruction<>(
                resources,
                programBuilder,
                resources.getContext().buildFramebufferObject(imageWidth, imageHeight)
                        .addColorAttachment(ColorFormat.RGBA32F)
                        .addDepthAttachment(),
                program ->
                {
                    resources.setupShaderProgram(program);
                    program.setTexture("weightMaps", this.weightMaps);
                    program.setUniform("width",this.imageWidth);
                    program.setUniform("length",this.imageHeight);

                }
            )
        )
        {
            // Run the reconstruction and save the results to file
            NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();
            NativeVectorBuffer weightMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 3, settings.width * settings.height);
            for (int p = 0; p < settings.width * settings.height; p++)
            {
                weightMapBuffer.set(p, 0, solutions.areWeightsValid(p) ? 1.0 : 0.0);
            }
            for (int p = settings.width * settings.height; p < settings.width * settings.height*2; p++)
            {
                weightMapBuffer.set(p%(settings.width * settings.height), 1, solutions.areWeightsValid(p) ? 1.0 : 0.0);
            }
            for (int p = settings.width * settings.height*2; p < settings.width * settings.height*3; p++)
            {
                weightMapBuffer.set(p%(settings.width * settings.height), 2, solutions.areWeightsValid(p) ? 1.0 : 0.0);
            }

            for (int b = 0; b < solutions.getPTMmodel().getBasisFunctionCount(); b++)
            {
                // Copy weights from the individual solutions into the weight buffer laid out in texture space to be sent to the GPU.
                for (int p = 0; p < settings.width * settings.height; p++)
                {
                    weightMapBuffer.set(p, 0, solutions.getWeights(p).get(b));
                }
                for (int p =settings.width * settings.height ; p < settings.width * settings.height*2; p++)
                {
                    weightMapBuffer.set(p%(settings.width * settings.height), 1, solutions.getWeights(p).get(b));
                }
                for (int p = settings.width * settings.height*2; p < settings.width * settings.height*3; p++)
                {
                    weightMapBuffer.set(p%(settings.width * settings.height), 2, solutions.getWeights(p).get(b));
                }

                // Immediately load the weight map so that we can reuse the local memory buffer.
                weightMaps.loadLayer(b, weightMapBuffer);

            }

            reconstruction.execute(resources.getViewSet(),
                (k, framebuffer) -> saveReconstructionToFile(outputParentDirectory, directoryName, k, framebuffer),
                null, null /* do something with these later */);
        }

        catch (FileNotFoundException e)
        {
            log.error("An error occurred during reconstruction:", e);
        }

    }

    @Override
    public void close()
    {
        weightMaps.close();
    }

    private void saveReconstructionToFile( File outputParentDirectory, String directoryName, int k, Framebuffer<ContextType> framebuffer)
    {
        try
        {
            String filename = String.format("%04d.png", k);
            framebuffer.saveColorBufferToFile(0, "PNG",
                    new File(new File(outputParentDirectory, directoryName), filename));
        }
        catch (IOException e)
        {
            log.error("Error occurred while saving reconstruction:", e);
        }
    }
}
