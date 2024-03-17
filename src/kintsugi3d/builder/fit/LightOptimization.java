/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.fit;

import java.io.File;
import java.io.FileNotFoundException;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.gl.builders.framebuffer.FramebufferObjectBuilder;
import kintsugi3d.gl.core.*;
import kintsugi3d.gl.vecmath.Vector4;
import kintsugi3d.util.ColorList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightOptimization<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private static final Logger log = LoggerFactory.getLogger(LightOptimization.class);

    // Graphics context
    private final ContextType context;

    // Final diffuse estimation program
    private final ProgramObject<ContextType> estimationProgram;
    private final TextureResolution textureResolution;

    // Framebuffer for storing the diffuse solution
    private FramebufferObject<ContextType> framebuffer;

    private final Drawable<ContextType> drawable;

    public LightOptimization(ReadonlyIBRResources<ContextType> resources,
        SpecularFitProgramFactory<ContextType> programFactory, TextureResolution settings)
        throws FileNotFoundException
    {
        this.context = resources.getContext();
        this.estimationProgram = createProgram(resources, programFactory);
        this.textureResolution = settings;

        framebuffer = createFramebuffer(context, settings);
        drawable = resources.createDrawable(estimationProgram);
    }

    private static <ContextType extends Context<ContextType>> FramebufferObject<ContextType> createFramebuffer(
        ContextType context, TextureResolution texSettings)
    {
        FramebufferObjectBuilder<ContextType> builder = context
            .buildFramebufferObject(texSettings.width, texSettings.height)
            .addColorAttachment(ColorFormat.RGBA8);

        return builder.createFramebufferObject();
    }

    public void execute(SpecularMaterialResources<ContextType> specularFit)
    {
        if (specularFit.getNormalMap() != null)
        {
            // Set up light estimation shader program
            estimationProgram.setTexture("normalMap", specularFit.getNormalMap());
        }

        // Will reference the framebuffer that is in front after hole filling if everything is successful.
        FramebufferObject<ContextType> finalDiffuse = null;

        framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);

        // Perform diffuse fit
        drawable.draw(framebuffer);

        ColorList estimates = framebuffer.getTextureReaderForColorAttachment(0).readColorListRGBA(60, 170, 10, 10);
        Vector4 weightedSum = estimates.stream()
            .map(color -> new Vector4(color.x * color.w, color.y * color.w, color.z * color.w, color.w)) // premultiply weight
            .reduce(Vector4::plus).orElse(Vector4.ZERO);
        log.info("Estimated light offset: " + weightedSum.dividedBy(weightedSum.w));
    }

    public Texture2D<ContextType> getLightFit()
    {
        return framebuffer.getColorAttachmentTexture(0);
    }

    @Override
    public void close()
    {
        estimationProgram.close();
        drawable.close();
        framebuffer.close();
    }

    private static <ContextType extends Context<ContextType>>
    ProgramObject<ContextType> createProgram(
            ReadonlyIBRResources<ContextType> resources, SpecularFitProgramFactory<ContextType> programFactory) throws FileNotFoundException
    {
        return programFactory.createProgram(resources,
            new File("shaders/common/texspace_dynamic.vert"), new File("shaders/specularfit/estimateLight.frag"));
    }
}
