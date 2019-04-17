/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.gl.opengl;

import umn.gl.core.SamplerType;
import umn.gl.core.TextureType;

public class OpenGLNullTexture extends OpenGLTexture
{
    private SamplerType type;

    OpenGLNullTexture(OpenGLContext context, SamplerType type)
    {
        super(context, TextureType.NULL);
        this.type = type;
    }

    @Override
    int getOpenGLTextureTarget()
    {
        return OpenGLTexture.translateSamplerType(type);
    }

    @Override
    public int getMipmapLevelCount()
    {
        return 0;
    }
}
