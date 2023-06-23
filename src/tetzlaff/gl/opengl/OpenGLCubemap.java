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

import tetzlaff.gl.builders.base.ColorCubemapBuilderBase;
import tetzlaff.gl.builders.base.DepthStencilTextureBuilderBase;
import tetzlaff.gl.builders.base.DepthTextureBuilderBase;
import tetzlaff.gl.builders.base.StencilTextureBuilderBase;
import tetzlaff.gl.core.*;
import tetzlaff.gl.core.ColorFormat.DataType;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

public final class OpenGLCubemap extends OpenGLTexture implements Cubemap<OpenGLContext>
{
    private final int faceSize;
    private int levelCount;

    private static class FaceData
    {
        ByteBuffer buffer;
        NativeDataType dataType = NativeDataType.UNSIGNED_BYTE;
        int format;
    }

    static class ColorBuilder extends ColorCubemapBuilderBase<OpenGLContext, OpenGLCubemap>
    {
        private final int textureTarget;
        private final int faceSize;

        private final FaceData[] faces = new FaceData[6];

        private static int cubemapFaceToIndex(CubemapFace face)
        {
            switch(face)
            {
            case POSITIVE_X: return 0;
            case NEGATIVE_X: return 1;
            case POSITIVE_Y: return 2;
            case NEGATIVE_Y: return 3;
            case POSITIVE_Z: return 4;
            case NEGATIVE_Z: return 5;
            default: return -1; // Should never happen
            }
        }

        private static int getIntegerFormat(int format)
        {
            switch(format)
            {
                case GL_RED:  return GL_RED_INTEGER;
                case GL_RG:   return GL_RG_INTEGER;
                case GL_RGB:  return GL_RGB_INTEGER;
                case GL_RGBA: return GL_RGBA_INTEGER;
                default:      throw new IllegalArgumentException("Unrecognized format.");
            }
        }

        ColorBuilder(OpenGLContext context, int textureTarget, int faceSize)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.faceSize = faceSize;

            for (int i = 0; i < 6; i++)
            {
                faces[i] = new FaceData();
            }
        }

        @Override
        public ColorBuilder loadFace(CubemapFace face, NativeVectorBuffer data)
        {
            if (data.getCount() != faceSize * faceSize)
            {
                throw new IllegalArgumentException(
                    String.format("Native vector buffer does not have the required number of elements for this texture.  Expected: %d (%dx%d)  Actual: %d",
                        faceSize * faceSize, faceSize, faceSize, data.getCount()));
            }

            int index = cubemapFaceToIndex(face);
            faces[index].buffer = data.getBuffer();
            faces[index].dataType = data.getDataType();
            faces[index].format = OpenGLContext.getPixelDataFormatFromDimensions(data.getDimensions(), false);
            return this;
        }

        @Override
        public OpenGLCubemap createTexture()
        {
            for (int i = 0; i < 6; i++)
            {
                if(!this.isInternalFormatCompressed() &&
                        (this.getInternalColorFormat().dataType == DataType.SIGNED_INTEGER
                            || this.getInternalColorFormat().dataType == DataType.UNSIGNED_INTEGER))
                {
                    if (faces[i].format == 0)
                    {
                        faces[i].format = GL_RGBA_INTEGER;
                    }
                    else
                    {
                        faces[i].format = getIntegerFormat(faces[i].format);
                    }
                }
                else
                {
                    if (faces[i].format == 0)
                    {
                        faces[i].format = GL_RGBA;
                    }
                }
            }

            OptionalParameters opt = new OptionalParameters();
            opt.useLinearFiltering = this.isLinearFilteringEnabled();
            opt.useMipmaps = this.areMipmapsEnabled();
            opt.maxMipmapLevel = this.getMaxMipmapLevel();
            opt.maxAnisotropy = this.getMaxAnisotropy();
            opt.positiveX = faces[0];
            opt.negativeX = faces[1];
            opt.positiveY = faces[2];
            opt.negativeY = faces[3];
            opt.positiveZ = faces[4];
            opt.negativeZ = faces[5];

            if (this.isInternalFormatCompressed())
            {
                return new OpenGLCubemap(
                        this.context,
                        this.textureTarget,
                        this.getInternalCompressionFormat(),
                        this.faceSize,
                        opt);
            }
            else
            {
                return new OpenGLCubemap(
                        this.context,
                        this.textureTarget,
                        this.getInternalColorFormat(),
                        this.faceSize,
                        opt);
            }
        }
    }

    static class DepthBuilder extends DepthTextureBuilderBase<OpenGLContext, OpenGLCubemap>
    {
        private final int textureTarget;
        private final int faceSize;

        DepthBuilder(OpenGLContext context, int textureTarget, int faceSize)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.faceSize = faceSize;
        }

        @Override
        public OpenGLCubemap createTexture()
        {
            OptionalParameters opt = new OptionalParameters(GL_DEPTH_COMPONENT);
            opt.useLinearFiltering = this.isLinearFilteringEnabled();
            opt.useMipmaps = this.areMipmapsEnabled();
            opt.maxMipmapLevel = this.getMaxMipmapLevel();
            opt.maxAnisotropy = this.getMaxAnisotropy();

            return new OpenGLCubemap(
                    this.context,
                    this.textureTarget,
                    TextureType.DEPTH,
                    this.getInternalPrecision(),
                    this.faceSize,
                    opt);
        }
    }

    static class StencilBuilder extends StencilTextureBuilderBase<OpenGLContext, OpenGLCubemap>
    {
        private final int textureTarget;
        private final int faceSize;

        StencilBuilder(OpenGLContext context, int textureTarget, int faceSize)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.faceSize = faceSize;
        }

        @Override
        public OpenGLCubemap createTexture()
        {
            OptionalParameters opt = new OptionalParameters(GL_STENCIL_INDEX);
            opt.useLinearFiltering = this.isLinearFilteringEnabled();
            opt.useMipmaps = this.areMipmapsEnabled();
            opt.maxMipmapLevel = this.getMaxMipmapLevel();
            opt.maxAnisotropy = this.getMaxAnisotropy();

            return new OpenGLCubemap(
                    this.context,
                    this.textureTarget,
                    TextureType.STENCIL,
                    this.getInternalPrecision(),
                    this.faceSize,
                    opt);
        }
    }

    static class DepthStencilBuilder extends DepthStencilTextureBuilderBase<OpenGLContext, OpenGLCubemap>
    {
        private final int textureTarget;
        private final int faceSize;

        DepthStencilBuilder(OpenGLContext context, int textureTarget, int faceSize)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.faceSize = faceSize;
        }

        @Override
        public OpenGLCubemap createTexture()
        {
            OptionalParameters opt = new OptionalParameters(GL_DEPTH_STENCIL);
            opt.useLinearFiltering = this.isLinearFilteringEnabled();
            opt.useMipmaps = this.areMipmapsEnabled();
            opt.maxMipmapLevel = this.getMaxMipmapLevel();
            opt.maxAnisotropy = this.getMaxAnisotropy();

            return new OpenGLCubemap(
                    this.context,
                    this.textureTarget,
                    this.isFloatingPointEnabled() ? TextureType.FLOATING_POINT_DEPTH_STENCIL : TextureType.DEPTH_STENCIL,
                    this.isFloatingPointEnabled() ? 40 : 32,
                    this.faceSize,
                    opt);
        }
    }

    private static class OptionalParameters
    {
        FaceData positiveX = new FaceData();
        FaceData negativeX = new FaceData();
        FaceData positiveY = new FaceData();
        FaceData negativeY = new FaceData();
        FaceData positiveZ = new FaceData();
        FaceData negativeZ = new FaceData();

        boolean useLinearFiltering = false;
        boolean useMipmaps = false;
        int maxMipmapLevel = Integer.MAX_VALUE;
        float maxAnisotropy = 1.0f;

        OptionalParameters()
        {
        }

        OptionalParameters(int format)
        {
            positiveX.format = format;
            negativeX.format = format;
            positiveY.format = format;
            negativeY.format = format;
            positiveZ.format = format;
            negativeZ.format = format;
        }
    }

    private OpenGLCubemap(OpenGLContext context, int textureTarget, ColorFormat colorFormat, int faceSize, OptionalParameters opt)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, textureTarget, colorFormat, opt.useMipmaps);
        this.faceSize = faceSize;
        init(OpenGLContext.getOpenGLInternalColorFormat(colorFormat), opt);
    }

    private OpenGLCubemap(OpenGLContext context, int textureTarget, CompressionFormat compressionFormat, int faceSize, OptionalParameters opt)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, textureTarget, compressionFormat, opt.useMipmaps);
        this.faceSize = faceSize;
        init(OpenGLContext.getOpenGLCompressionFormat(compressionFormat), opt);
    }

    private OpenGLCubemap(OpenGLContext context, int textureTarget, TextureType textureType, int precision, int faceSize, OptionalParameters opt)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, textureTarget, textureType, opt.useMipmaps);
        this.faceSize = faceSize;
        init(getSpecialInternalFormat(textureType, precision), opt);
    }

    private void init(int internalFormat, OptionalParameters opt)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        this.bind();

        if (opt.positiveX.buffer == null)
        {
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, internalFormat, faceSize, faceSize, 0, opt.positiveX.format, GL_UNSIGNED_BYTE, 0);
            OpenGLContext.errorCheck();
        }
        else
        {
            int dataTypeConstant = OpenGLContext.getDataTypeConstant(opt.positiveX.dataType);

            glPixelStorei(GL_UNPACK_ALIGNMENT, getUnpackAlignment(opt.positiveX.format, dataTypeConstant));
            OpenGLContext.errorCheck();

            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, internalFormat, faceSize, faceSize, 0, opt.positiveX.format, dataTypeConstant, opt.positiveX.buffer);
            OpenGLContext.errorCheck();
        }

        if (opt.negativeX.buffer == null)
        {
            glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, internalFormat, faceSize, faceSize, 0, opt.negativeX.format, GL_UNSIGNED_BYTE, 0);
            OpenGLContext.errorCheck();
        }
        else
        {
            int dataTypeConstant = OpenGLContext.getDataTypeConstant(opt.negativeX.dataType);

//            glPixelStorei(GL_UNPACK_ALIGNMENT, getUnpackAlignment(opt.negativeX.format, dataTypeConstant));
//            OpenGLContext.errorCheck();

            glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, internalFormat, faceSize, faceSize, 0, opt.negativeX.format, dataTypeConstant, opt.negativeX.buffer);
            OpenGLContext.errorCheck();
        }

        if (opt.positiveY.buffer == null)
        {
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, internalFormat, faceSize, faceSize, 0, opt.positiveY.format, GL_UNSIGNED_BYTE, 0);
            OpenGLContext.errorCheck();
        }
        else
        {
            int dataTypeConstant = OpenGLContext.getDataTypeConstant(opt.positiveY.dataType);

//            glPixelStorei(GL_UNPACK_ALIGNMENT, getUnpackAlignment(opt.positiveY.format, dataTypeConstant));
//            OpenGLContext.errorCheck();

            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, internalFormat, faceSize, faceSize, 0, opt.positiveY.format, dataTypeConstant, opt.positiveY.buffer);
            OpenGLContext.errorCheck();
        }

        if (opt.negativeY.buffer == null)
        {
            glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, internalFormat, faceSize, faceSize, 0, opt.negativeY.format, GL_UNSIGNED_BYTE, 0);
            OpenGLContext.errorCheck();
        }
        else
        {
            int dataTypeConstant = OpenGLContext.getDataTypeConstant(opt.negativeY.dataType);

//            glPixelStorei(GL_UNPACK_ALIGNMENT, getUnpackAlignment(opt.negativeY.format, dataTypeConstant));
//            OpenGLContext.errorCheck();

            glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, internalFormat, faceSize, faceSize, 0, opt.negativeY.format, dataTypeConstant, opt.negativeY.buffer);
            OpenGLContext.errorCheck();
        }

        if (opt.positiveZ.buffer == null)
        {
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, internalFormat, faceSize, faceSize, 0, opt.positiveZ.format, GL_UNSIGNED_BYTE, 0);
            OpenGLContext.errorCheck();
        }
        else
        {
            int dataTypeConstant = OpenGLContext.getDataTypeConstant(opt.positiveZ.dataType);

//            glPixelStorei(GL_UNPACK_ALIGNMENT, getUnpackAlignment(opt.positiveZ.format, dataTypeConstant));
//            OpenGLContext.errorCheck();

            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, internalFormat, faceSize, faceSize, 0, opt.positiveZ.format, dataTypeConstant, opt.positiveZ.buffer);
            OpenGLContext.errorCheck();
        }

        if (opt.negativeZ.buffer == null)
        {
            glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, internalFormat, faceSize, faceSize, 0, opt.negativeZ.format, GL_UNSIGNED_BYTE, 0);
            OpenGLContext.errorCheck();
        }
        else
        {
            int dataTypeConstant = OpenGLContext.getDataTypeConstant(opt.negativeZ.dataType);

//            glPixelStorei(GL_UNPACK_ALIGNMENT, getUnpackAlignment(opt.negativeZ.format, dataTypeConstant));
//            OpenGLContext.errorCheck();

            glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, internalFormat, faceSize, faceSize, 0, opt.negativeZ.format, dataTypeConstant, opt.negativeZ.buffer);
            OpenGLContext.errorCheck();
        }

        this.initFilteringAndMipmaps(opt.useLinearFiltering, opt.maxMipmapLevel);

        if (opt.maxAnisotropy > 1.0f)
        {
            glTexParameterf(getOpenGLTextureTarget(), GL_TEXTURE_MAX_ANISOTROPY_EXT, opt.maxAnisotropy);
            OpenGLContext.errorCheck();
        }
    }

    @Override
    void initFilteringAndMipmaps(boolean useLinearFiltering, int maxMipmapLevel, boolean generateMipmaps)
    {
        super.initFilteringAndMipmaps(useLinearFiltering, maxMipmapLevel, generateMipmaps);

        if (useMipmaps)
        {
            // Calculate the number of mipmap levels
            this.levelCount = 0;
            int dim = faceSize;
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

        glTexParameteri(getOpenGLTextureTarget(), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        OpenGLContext.errorCheck();
        
        glTexParameteri(getOpenGLTextureTarget(), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        OpenGLContext.errorCheck();
        
        glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
        OpenGLContext.errorCheck();
    }

    @Override
    public int getFaceSize()
    {
        return this.faceSize;
    }

    @Override
    public int getMipmapLevelCount()
    {
        return this.levelCount;
    }

    @Override
    public void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT)
    {
        this.bind();
        int numericWrapS = translateWrapMode(wrapS);
        int numericWrapT = translateWrapMode(wrapT);

        if (numericWrapS != 0)
        {
            glTexParameteri(getOpenGLTextureTarget(), GL_TEXTURE_WRAP_S, numericWrapS);
            OpenGLContext.errorCheck();
        }

        if (numericWrapT != 0)
        {
            glTexParameteri(getOpenGLTextureTarget(), GL_TEXTURE_WRAP_T, numericWrapT);
            OpenGLContext.errorCheck();
        }
    }

    @Override
    public FramebufferAttachment<OpenGLContext> getFaceAsFramebufferAttachment(CubemapFace face)
    {
        int textureId = this.getTextureId();

        int layerIndex;

        switch(face)
        {
        case POSITIVE_X: layerIndex = 0; break;
        case NEGATIVE_X: layerIndex = 1; break;
        case POSITIVE_Y: layerIndex = 2; break;
        case NEGATIVE_Y: layerIndex = 3; break;
        case POSITIVE_Z: layerIndex = 4; break;
        case NEGATIVE_Z: layerIndex = 5; break;
        default: layerIndex = -1; break; // Should never happen
        }

        return new OpenGLFramebufferAttachment()
        {
            @Override
            public OpenGLContext getContext()
            {
                return context;
            }

            @Override
            public void attachToDrawFramebuffer(int attachment, int level)
            {
                glFramebufferTextureLayer(GL_DRAW_FRAMEBUFFER, attachment, textureId, level, layerIndex);
                OpenGLContext.errorCheck();
            }

            @Override
            public void attachToReadFramebuffer(int attachment, int level)
            {
                glFramebufferTextureLayer(GL_READ_FRAMEBUFFER, attachment, textureId, level, layerIndex);
                OpenGLContext.errorCheck();
            }

        };
    }
}
