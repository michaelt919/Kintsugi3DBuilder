/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.gl.geometry;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.gl.core.Program;
import tetzlaff.gl.core.ShaderType;

public abstract class GeometryTexturesBase<ContextType extends Context<ContextType>> implements GeometryTextures<ContextType>
{
    private final ContextType context;

    protected GeometryTexturesBase(ContextType context)
    {
        this.context = context;
    }

    @Override
    public ContextType getContext()
    {
        return context;
    }

    @Override
    public void setupShaderProgram(Program<ContextType> program)
    {
        program.setTexture("positionTex", getPositionTexture());
        program.setTexture("normalTex", getNormalTexture());
        program.setTexture("tangentTex", getTangentTexture());
    }

//    public GeometryTextures<ContextType> viewport(int x, int y, int width, int height)
//    {
//        // TODO implement this using framebuffer blitting
//    }
}
