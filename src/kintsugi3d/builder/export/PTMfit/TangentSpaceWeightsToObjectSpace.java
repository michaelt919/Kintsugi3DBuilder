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

package kintsugi3d.builder.export.PTMfit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.builder.core.TextureFitSettings;
import kintsugi3d.builder.resources.IBRResourcesImageSpace;
import kintsugi3d.util.ShaderHoleFill;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TangentSpaceWeightsToObjectSpace <ContextType extends Context<ContextType>>
{
    private static final Logger log = LoggerFactory.getLogger(TangentSpaceWeightsToObjectSpace.class);
    private final IBRResourcesImageSpace<ContextType> resources;
    private final TextureFitSettings settings;

    public TangentSpaceWeightsToObjectSpace(IBRResourcesImageSpace<ContextType> resources, TextureFitSettings settings)
    {
        this.resources = resources;
        this.settings = settings;
    }

    public void run(PTMsolution solutions, ProgramBuilder<ContextType> programBuilder, int weightStart, int weightCount, File outputDirectory)
    {
        try (ProgramObject<ContextType> program = programBuilder.createProgram();
             FramebufferObject<ContextType> fbo = resources.getContext().buildFramebufferObject(settings.width, settings.height)
                 .addColorAttachments(ColorFormat.RGBA8, weightCount)
                 .createFramebufferObject();
             FramebufferObject<ContextType> fbo2 = resources.getContext().buildFramebufferObject(settings.width, settings.height)
                     .addColorAttachments(ColorFormat.RGBA8, weightCount)
                     .createFramebufferObject();
             Texture3D<ContextType> weightMaps =
                     resources.getContext().getTextureFactory().build2DColorTextureArray(settings.width, settings.height, 10)
                     .setInternalFormat(ColorFormat.RGBA32F)
                     .setLinearFilteringEnabled(true)
                     .setMipmapsEnabled(false)
                     .createTexture())
        {
            // Run the reconstruction and save the results to file
            NativeVectorBufferFactory factory = NativeVectorBufferFactory.getInstance();
            NativeVectorBuffer weightMapBuffer = factory.createEmpty(NativeDataType.FLOAT, 4, settings.width * settings.height);
            for (int p = 0; p < settings.width * settings.height; p++)
            {
                weightMapBuffer.set(p, 3, /*solutions.areWeightsValid(p) ? 1.0 : 0.0*/ 1.0);
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

            program.setTexture("weightMaps", weightMaps);
            Drawable<ContextType> drawable = resources.createDrawable(program);
            for (int i = 0; i < weightCount; i++)
            {
                fbo.clearColorBuffer(i, 0, 0, 0, 0);
            }
            drawable.draw(fbo);
            FramebufferObject<ContextType> finalFBO = new ShaderHoleFill<>(resources.getContext()).execute(fbo, fbo2);
            saveToFile(finalFBO, weightStart, weightCount, outputDirectory);
        }

        catch (FileNotFoundException e)
        {
            log.error("An error occurred:", e);
        }

    }
    private void saveToFile(Framebuffer<ContextType> framebuffer, int weightStart, int weightCount, File outputDirectory)
    {
        try
        {
            for (int i = 0; i < weightCount; i++)
            {
                String filename = String.format("objWeights%02d.png", weightStart + i);
                framebuffer.getTextureReaderForColorAttachment(i).saveToFile("PNG", new File(outputDirectory, filename));
            }
        }
        catch (IOException e)
        {
            log.error("An error occurred saving file:", e);
        }
    }
}