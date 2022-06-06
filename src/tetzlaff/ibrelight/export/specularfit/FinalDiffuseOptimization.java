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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.rendering.resources.IBRResources;
import tetzlaff.util.ShaderHoleFill;

public class FinalDiffuseOptimization<ContextType extends Context<ContextType>> implements AutoCloseable
{
    // Graphics context
    private final ContextType context;

    // Final diffuse estimation program
    private final Program<ContextType> estimationProgram;

    // Framebuffer for storing the diffuse solution
    private FramebufferObject<ContextType> framebuffer;

    private final Drawable<ContextType> drawable;

    private final SpecularFitSettings settings;

    public FinalDiffuseOptimization(ContextType context, IBRResources<ContextType> resources, SpecularFitSettings settings)
        throws FileNotFoundException
    {
        this.context = context;
        estimationProgram = createDiffuseEstimationProgram(new SpecularFitProgramFactory<>(resources, settings));
        framebuffer = context.buildFramebufferObject(settings.width, settings.height)
            .addColorAttachment(ColorFormat.RGBA32F)
            .createFramebufferObject();
        drawable = resources.createDrawable(estimationProgram);
        this.settings = settings;
    }

    public void execute(SpecularFit<ContextType> specularFit)
    {
        // Set up diffuse estimation shader program
        specularFit.basisResources.useWithShaderProgram(estimationProgram);
        estimationProgram.setTexture("normalEstimate", specularFit.getNormalMap());
        estimationProgram.setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());

        // Second framebuffer for filling holes (used to double-buffer the first framebuffer)
        // Placed outside of try-with-resources since it might end up being the primary framebuffer after filling holes.
        FramebufferObject<ContextType> framebuffer2 = context.buildFramebufferObject(settings.width, settings.height)
            .addColorAttachment(ColorFormat.RGBA32F)
            .createFramebufferObject();

        // Will reference the framebuffer that is in front after hole filling if everything is successful.
        FramebufferObject<ContextType> finalDiffuse = null;

        try (ShaderHoleFill<ContextType> holeFill = new ShaderHoleFill<>(context))
        {
            // Perform diffuse fit
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
            drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);

            // Fill holes
            finalDiffuse = holeFill.execute(framebuffer, framebuffer2);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (Objects.equals(finalDiffuse, framebuffer2))
            {
                // New framebuffer is the front framebuffer after filling holes;
                // close the old one and make the new one the primary framebuffer
                framebuffer.close();
                framebuffer = framebuffer2;
            }
            else
            {
                // New framebuffer is the back framebuffer after filling holes (or an exception occurred);
                // either way; just close it and leave the primary framebuffer the same.
                framebuffer2.close();
            }
        }
    }

    @Override
    public void close()
    {
        estimationProgram.close();
        framebuffer.close();
    }

    public Texture2D<ContextType> getDiffuseMap()
    {
        return framebuffer.getColorAttachmentTexture(0);
    }

    public void saveDiffuseMap()
    {
        try
        {
            framebuffer.saveColorBufferToFile(0, "PNG", new File(settings.outputDirectory, "diffuse.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static <ContextType extends Context<ContextType>>
        Program<ContextType> createDiffuseEstimationProgram(SpecularFitProgramFactory<ContextType> programFactory) throws FileNotFoundException
    {
        return programFactory.createProgram(
            new File("shaders/common/texspace_noscale.vert"),
            new File("shaders/specularfit/estimateDiffuse.frag"));
    }
}
