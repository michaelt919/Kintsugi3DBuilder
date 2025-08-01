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

package kintsugi3d.builder.fit.finalize;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.fit.SpecularFitProgramFactory;
import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.gl.builders.framebuffer.FramebufferObjectBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.util.ShaderHoleFill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class FinalDiffuseOptimization<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private static final Logger log = LoggerFactory.getLogger(FinalDiffuseOptimization.class);

    // Graphics context
    private final ContextType context;

    // Final diffuse estimation program
    private final ProgramObject<ContextType> estimationProgram;
    private final TextureResolution textureResolution;

    private final boolean includeConstant;

    // Framebuffer for storing the diffuse solution
    private FramebufferObject<ContextType> framebuffer;

    private final Drawable<ContextType> drawable;

    public FinalDiffuseOptimization(ReadonlyIBRResources<ContextType> resources,
        SpecularFitProgramFactory<ContextType> programFactory, TextureResolution settings, boolean includeConstant)
        throws IOException
    {
        this.context = resources.getContext();
        this.estimationProgram = includeConstant ? createDiffuseTranslucentEstimationProgram(resources, programFactory)
            : createDiffuseEstimationProgram(resources, programFactory);
        this.textureResolution = settings;
        this.includeConstant = includeConstant;

        framebuffer = createFramebuffer(context, settings, includeConstant);
        drawable = resources.createDrawable(estimationProgram);
    }

    private static <ContextType extends Context<ContextType>> FramebufferObject<ContextType> createFramebuffer(
        ContextType context, TextureResolution texSettings, boolean includeConstant)
    {
        FramebufferObjectBuilder<ContextType> builder = context
            .buildFramebufferObject(texSettings.width, texSettings.height)
            .addColorAttachment(ColorFormat.RGBA8);

        if (includeConstant)
        {
            builder.addColorAttachment(ColorFormat.RGBA8); // Add attachment for storing constant term texture
//            builder.addColorAttachment(ColorFormat.RGBA8); // Add attachment for storing quadratic term texture
        }

        return builder.createFramebufferObject();
    }

    public void execute(SpecularMaterialResources<ContextType> specularFit)
    {
        // Set up diffuse estimation shader program
        specularFit.getBasisResources().useWithShaderProgram(estimationProgram);
        specularFit.getBasisWeightResources().useWithShaderProgram(estimationProgram);
        estimationProgram.setTexture("normalMap", specularFit.getNormalMap());
        estimationProgram.setTexture("roughnessMap", specularFit.getSpecularRoughnessMap());

        // Second framebuffer for filling holes (used to double-buffer the first framebuffer)
        // Placed outside of try-with-resources since it might end up being the primary framebuffer after filling holes.
        FramebufferObject<ContextType> framebuffer2 = createFramebuffer(context, textureResolution, includeConstant);

        // Will reference the framebuffer that is in front after hole filling if everything is successful.
        FramebufferObject<ContextType> finalDiffuse = null;

        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

        if (includeConstant)
        {
            framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
            framebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        // Perform diffuse fit
        drawable.draw(framebuffer);

        try (ShaderHoleFill<ContextType> holeFill = new ShaderHoleFill<>(context))
        {
            // Fill holes
            finalDiffuse = holeFill.execute(framebuffer, framebuffer2);
        }
        catch (IOException e)
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
        drawable.close();
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

//    public Texture2D<ContextType> getQuadraticMap()
//    {
//        return framebuffer.getColorAttachmentTexture(2);
//    }

    public boolean includesConstantMap()
    {
        return includeConstant;
    }

    private static <ContextType extends Context<ContextType>>
    ProgramObject<ContextType> createDiffuseEstimationProgram(
            ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory) throws IOException
    {
        return programFactory.createProgram(resources,
            new File("shaders/common/texspace_dynamic.vert"), new File("shaders/specularfit/estimateDiffuse.frag"));
    }
    private static <ContextType extends Context<ContextType>>
    ProgramObject<ContextType> createDiffuseTranslucentEstimationProgram(
        ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory) throws IOException
    {
        return programFactory.createProgram(resources,
            new File("shaders/common/texspace_dynamic.vert"), new File("shaders/specularfit/estimateDiffuseTranslucent.frag"));
    }
}
