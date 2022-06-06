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

package tetzlaff.gl.opengl;

import java.nio.ByteBuffer;

import tetzlaff.gl.builders.base.ColorTextureBuilderBase;
import tetzlaff.gl.core.ColorFormat;
import tetzlaff.gl.core.ColorFormat.DataType;
import tetzlaff.gl.core.CompressionFormat;
import tetzlaff.gl.core.Texture1D;
import tetzlaff.gl.core.TextureWrapMode;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL44.*;

// mipmaps

final class OpenGLTexture1D extends OpenGLTexture implements Texture1D<OpenGLContext>
{
    private final int width;
    private int levelCount;

    static class OpenGLTexture1DFromBufferBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture1D>
    {
        private final int textureTarget;
        private final int width;
        private final int dimensions;
        private final int type;
        private final ByteBuffer buffer;

        OpenGLTexture1DFromBufferBuilder(OpenGLContext context, int textureTarget, int width, int dimensions, int type, ByteBuffer buffer)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.width = width;
            this.dimensions = dimensions;
            this.type = type;
            this.buffer = buffer;
        }

        @Override
        public OpenGLTexture1D createTexture()
        {
            if (this.isInternalFormatCompressed())
            {
                return new OpenGLTexture1D(
                        this.context,
                        this.textureTarget,
                        this.getInternalCompressionFormat(),
                        this.width,
                        OpenGLContext.getPixelDataFormatFromDimensions(this.dimensions, false),
                        this.type,
                        this.buffer,
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
                        this.getMaxMipmapLevel(),
                        this.getMaxAnisotropy());
            }
            else
            {
                return new OpenGLTexture1D(
                        this.context,
                        this.textureTarget,
                        this.getInternalColorFormat(),
                        this.width,
                        OpenGLContext.getPixelDataFormatFromDimensions(
                            this.dimensions,
                            this.getInternalColorFormat().dataType == DataType.SIGNED_INTEGER
                                || this.getInternalColorFormat().dataType == DataType.UNSIGNED_INTEGER),
                        this.type,
                        this.buffer,
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
                        this.getMaxMipmapLevel(),
                        this.getMaxAnisotropy());
            }
        }
    }

    private OpenGLTexture1D(OpenGLContext context, int textureTarget, ColorFormat colorFormat, int width, int format, int type, ByteBuffer buffer,
            boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, textureTarget, colorFormat, useMipmaps);
        this.width = width;

        init(textureTarget, OpenGLContext.getOpenGLInternalColorFormat(colorFormat), format, type, buffer, useLinearFiltering, maxMipmapLevel, maxAnisotropy);
    }

    private OpenGLTexture1D(OpenGLContext context, int textureTarget, CompressionFormat compressionFormat, int width, int format, int type, ByteBuffer buffer,
            boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, textureTarget, compressionFormat, useMipmaps);
        this.width = width;

        init(textureTarget, OpenGLContext.getOpenGLCompressionFormat(compressionFormat), format, type, buffer, useLinearFiltering, maxMipmapLevel, maxAnisotropy);
    }

    private void init(int textureTarget, int internalFormat, int format, int type, ByteBuffer buffer,
            boolean useLinearFiltering, int maxMipmapLevel, float maxAnisotropy)
    {
        this.bind();

        if (type == GL_UNSIGNED_SHORT_5_6_5 || type == GL_UNSIGNED_SHORT_5_6_5_REV || type == GL_UNSIGNED_SHORT_4_4_4_4 ||
                 type == GL_UNSIGNED_SHORT_4_4_4_4_REV || type == GL_UNSIGNED_SHORT_5_5_5_1 || type == GL_UNSIGNED_SHORT_1_5_5_5_REV)
        {
            glPixelStorei(GL_UNPACK_ALIGNMENT, 2);
            OpenGLContext.errorCheck();
        }
        else if (format == GL_RGBA || format == GL_BGRA || format == GL_RGBA_INTEGER || format == GL_BGRA_INTEGER || type == GL_UNSIGNED_INT || type == GL_INT || type == GL_FLOAT ||
                type == GL_UNSIGNED_INT_8_8_8_8 || type == GL_UNSIGNED_INT_8_8_8_8_REV || type == GL_UNSIGNED_INT_10_10_10_2 || type == GL_UNSIGNED_INT_2_10_10_10_REV)
        {
            glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
            OpenGLContext.errorCheck();
        }
        else if (type == GL_UNSIGNED_SHORT || type == GL_SHORT)
        {
            glPixelStorei(GL_UNPACK_ALIGNMENT, 2);
            OpenGLContext.errorCheck();
        }
        else
        {
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            OpenGLContext.errorCheck();
        }

        glTexImage1D(textureTarget, 0, internalFormat, width, 0, format, type, buffer);
        OpenGLContext.errorCheck();

        if (useMipmaps)
        {
            // Calculate the number of mipmap levels
            this.levelCount = 0;
            int dim = width;
            while (dim > 0)
            {
                this.levelCount++;
                dim /= 2;
            }
        }
        else
        {
            // No mipmaps
            this.levelCount = 1;
        }

        this.initFilteringAndMipmaps(useLinearFiltering, maxMipmapLevel);

        glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        OpenGLContext.errorCheck();

        if (maxAnisotropy > 1.0f)
        {
            glTexParameterf(textureTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
            OpenGLContext.errorCheck();
        }
    }

    @Override
    public int getWidth()
    {
        return this.width;
    }

    @Override
    public int getMipmapLevelCount()
    {
        return this.levelCount;
    }

    @Override
    public void setTextureWrap(TextureWrapMode wrap)
    {
        this.bind();
        switch(wrap)
        {
        case None:
            glTexParameteri(getOpenGLTextureTarget(), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            OpenGLContext.errorCheck();
            break;
        case MirrorOnce:
            glTexParameteri(getOpenGLTextureTarget(), GL_TEXTURE_WRAP_S, GL_MIRROR_CLAMP_TO_EDGE);
            OpenGLContext.errorCheck();
            break;
        case Repeat:
            glTexParameteri(getOpenGLTextureTarget(), GL_TEXTURE_WRAP_S, GL_REPEAT);
            OpenGLContext.errorCheck();
            break;
        case MirroredRepeat:
            glTexParameteri(getOpenGLTextureTarget(), GL_TEXTURE_WRAP_S, GL_MIRRORED_REPEAT);
            OpenGLContext.errorCheck();
            break;
        }
    }
}
