/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.export.specular;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import kintsugi3d.builder.core.TextureResolution;
import kintsugi3d.builder.resources.specular.SpecularMaterialResources;
import kintsugi3d.gl.core.*;

public class WeightImageCreator<ContextType extends Context<ContextType>> implements Resource
{

    private final int weightsPerImage;

    private final ProgramObject<ContextType> program;
    private final VertexBuffer<ContextType> rect;
    private final Drawable<ContextType> drawable;
    private final FramebufferObject<ContextType> framebuffer;

    public WeightImageCreator(ContextType context, TextureResolution resolution, int weightsPerImage) throws IOException
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

    public void createImages(SpecularMaterialResources<ContextType> specularFit, File outputDirectory) throws IOException
    {
        specularFit.getBasisWeightResources().useWithShaderProgram(program);
        drawable.program().setUniform("weightStride", weightsPerImage);

        int basisCount = specularFit.getBasisResources().getBasisCount();

        // Loop over the index of each final image to export
        for (int i = 0; i < basisCount; i += weightsPerImage)
        {
            drawable.program().setUniform("weightIndex", i);
            drawable.draw(framebuffer);
            String filename = SpecularFitSerializer.getWeightFileName(i / weightsPerImage, weightsPerImage);
            framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", new File(outputDirectory, filename));
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
