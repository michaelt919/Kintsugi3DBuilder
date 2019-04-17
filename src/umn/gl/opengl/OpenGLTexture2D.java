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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.imageio.ImageIO;

import umn.gl.builders.base.ColorTextureBuilderBase;
import umn.gl.builders.base.DepthStencilTextureBuilderBase;
import umn.gl.builders.base.DepthTextureBuilderBase;
import umn.gl.builders.base.StencilTextureBuilderBase;
import umn.gl.core.*;
import umn.gl.core.ColorFormat.DataType;
import umn.gl.types.AbstractDataType;
import umn.util.RadianceImageLoader;
import umn.util.RadianceImageLoader.Image;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

// mipmaps

final class OpenGLTexture2D extends OpenGLTexture implements Texture2D<OpenGLContext>
{
    private int openGLTextureTarget;
    private int width;
    private int height;
    private int mipmapLevelCount;

    static class OpenGLTexture2DFromFileBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private final int textureTarget;
        private final BufferedImage colorImg;
        private BufferedImage maskImg;
        private final boolean flipVertical;

        OpenGLTexture2DFromFileBuilder(OpenGLContext context, int textureTarget, BufferedImage colorImg, BufferedImage maskImg, boolean flipVertical)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.colorImg = colorImg;
            this.maskImg = maskImg;
            if (maskImg != null)
            {
                if (maskImg.getWidth() != colorImg.getWidth() || maskImg.getHeight() != colorImg.getHeight())
                {
                    throw new IllegalArgumentException("Color image and mask image must have the same dimensions.");
                }
            }
            this.flipVertical = flipVertical;
        }

        OpenGLTexture2DFromFileBuilder(OpenGLContext context, int textureTarget, InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
        {
            super(context);
            this.textureTarget = textureTarget;
            this.colorImg = ImageIO.read(imageStream);
            if (maskStream != null)
            {
                this.maskImg = ImageIO.read(maskStream);
                if (maskImg.getWidth() != colorImg.getWidth() || maskImg.getHeight() != colorImg.getHeight())
                {
                    throw new IllegalArgumentException("Color image and mask image must have the same dimensions.");
                }
            }
            this.flipVertical = flipVertical;
        }

        @Override
        public OpenGLTexture2D createTexture()
        {
            int width = colorImg.getWidth();
            int height = colorImg.getHeight();
            ByteBuffer buffer = OpenGLTexture.bufferedImageToNativeBuffer(colorImg, maskImg, flipVertical);

            if (this.isInternalFormatCompressed())
            {
                return new OpenGLTexture2D(
                        this.context,
                        this.textureTarget,
                        this.getInternalCompressionFormat(),
                        width,
                        height,
                        GL_BGRA,
                        GL_UNSIGNED_BYTE,
                        buffer,
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
                        this.getMaxMipmapLevel(),
                        this.getMaxAnisotropy());
            }
            else
            {
                return new OpenGLTexture2D(
                        this.context,
                        this.textureTarget,
                        this.getInternalColorFormat(),
                        width,
                        height,
                        GL_BGRA,
                        GL_UNSIGNED_BYTE,
                        buffer,
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
                        this.getMaxMipmapLevel(),
                        this.getMaxAnisotropy());
            }
        }
    }

    static class OpenGLTexture2DMappedFromFileBuilder<MappedType> extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private final int textureTarget;
        private final BufferedImage colorImg;
        private BufferedImage maskImg;
        private final boolean flipVertical;
        private final AbstractDataType<? super MappedType> mappedType;
        private final Function<Color, MappedType> mappingFunction;

        OpenGLTexture2DMappedFromFileBuilder(OpenGLContext context, int textureTarget, BufferedImage colorImg, BufferedImage maskImg, boolean flipVertical,
            AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.colorImg = colorImg;
            this.maskImg = maskImg;
            if (maskImg != null)
            {
                if (maskImg.getWidth() != colorImg.getWidth() || maskImg.getHeight() != colorImg.getHeight())
                {
                    throw new IllegalArgumentException("Color image and mask image must have the same dimensions.");
                }
            }
            this.flipVertical = flipVertical;
            this.mappedType = mappedType;
            this.mappingFunction = mappingFunction;
        }

        OpenGLTexture2DMappedFromFileBuilder(OpenGLContext context, int textureTarget, InputStream imageStream, InputStream maskStream, boolean flipVertical,
            AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
        {
            super(context);
            this.textureTarget = textureTarget;
            this.colorImg = ImageIO.read(imageStream);
            if (maskStream != null)
            {
                this.maskImg = ImageIO.read(maskStream);
                if (maskImg.getWidth() != colorImg.getWidth() || maskImg.getHeight() != colorImg.getHeight())
                {
                    throw new IllegalArgumentException("Color image and mask image must have the same dimensions.");
                }
            }
            this.flipVertical = flipVertical;
            this.mappedType = mappedType;
            this.mappingFunction = mappingFunction;
        }

        @Override
        public OpenGLTexture2D createTexture()
        {
            int width = colorImg.getWidth();
            int height = colorImg.getHeight();

            int format = OpenGLContext.getPixelDataFormatFromDimensions(
                mappedType.getComponentCount(),
                !this.isInternalFormatCompressed() &&
                    (this.getInternalColorFormat().dataType == DataType.SIGNED_INTEGER
                        || this.getInternalColorFormat().dataType == DataType.UNSIGNED_INTEGER));
            int type = OpenGLContext.getDataTypeConstant(mappedType);
            Function<ByteBuffer, Consumer<? super MappedType>> bufferWrapperFunctionPartial = mappedType::wrapByteBuffer;
            int mappedColorLength = mappedType.getSizeInBytes();

            Function<ByteBuffer, Consumer<Color>> bufferWrapperFunctionFull = byteBuffer ->
            {
                Consumer<? super MappedType> partiallyWrappedBuffer = bufferWrapperFunctionPartial.apply(byteBuffer);
                return color -> partiallyWrappedBuffer.accept(mappingFunction.apply(color));
            };

            ByteBuffer buffer = OpenGLTexture.bufferedImageToNativeBuffer(colorImg, maskImg, flipVertical, bufferWrapperFunctionFull, mappedColorLength);

            if (this.isInternalFormatCompressed())
            {
                return new OpenGLTexture2D(
                    this.context,
                    this.textureTarget,
                    this.getInternalCompressionFormat(),
                    width,
                    height,
                    format,
                    type,
                    buffer,
                    this.isLinearFilteringEnabled(),
                    this.areMipmapsEnabled(),
                    this.getMaxMipmapLevel(),
                    this.getMaxAnisotropy());
            }
            else
            {
                return new OpenGLTexture2D(
                    this.context,
                    this.textureTarget,
                    this.getInternalColorFormat(),
                    width,
                    height,
                    format,
                    type,
                    buffer,
                    this.isLinearFilteringEnabled(),
                    this.areMipmapsEnabled(),
                    this.getMaxMipmapLevel(),
                    this.getMaxAnisotropy());
            }
        }
    }

    static class OpenGLTexture2DFromHDRFileBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private final int textureTarget;
        private final Image colorImg;
        private BufferedImage maskImg;

        OpenGLTexture2DFromHDRFileBuilder(OpenGLContext context, int textureTarget, BufferedInputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
        {
            super(context);
            this.textureTarget = textureTarget;
            this.colorImg = new RadianceImageLoader().read(imageStream, flipVertical, true);
            if (maskStream != null)
            {
                this.maskImg = ImageIO.read(maskStream);
                if (maskImg.getWidth() != colorImg.width || maskImg.getHeight() != colorImg.height)
                {
                    throw new IllegalArgumentException("Color image and mask image must have the same dimensions.");
                }
            }
        }

        @Override
        public OpenGLTexture2D createTexture()
        {
            int width = colorImg.width;
            int height = colorImg.height;

            ByteBuffer buffer = OpenGLTexture.hdrImageToNativeBuffer(colorImg, maskImg);

            if (this.isInternalFormatCompressed())
            {
                return new OpenGLTexture2D(
                        this.context,
                        this.textureTarget,
                        this.getInternalCompressionFormat(),
                        width,
                        height,
                        maskImg == null ? GL_RGB : GL_RGBA,
                        GL_FLOAT,
                        buffer,
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
                        this.getMaxMipmapLevel(),
                        this.getMaxAnisotropy());
            }
            else
            {
                return new OpenGLTexture2D(
                        this.context,
                        this.textureTarget,
                        this.getInternalColorFormat(),
                        width,
                        height,
                        maskImg == null ? GL_RGB : GL_RGBA,
                        GL_FLOAT,
                        buffer,
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
                        this.getMaxMipmapLevel(),
                        this.getMaxAnisotropy());
            }
        }
    }

    static class OpenGLTexture2DFromBufferBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private final int textureTarget;
        private final int width;
        private final int height;
        private final int dimensions;
        private final int type;
        private final ByteBuffer buffer;

        OpenGLTexture2DFromBufferBuilder(OpenGLContext context, int textureTarget, int width, int height, int dimensions, int type, ByteBuffer buffer)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.width = width;
            this.height = height;
            this.dimensions = dimensions;
            this.type = type;
            this.buffer = buffer;
        }

        @Override
        public OpenGLTexture2D createTexture()
        {
            if (this.isInternalFormatCompressed())
            {
                return new OpenGLTexture2D(
                        this.context,
                        this.textureTarget,
                        this.getInternalCompressionFormat(),
                        this.width,
                        this.height,
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
                return new OpenGLTexture2D(
                        this.context,
                        this.textureTarget,
                        this.getInternalColorFormat(),
                        this.width,
                        this.height,
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

    static class OpenGLTexture2DColorBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private final int textureTarget;
        private final int width;
        private final int height;

        OpenGLTexture2DColorBuilder(OpenGLContext context, int textureTarget, int width, int height)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.width = width;
            this.height = height;
        }

        @Override
        public OpenGLTexture2D createTexture()
        {
            if (this.isInternalFormatCompressed())
            {
                return new OpenGLTexture2D(
                        this.context,
                        this.textureTarget,
                        this.getMultisamples(),
                        this.getInternalCompressionFormat(),
                        this.width,
                        this.height,
                        GL_RGBA,
                        this.areMultisampleLocationsFixed(),
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
                        this.getMaxMipmapLevel(),
                        this.getMaxAnisotropy());
            }
            else
            {
                return new OpenGLTexture2D(
                        this.context,
                        this.textureTarget,
                        this.getMultisamples(),
                        this.getInternalColorFormat(),
                        this.width,
                        this.height,
                        (this.getInternalColorFormat().dataType == DataType.SIGNED_INTEGER
                            || this.getInternalColorFormat().dataType == DataType.UNSIGNED_INTEGER)
                                ? GL_RGBA_INTEGER : GL_RGBA,
                        this.areMultisampleLocationsFixed(),
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
                        this.getMaxMipmapLevel(),
                        this.getMaxAnisotropy());
            }
        }
    }

    static class OpenGLTexture2DDepthBuilder extends DepthTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private final int textureTarget;
        private final int width;
        private final int height;

        OpenGLTexture2DDepthBuilder(OpenGLContext context, int textureTarget, int width, int height)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.width = width;
            this.height = height;
        }

        @Override
        public OpenGLTexture2D createTexture()
        {
            return new OpenGLTexture2D(
                    this.context,
                    this.textureTarget,
                    this.getMultisamples(),
                    TextureType.DEPTH,
                    this.getInternalPrecision(),
                    this.width,
                    this.height,
                    GL_DEPTH_COMPONENT,
                    this.areMultisampleLocationsFixed(),
                    this.isLinearFilteringEnabled(),
                    this.areMipmapsEnabled(),
                    this.getMaxMipmapLevel(),
                    this.getMaxAnisotropy());
        }
    }

    static class OpenGLTexture2DStencilBuilder extends StencilTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private final int textureTarget;
        private final int width;
        private final int height;

        OpenGLTexture2DStencilBuilder(OpenGLContext context, int textureTarget, int width, int height)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.width = width;
            this.height = height;
        }

        @Override
        public OpenGLTexture2D createTexture()
        {
            return new OpenGLTexture2D(
                    this.context,
                    this.textureTarget,
                    this.getMultisamples(),
                    TextureType.STENCIL,
                    this.getInternalPrecision(),
                    this.width,
                    this.height,
                    GL_STENCIL_INDEX,
                    this.areMultisampleLocationsFixed(),
                    this.isLinearFilteringEnabled(),
                    this.areMipmapsEnabled(),
                    this.getMaxMipmapLevel(),
                    this.getMaxAnisotropy());
        }
    }

    static class OpenGLTexture2DDepthStencilBuilder extends DepthStencilTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private final int textureTarget;
        private final int width;
        private final int height;

        OpenGLTexture2DDepthStencilBuilder(OpenGLContext context, int textureTarget, int width, int height)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.width = width;
            this.height = height;
        }

        @Override
        public OpenGLTexture2D createTexture()
        {
            return new OpenGLTexture2D(
                    this.context,
                    this.textureTarget,
                    this.getMultisamples(),
                    this.isFloatingPointEnabled() ? TextureType.FLOATING_POINT_DEPTH_STENCIL : TextureType.DEPTH_STENCIL,
                    this.isFloatingPointEnabled() ? 40 : 32,
                    this.width,
                    this.height,
                    GL_DEPTH_STENCIL,
                    this.areMultisampleLocationsFixed(),
                    this.isLinearFilteringEnabled(),
                    this.areMipmapsEnabled(),
                    this.getMaxMipmapLevel(),
                    this.getMaxAnisotropy());
        }
    }

    private OpenGLTexture2D(OpenGLContext context, int openGLTextureTarget, ColorFormat colorFormat, int width, int height, int format, int type, ByteBuffer buffer,
            boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, colorFormat);
        init(openGLTextureTarget, OpenGLContext.getOpenGLInternalColorFormat(colorFormat), width, height, format, type, buffer,
                useLinearFiltering, useMipmaps, maxMipmapLevel, maxAnisotropy);
    }

    private OpenGLTexture2D(OpenGLContext context, int openGLTextureTarget, CompressionFormat compressionFormat, int width, int height, int format, int type, ByteBuffer buffer,
            boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, compressionFormat);
        init(openGLTextureTarget, OpenGLContext.getOpenGLCompressionFormat(compressionFormat), width, height, format, type, buffer,
                useLinearFiltering, useMipmaps, maxMipmapLevel, maxAnisotropy);
    }

    private OpenGLTexture2D(OpenGLContext context, int openGLTextureTarget, int multisamples, ColorFormat colorFormat, int width, int height, int format,
            boolean fixedMultisampleLocations, boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, colorFormat);
        init(openGLTextureTarget, multisamples, OpenGLContext.getOpenGLInternalColorFormat(colorFormat), width, height, format,
                fixedMultisampleLocations, useLinearFiltering, useMipmaps, maxMipmapLevel, maxAnisotropy);
    }

    private OpenGLTexture2D(OpenGLContext context, int openGLTextureTarget, int multisamples, CompressionFormat compressionFormat, int width, int height, int format,
            boolean fixedMultisampleLocations, boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, compressionFormat);
        init(openGLTextureTarget, multisamples, OpenGLContext.getOpenGLCompressionFormat(compressionFormat), width, height, format,
                fixedMultisampleLocations, useLinearFiltering, useMipmaps, maxMipmapLevel, maxAnisotropy);
    }

    // TODO way too many parameters and too much repeated code across various texture subtypes
    private OpenGLTexture2D(OpenGLContext context, int openGLTextureTarget, int multisamples, TextureType textureType, int precision, int width, int height, int format,
            boolean fixedMultisampleLocations, boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, textureType);
        init(openGLTextureTarget, multisamples, getSpecialInternalFormat(textureType, precision), width, height, format,
                fixedMultisampleLocations, useLinearFiltering, useMipmaps, maxMipmapLevel, maxAnisotropy);
    }

    private void init(int textureTarget, int multisamples, int internalFormat, int width, int height, int format,
            boolean fixedMultisampleLocations, boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        this.openGLTextureTarget = textureTarget;
        this.bind();
        this.width = width;
        this.height = height;
        if (textureTarget == GL_TEXTURE_2D && multisamples > 1)
        {
            this.openGLTextureTarget = GL_TEXTURE_2D_MULTISAMPLE;
            this.mipmapLevelCount = 1;
            glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, multisamples, internalFormat, width, height, fixedMultisampleLocations);
            OpenGLContext.errorCheck();
            this.initFilteringAndMipmaps(false, false, 0); // linear filtering and mipmaps not allowed with multisampling
            // TODO: multisample textures don't seem to work correctly
        }
        else
        {
            // Last four parameters are essentially meaningless, but are subject to certain validation conditions
            glTexImage2D(textureTarget, 0, internalFormat, width, height, 0, format, GL_UNSIGNED_BYTE, 0);
            OpenGLContext.errorCheck();
            this.initFilteringAndMipmaps(useLinearFiltering, useMipmaps, maxMipmapLevel);
        }

        if (maxAnisotropy > 1.0f)
        {
            glTexParameterf(textureTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
            OpenGLContext.errorCheck();
        }
    }

    private void init(int textureTarget, int internalFormat, int width, int height, int format, int type, ByteBuffer buffer,
            boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
    {
        this.openGLTextureTarget = textureTarget;
        this.bind();
        this.width = width;
        this.height = height;

        glPixelStorei(GL_UNPACK_ALIGNMENT, OpenGLTexture.getUnpackAlignment(format, type));
        OpenGLContext.errorCheck();

        glTexImage2D(textureTarget, 0, internalFormat, width, height, 0, format, type, buffer);
        OpenGLContext.errorCheck();
        this.initFilteringAndMipmaps(useLinearFiltering, useMipmaps, maxMipmapLevel);

        if (maxAnisotropy > 1.0f)
        {
            glTexParameterf(textureTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
            OpenGLContext.errorCheck();
        }
    }

    @Override
    void initFilteringAndMipmaps(boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, boolean generateMipmaps)
    {
        super.initFilteringAndMipmaps(useLinearFiltering, useMipmaps, maxMipmapLevel, generateMipmaps);

        if (useMipmaps)
        {
            // Calculate the number of mipmap levels
            this.mipmapLevelCount = 0;
            int dim = Math.max(width, height);
            while (dim > 0 && this.mipmapLevelCount < maxMipmapLevel)
            {
                this.mipmapLevelCount++;
                dim /= 2;
            }
        }
        else
        {
            // No mipmaps
            this.mipmapLevelCount = 1;
        }

        glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        OpenGLContext.errorCheck();
        
        glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        OpenGLContext.errorCheck();
    }

    @Override
    public int getWidth()
    {
        return this.width;
    }

    @Override
    public int getHeight()
    {
        return this.height;
    }

    @Override
    protected int getOpenGLTextureTarget()
    {
        return this.openGLTextureTarget;
    }

    @Override
    public int getMipmapLevelCount()
    {
        return this.mipmapLevelCount;
    }

    @Override
    public void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT)
    {
        this.bind();
        int numericWrapS = translateWrapMode(wrapS);
        int numericWrapT = translateWrapMode(wrapT);

        if (numericWrapS != 0)
        {
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_S, numericWrapS);
            OpenGLContext.errorCheck();
        }

        if (numericWrapT != 0)
        {
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_T, numericWrapT);
            OpenGLContext.errorCheck();
        }
    }
}
