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
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.rendering.resources.ReadonlyIBRResources;
import tetzlaff.util.ShaderHoleFill;

public class FinalDiffuseOptimization<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private static final Logger log = LoggerFactory.getLogger(FinalDiffuseOptimization.class);

    // Graphics context
    private final ContextType context;

    // Final diffuse estimation program
    private final ProgramObject<ContextType> estimationProgram;
    private final TextureFitSettings textureFitSettings;

    private final boolean includeConstant;

    // Framebuffer for storing the diffuse solution
    private FramebufferObject<ContextType> framebuffer;

    private final Drawable<ContextType> drawable;

    public FinalDiffuseOptimization(ReadonlyIBRResources<ContextType> resources,
        SpecularFitProgramFactory<ContextType> programFactory, TextureFitSettings settings, boolean includeConstant)
        throws FileNotFoundException
    {
        this.context = resources.getContext();
        this.estimationProgram = createDiffuseEstimationProgram(resources, programFactory);
        this.textureFitSettings = settings;
        this.includeConstant = includeConstant;

        framebuffer = createFramebuffer(context, settings, includeConstant);
        drawable = resources.createDrawable(estimationProgram);
    }

    private static <ContextType extends Context<ContextType>> FramebufferObject<ContextType> createFramebuffer(
        ContextType context, TextureFitSettings texSettings, boolean includeConstant)
    {
        FramebufferObjectBuilder<ContextType> builder = context
            .buildFramebufferObject(texSettings.width, texSettings.height)
            .addColorAttachment(ColorFormat.RGBA8);

        if (includeConstant)
        {
            builder.addColorAttachment(ColorFormat.RGBA8); // Add attachment for storing constant term texture
        }

        return builder.createFramebufferObject();
    }

    public void execute(SpecularResources<ContextType> specularFit)
    {
        // Set up diffuse estimation shader program
        specularFit.getBasisResources().useWithShaderProgram(estimationProgram);
        specularFit.getBasisWeightResources().useWithShaderProgram(estimationProgram);
        estimationProgram.setTexture("normalEstimate", specularFit.getNormalMap());
        estimationProgram.setTexture("roughnessEstimate", specularFit.getSpecularRoughnessMap());

        // Second framebuffer for filling holes (used to double-buffer the first framebuffer)
        // Placed outside of try-with-resources since it might end up being the primary framebuffer after filling holes.
        FramebufferObject<ContextType> framebuffer2 = createFramebuffer(context, textureFitSettings, includeConstant);

        // Will reference the framebuffer that is in front after hole filling if everything is successful.
        FramebufferObject<ContextType> finalDiffuse = null;

        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

        if (includeConstant)
        {
            framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        // Perform diffuse fit
        drawable.draw(framebuffer);

        try (ShaderHoleFill<ContextType> holeFill = new ShaderHoleFill<>(context))
        {
            // Fill holes
            finalDiffuse = holeFill.execute(framebuffer, framebuffer2);
        }
        catch (FileNotFoundException e)
        {
            log.error("An error occurred while filling holes:", e);
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

    public Texture2D<ContextType> getConstantMap()
    {
        return framebuffer.getColorAttachmentTexture(1);
    }

    public boolean includesConstantMap()
    {
        return includeConstant;
    }

    private static <ContextType extends Context<ContextType>>
    ProgramObject<ContextType> createDiffuseEstimationProgram(
            ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory) throws FileNotFoundException
    {
        return programFactory.createProgram(resources,
            new File("shaders/common/texspace_dynamic.vert"),
            new File("shaders/specularfit/estimateDiffuse.frag"));
    }
}
