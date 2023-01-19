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

import tetzlaff.gl.core.*;

public class BasisImageCreator<ContextType extends Context<ContextType>> implements AutoCloseable
{
    // Program for drawing basis functions as supplemental output
    private final Program<ContextType> program;

    // Rectangle vertex buffer
    private final VertexBuffer<ContextType> rect;

    // Drawable with program and rectangle vertex buffer
    private final Drawable<ContextType> drawable;

    // Framebuffer for the basis images
    private final FramebufferObject<ContextType> framebuffer;

    private final SpecularFitSettings settings;

    public BasisImageCreator(ContextType context, SpecularFitSettings settings) throws FileNotFoundException
    {
        program = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/basisImage.frag"))
            .createProgram();

        rect = context.createRectangle();

        drawable = context.createDrawable(program);
        drawable.addVertexBuffer("position", rect);

        framebuffer = context.buildFramebufferObject(
            2 * settings.microfacetDistributionResolution + 1, 2 * settings.microfacetDistributionResolution + 1)
            .addColorAttachment(ColorFormat.RGBA8)
            .createFramebufferObject();

        this.settings = settings;
    }

    public void createImages(SpecularFitFromOptimization<ContextType> specularFit) throws IOException
    {
        program.setTexture("basisFunctions", specularFit.basisResources.basisMaps);

        // Save basis functions in image format.
        for (int i = 0; i < settings.basisCount; i++)
        {
            drawable.program().setUniform("basisIndex", i);
            drawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
            framebuffer.saveColorBufferToFile(0, "PNG", new File(settings.outputDirectory, String.format("basis_%02d.png", i)));
        }
    }

    @Override
    public void close()
    {
        program.close();
        rect.close();
        framebuffer.close();
    }
}
