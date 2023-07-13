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

import tetzlaff.gl.core.*;
import tetzlaff.ibrelight.core.TextureFitSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class WeightImageCreator<ContextType extends Context<ContextType>> implements AutoCloseable
{

    private final int weightsPerImage;

    private final Program<ContextType> program;
    private final VertexBuffer<ContextType> rect;
    private final Drawable<ContextType> drawable;
    private final FramebufferObject<ContextType> framebuffer;
    private final TextureFitSettings settings;

    public WeightImageCreator(ContextType context, TextureFitSettings settings, int weightsPerImage) throws FileNotFoundException
    {
        this.weightsPerImage = weightsPerImage;

        program = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/combineWeights.frag"))
            .createProgram();

        rect = context.createRectangle();

        drawable = context.createDrawable(program);
        drawable.addVertexBuffer("position", rect);
        drawable.setDefaultPrimitiveMode(PrimitiveMode.TRIANGLE_FAN);

        this.settings = settings;

        framebuffer = context.buildFramebufferObject(settings.width, settings.height)
            .addColorAttachment(ColorFormat.RGBA8)
            .createFramebufferObject();
    }

    public void createImages(SpecularResources<ContextType> specularFit, File outputDirectory) throws IOException
    {
        specularFit.getBasisWeightResources().useWithShaderProgram(program);
        drawable.program().setUniform("weightStride", weightsPerImage);

        int basisCount = specularFit.getBasisResources().getSpecularBasisSettings().getBasisCount();

        // Loop over the index of each final image to export
        for (int i = 0; i < basisCount; i += weightsPerImage)
        {
            drawable.program().setUniform("weightIndex", i);
            drawable.draw(framebuffer);
            framebuffer.saveColorBufferToFile(0, "PNG", new File(outputDirectory, SpecularFitSerializer.getWeightFileName(i / weightsPerImage, weightsPerImage)));
        }
    }

    @Override
    public void close() throws Exception
    {
        program.close();
        rect.close();
        framebuffer.close();
    }
}
