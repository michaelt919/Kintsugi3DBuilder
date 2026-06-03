/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.io.specular;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.resources.project.specular.TextureResources;
import kintsugi3d.gl.core.*;

import java.io.File;
import java.io.IOException;

public class WeightImageWriter<ContextType extends Context<ContextType>> implements Resource
{

    private final int weightsPerImage;

    private final ProgramObject<ContextType> program;
    private final VertexBuffer<ContextType> rect;
    private final Drawable<ContextType> drawable;
    private final FramebufferObject<ContextType> framebuffer;

    public WeightImageWriter(ContextType context, TextureResolution resolution, int weightsPerImage) throws IOException
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

        framebuffer = context.buildFramebufferObject(resolution.width, resolution.height)
            .addColorAttachment(ColorFormat.RGBA8)
            .createFramebufferObject();
    }

    public void saveImages(TextureResources<ContextType> specularFit, String format,
                           File outputDirectory, String... filenames) throws IOException
    {
        specularFit.getBasisWeightResources().useWithShaderProgram(program);

        int basisCount = specularFit.getBasisResources().getBasisCount();

        // Loop over the index of each final image to export
        for (int i = 0; i * weightsPerImage < basisCount && i < filenames.length; i++)
        {
            drawable.program().setUniform("weightIndex", i * weightsPerImage);
            drawable.program().setUniform("weightStride", Math.min(weightsPerImage, basisCount - i * weightsPerImage));
            drawable.draw(framebuffer);
            framebuffer.getTextureReaderForColorAttachment(0).saveToFile(format, new File(outputDirectory, filenames[i]));
        }
    }

    @Override
    public void close()
    {
        program.close();
        drawable.close();
        rect.close();
        framebuffer.close();
    }
}
