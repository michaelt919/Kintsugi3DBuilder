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

package kintsugi3d.builder.fit.decomposition;

import kintsugi3d.builder.fit.settings.BasisSettings;
import kintsugi3d.builder.resources.project.specular.SpecularMaterialResources;
import kintsugi3d.gl.core.*;

import java.io.File;
import java.io.IOException;

public class BasisImageCreator<ContextType extends Context<ContextType>> implements AutoCloseable
{
    // Program for drawing basis functions as supplemental output
    private final ProgramObject<ContextType> program;

    // Rectangle vertex buffer
    private final VertexBuffer<ContextType> rect;

    // Drawable with program and rectangle vertex buffer
    private final Drawable<ContextType> drawable;

    // Framebuffer for the basis images
    private final FramebufferObject<ContextType> framebuffer;

    private final BasisSettings settings;

    public BasisImageCreator(ContextType context, BasisSettings settings) throws IOException
    {
        program = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/specularfit/basisImage.frag"))
            .createProgram();

        rect = context.createRectangle();

        drawable = context.createDrawable(program);
        drawable.addVertexBuffer("position", rect);
        drawable.setDefaultPrimitiveMode(PrimitiveMode.TRIANGLE_FAN);

        this.settings = settings;

        framebuffer = context.buildFramebufferObject(
            2 * this.settings.getBasisResolution() + 1, 2 * this.settings.getBasisResolution() + 1)
            .addColorAttachment(ColorFormat.RGBA8)
            .createFramebufferObject();
    }

    public void createImages(SpecularMaterialResources<ContextType> specularFit, File outputDirectory) throws IOException
    {
        specularFit.getBasisResources().useWithShaderProgram(program);
        specularFit.getBasisWeightResources().useWithShaderProgram(program);

        MaterialBasis basis = specularFit.getBasisResources().getBasis();

        // Save basis functions in image format.
        for (int i = 0; i < settings.getBasisCount(); i++)
        {
            drawable.program().setUniform("basisIndex", i);
            drawable.program().setUniform("diffuseColor", basis.getDiffuseColor(i).asSinglePrecision());
            drawable.draw(framebuffer);
            framebuffer.getTextureReaderForColorAttachment(0)
                .saveToFile("PNG", new File(outputDirectory, getBasisImageFilename(i)));
        }
    }

    public static String getBasisImageFilename(int materialIndex)
    {
        return String.format("basis_%02d.png", materialIndex);
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
