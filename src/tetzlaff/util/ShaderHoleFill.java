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

package tetzlaff.util;

import java.io.File;
import java.io.FileNotFoundException;

import tetzlaff.gl.core.*;

public class ShaderHoleFill<ContextType extends Context<ContextType>> implements AutoCloseable
{
    // Hole fill program
    private final Program<ContextType> program;

    // Rectangle vertex buffer
    private final VertexBuffer<ContextType> rect;

    // Drawable
    private final Drawable<ContextType> drawable;

    public ShaderHoleFill(ContextType context) throws FileNotFoundException
    {
        program = context.getShaderProgramBuilder()
            .addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
            .addShader(ShaderType.FRAGMENT, new File("shaders/common/holefill.frag"))
            .createProgram();

        rect = context.createRectangle();

        drawable = context.createDrawable(program);
        drawable.addVertexBuffer("position", rect);
    }

    @Override
    public void close()
    {
        program.close();
        rect.close();
    }

    public FramebufferObject<ContextType> execute(
        FramebufferObject<ContextType> initFrontFramebuffer, FramebufferObject<ContextType> initBackFramebuffer)
    {
        FramebufferObject<ContextType> frontFramebuffer = initFrontFramebuffer;
        FramebufferObject<ContextType> backFramebuffer = initBackFramebuffer;

        FramebufferSize fboSize = frontFramebuffer.getSize();
        int iterations = Math.max(fboSize.width, fboSize.height);
        for (int i = 0; i < iterations; i++)
        {
            // Loop over input / output channels
            for (int j = 0; j < initFrontFramebuffer.getColorAttachmentCount(); j++)
            {
                program.setTexture("input" + j, frontFramebuffer.getColorAttachmentTexture(j));
            }

            drawable.draw(PrimitiveMode.TRIANGLE_FAN, backFramebuffer);

            FramebufferObject<ContextType> tmp = frontFramebuffer;
            frontFramebuffer = backFramebuffer;
            backFramebuffer = tmp;

        }

        return frontFramebuffer;
    }
}
