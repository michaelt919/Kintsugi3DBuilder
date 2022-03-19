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

package tetzlaff.ibrelight.rendering;

import java.io.FileNotFoundException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.ibrelight.rendering.resources.IBRResources;

public class ImageReconstruction<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private final Program<ContextType> program;
    private final Drawable<ContextType> drawable;
    private final FramebufferObject<ContextType> framebuffer;

    public ImageReconstruction(IBRResources<ContextType> resources, ProgramBuilder<ContextType> programBuilder,
                               FramebufferObjectBuilder<ContextType> framebufferObjectBuilder, Consumer<Program<ContextType>> programSetup)
        throws FileNotFoundException
    {
        this.program = programBuilder.createProgram();
        programSetup.accept(program);
        this.drawable = resources.createDrawable(program);
        this.framebuffer = framebufferObjectBuilder.createFramebufferObject();
    }

    public void execute(ViewSet viewSet, BiConsumer<Integer, Framebuffer<ContextType>> reconstructionAction)
    {
        for (int k = 0; k < viewSet.getCameraPoseCount(); k++)
        {
            drawable.program().setUniform("model_view", viewSet.getCameraPose(k));
            drawable.program().setUniform("projection",
                viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(k)).getProjectionMatrix(
                    viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
            drawable.program().setUniform("reconstructionCameraPos",
                viewSet.getCameraPoseInverse(k).getColumn(3).getXYZ());
            drawable.program().setUniform("reconstructionLightPos",
                viewSet.getCameraPoseInverse(k).times(viewSet.getLightPosition(viewSet.getLightIndex(k)).asPosition()).getXYZ());
            drawable.program().setUniform("reconstructionLightIntensity",
                    viewSet.getLightIntensity(viewSet.getLightIndex(k)));
            drawable.program().setUniform("gamma", viewSet.getGamma());

            for (int i = 0; i < framebuffer.getColorAttachmentCount(); i++)
            {
                // Clear to black
                framebuffer.clearColorBuffer(i, 0.0f, 0.0f, 0.0f, 1.0f);
            }

            // Also clear the depth buffer
            framebuffer.clearDepthBuffer();

            // Draw the view into the framebuffer.
            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            // Give the callback an opportunity to do something with the view.
            reconstructionAction.accept(k, framebuffer);
        }
    }

    @Override
    public void close()
    {
        program.close();
        framebuffer.close();
    }
}
