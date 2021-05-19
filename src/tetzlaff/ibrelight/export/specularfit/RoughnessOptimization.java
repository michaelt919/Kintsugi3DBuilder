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

import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.core.*;

public class RoughnessOptimization<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private final SpecularFitSettings settings;

    private final Program<ContextType> specularRoughnessFitProgram;
    private final FramebufferObject<ContextType> specularTexFramebuffer;
    private final VertexBuffer<ContextType> rect;

    private final Drawable<ContextType> specularRoughnessFitDrawable;

    public RoughnessOptimization(ContextType context, BasisResources<ContextType> resources, SpecularFitSettings settings)
        throws FileNotFoundException
    {
        // Fit specular parameters from weighted basis functions
        specularRoughnessFitProgram = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/specularRoughnessFit.frag"))
            .define("BASIS_COUNT", settings.basisCount)
            .define("MICROFACET_DISTRIBUTION_RESOLUTION", settings.microfacetDistributionResolution)
            .createProgram();

        // Create basic rectangle vertex buffer
        rect = context.createRectangle();

        // Framebuffer for fitting and storing the specular parameter estimates (specular Fresnel color and roughness)
        specularTexFramebuffer = context.buildFramebufferObject(settings.width, settings.height)
            .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8).setLinearFilteringEnabled(true))
            .addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGBA8).setLinearFilteringEnabled(true))
            .createFramebufferObject();

        // Set up shader program
        specularRoughnessFitDrawable = context.createDrawable(specularRoughnessFitProgram);
        specularRoughnessFitDrawable.addVertexBuffer("position", rect);
        resources.useWithShaderProgram(specularRoughnessFitProgram);
        specularRoughnessFitProgram.setUniform("gamma", settings.additional.getFloat("gamma"));
        specularRoughnessFitProgram.setUniform("fittingGamma", 1.0f);

        // Set initial assumption for roughness when calculating masking/shadowing.
        specularTexFramebuffer.clearColorBuffer(1, 1.0f, 1.0f, 1.0f, 1.0f);

        this.settings = settings;
    }

    public Texture2D<ContextType> getReflectivityTexture()
    {
        return specularTexFramebuffer.getColorAttachmentTexture(0);
    }

    public Texture2D<ContextType> getRoughnessTexture()
    {
        return specularTexFramebuffer.getColorAttachmentTexture(1);
    }

    public void execute()
    {
        // Fit specular so that we have a roughness estimate for masking/shadowing.
        specularTexFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f,0.0f);
        specularTexFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f,0.0f);
        specularRoughnessFitDrawable.draw(PrimitiveMode.TRIANGLE_FAN, specularTexFramebuffer);
    }

    public void saveTextures()
    {
        try
        {
            specularTexFramebuffer.saveColorBufferToFile(0, "PNG", new File(settings.outputDirectory, "specular.png"));
            specularTexFramebuffer.saveColorBufferToFile(1, "PNG", new File(settings.outputDirectory, "roughness.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void close()
    {
        specularRoughnessFitProgram.close();
        specularTexFramebuffer.close();
        rect.close();
    }
}
