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

package kintsugi3d.gl.opengl;

import kintsugi3d.gl.core.*;
import kintsugi3d.gl.core.ColorFormat.DataType;
import kintsugi3d.util.RadianceImageLoader.Image;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.glFramebufferTexture;
import static org.lwjgl.opengl.GL44.GL_MIRROR_CLAMP_TO_EDGE;

abstract class OpenGLTexture implements Texture<OpenGLContext>, OpenGLFramebufferAttachment {
    protected final OpenGLContext context;

    private final int textureId;
    private boolean closed = false;

    protected final int openGLTextureTarget;
    protected boolean staleMipmaps = false;

    static final class Parameters
    {
        final int multisamples;
        final int format;
        final boolean fixedMultisampleLocations;
        final boolean useLinearFiltering;
        final boolean useMipmaps;
        final int maxMipmapLevel;
        final float maxAnisotropy;

        /**
         * Default constructor for a null texture
         */
        Parameters()
        {
            this(0, false, false, 0, 0.0f, 0, true);
        }

        Parameters(int format, boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy, int multisamples, boolean fixedMultisampleLocations)
        {
            this.format = format;
            this.useLinearFiltering = useLinearFiltering;
            this.useMipmaps = useMipmaps;
            this.maxMipmapLevel = maxMipmapLevel;
            this.maxAnisotropy = maxAnisotropy;
            this.multisamples = multisamples;
            this.fixedMultisampleLocations = fixedMultisampleLocations;
        }

        Parameters(int format, boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
        {
            this(format, useLinearFiltering, useMipmaps, maxMipmapLevel, maxAnisotropy, 1, true);
        }
    }

    final Parameters parameters;

    private final ColorFormat colorFormat;
    private final CompressionFormat compressionFormat;
    final int precision;
    private final TextureType textureType;

    private OpenGLTexture(OpenGLContext context, int openGLTextureTarget, ColorFormat colorFormat,
        CompressionFormat compressionFormat, TextureType textureType, int precision, Parameters parameters)
    {
        this.context = context;
        this.openGLTextureTarget = openGLTextureTarget;
        this.textureType = textureType;
        this.colorFormat = colorFormat;
        this.compressionFormat = compressionFormat;
        this.precision = precision;
        this.parameters = parameters;

        if (textureType == TextureType.NULL)
        {
            this.textureId = 0;
        }
        else
        {
            this.textureId = glGenTextures();
            OpenGLContext.errorCheck();
        }
    }

    OpenGLTexture(OpenGLContext context, int openGLTextureTarget, TextureType textureType, int precision, Parameters parameters)
    {
        this(context, openGLTextureTarget, null, null, textureType, precision, parameters);
    }

    OpenGLTexture(OpenGLContext context, int openGLTextureTarget, ColorFormat colorFormat, Parameters parameters)
    {
        this(context, openGLTextureTarget, colorFormat, null, TextureType.COLOR, 0, parameters);
    }

    OpenGLTexture(OpenGLContext context, int openGLTextureTarget, CompressionFormat compressionFormat, Parameters parameters)
    {
        this(context, openGLTextureTarget, null, compressionFormat, TextureType.COLOR, 0, parameters);
    }

    @Override
    public OpenGLContext getContext()
    {
        return this.context;
    }

    @Override
    public ColorFormat getInternalUncompressedColorFormat()
    {
        return this.colorFormat;
    }

    @Override
    public CompressionFormat getInternalCompressedColorFormat()
    {
        return this.compressionFormat;
    }

    @Override
    public boolean isInternalFormatCompressed()
    {
        return this.compressionFormat != null;
    }

    @Override
    public TextureType getTextureType()
    {
        return textureType;
    }

    protected int getOpenGLTextureTarget()
    {
        return this.openGLTextureTarget;
    }

    void bind()
    {
        context.bindTextureToUnit(0, this);
    }

    int getTextureId()
    {
        return this.textureId;
    }

    void bindToTextureUnit(int textureUnitIndex)
    {
        if (textureUnitIndex < 0)
        {
            throw new IllegalArgumentException("Texture unit index cannot be negative.");
        }
        else if (textureUnitIndex > this.context.getState().getMaxCombinedTextureImageUnits())
        {
            throw new IllegalArgumentException("Texture unit index (" + textureUnitIndex + ") is greater than the maximum allowed index (" +
                    (this.context.getState().getMaxCombinedTextureImageUnits()-1) + ").");
        }

        context.bindTextureToUnit(textureUnitIndex, this);

        if(this.staleMipmaps)
        {
            // Create mipmaps
            glGenerateMipmap(this.openGLTextureTarget);
            OpenGLContext.errorCheck();

            this.staleMipmaps = false;
        }
    }

    void initFilteringAndMipmaps(boolean useLinearFiltering, int maxMipmapLevel)
    {
        initFilteringAndMipmaps(useLinearFiltering, maxMipmapLevel, true);
    }

    void initFilteringAndMipmaps(boolean useLinearFiltering, int maxMipmapLevel, boolean generateMipmaps)
    {
        if (parameters.useMipmaps)
        {
            if (maxMipmapLevel < Integer.MAX_VALUE)
            {
                glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MAX_LEVEL, maxMipmapLevel);
                OpenGLContext.errorCheck();
            }

            if (generateMipmaps)
            {
                // Create mipmaps
                glGenerateMipmap(this.getOpenGLTextureTarget());
                OpenGLContext.errorCheck();
            }

            if (useLinearFiltering)
            {
                glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                OpenGLContext.errorCheck();
                glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                OpenGLContext.errorCheck();
            }
            else
            {
                glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
                OpenGLContext.errorCheck();
                glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                OpenGLContext.errorCheck();
            }
        }
        else
        {
            if (useLinearFiltering)
            {
                glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                OpenGLContext.errorCheck();
                glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                OpenGLContext.errorCheck();
            }
            else
            {
                glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                OpenGLContext.errorCheck();
                glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                OpenGLContext.errorCheck();
            }
        }
    }

    static int getUnpackAlignment(int format, int dataType)
    {
        if (dataType == GL_UNSIGNED_SHORT_5_6_5 || dataType == GL_UNSIGNED_SHORT_5_6_5_REV || dataType == GL_UNSIGNED_SHORT_4_4_4_4 ||
                dataType == GL_UNSIGNED_SHORT_4_4_4_4_REV || dataType == GL_UNSIGNED_SHORT_5_5_5_1 || dataType == GL_UNSIGNED_SHORT_1_5_5_5_REV)
        {
            return 2;
        }
        else if (format == GL_RGBA || format == GL_BGRA || format == GL_RGBA_INTEGER || format == GL_BGRA_INTEGER || dataType == GL_UNSIGNED_INT || dataType == GL_INT || dataType == GL_FLOAT ||
                dataType == GL_UNSIGNED_INT_8_8_8_8 || dataType == GL_UNSIGNED_INT_8_8_8_8_REV || dataType == GL_UNSIGNED_INT_10_10_10_2 || dataType == GL_UNSIGNED_INT_2_10_10_10_REV)
        {
            return 4;
        }
        else if (dataType == GL_UNSIGNED_SHORT || dataType == GL_SHORT)
        {
            return 2;
        }
        else
        {
            return 1;
        }
    }

    static int getSpecialInternalFormat(TextureType textureType, int precision)
    {
        switch(textureType)
        {
        case DEPTH:
            return OpenGLContext.getOpenGLInternalDepthFormat(precision);
        case STENCIL:
            return OpenGLContext.getOpenGLInternalStencilFormat(precision);
        case DEPTH_STENCIL:
            return GL_DEPTH24_STENCIL8;
        case FLOATING_POINT_DEPTH:
            return GL_DEPTH_COMPONENT32F;
        case FLOATING_POINT_DEPTH_STENCIL:
            return GL_DEPTH32F_STENCIL8;
        case COLOR:
        default:
            return OpenGLContext.getOpenGLInternalColorFormat(
                ColorFormat.createCustom(precision, precision, precision, precision, DataType.NORMALIZED_FIXED_POINT));
        }
    }

    static int translateWrapMode(TextureWrapMode wrapMode)
    {
        switch(wrapMode)
        {
        case None: return GL_CLAMP_TO_EDGE;
        case MirrorOnce: return GL_MIRROR_CLAMP_TO_EDGE;
        case Repeat: return GL_REPEAT;
        case MirroredRepeat: return GL_MIRRORED_REPEAT;
        default: return 0;
        }
    }

    static int translateSamplerType(SamplerType samplerType)
    {
        switch (samplerType)
        {
            case FLOAT_1D:
            case INTEGER_1D:
            case UNSIGNED_INTEGER_1D:
                return GL_TEXTURE_1D;

            case FLOAT_2D:
            case INTEGER_2D:
            case UNSIGNED_INTEGER_2D:
                return GL_TEXTURE_2D;

            case FLOAT_3D:
            case INTEGER_3D:
            case UNSIGNED_INTEGER_3D:
                return GL_TEXTURE_3D;

            case FLOAT_CUBE_MAP:
            case INTEGER_CUBE_MAP:
            case UNSIGNED_INTEGER_CUBE_MAP:
                return GL_TEXTURE_CUBE_MAP;

            case FLOAT_1D_ARRAY:
            case INTEGER_1D_ARRAY:
            case UNSIGNED_INTEGER_1D_ARRAY:
                return GL_TEXTURE_1D_ARRAY;

            case FLOAT_2D_ARRAY:
            case INTEGER_2D_ARRAY:
            case UNSIGNED_INTEGER_2D_ARRAY:
                return GL_TEXTURE_2D_ARRAY;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static int[] getRGBAScanline(BufferedImage colorImg, BufferedImage maskImg, int y)
    {
        int[] scanline = colorImg.getRGB(0, y, colorImg.getWidth(), 1, null, 0, colorImg.getWidth());
        int[] mask = maskImg.getRGB(0, y, maskImg.getWidth(), 1, null, 0, maskImg.getWidth());

        for (int x = 0; x < scanline.length; x++)
        {
            scanline[x] = (scanline[x] & 0x00ffffff) | ((mask[x] & 0x0000ff00) << 16);
        }

        return scanline;
    }

    private static Stream<Map.Entry<Integer, int[]>> getScanlineStream(BufferedImage colorImg, BufferedImage maskImg, boolean flipVertical)
    {
        // Use parallel stream to accelerate image decoding / copy,
        IntStream scanlineRange = IntStream.range(0, colorImg.getHeight()).parallel();

        Stream<Map.Entry<Integer, int[]>> scanlineStream;

        if (maskImg == null)
        {
            if (flipVertical)
            {
                scanlineStream = scanlineRange.mapToObj(y -> new AbstractMap.SimpleEntry<>(
                    colorImg.getHeight() - 1 - y,
                    colorImg.getRGB(0, y, colorImg.getWidth(), 1, null, 0, colorImg.getWidth())));
            }
            else
            {
                scanlineStream = scanlineRange.mapToObj(y -> new AbstractMap.SimpleEntry<>(
                    y,
                    colorImg.getRGB(0, y, colorImg.getWidth(), 1, null, 0, colorImg.getWidth())));
            }
        }
        else
        {
            if (flipVertical)
            {
                scanlineStream = scanlineRange.mapToObj(y -> new AbstractMap.SimpleEntry<>(
                    colorImg.getHeight() - 1 - y,
                    getRGBAScanline(colorImg, maskImg, y)));
            }
            else
            {
                scanlineStream = scanlineRange.mapToObj(y -> new AbstractMap.SimpleEntry<>(
                    y,
                    getRGBAScanline(colorImg, maskImg, y)));
            }
        }

        return scanlineStream;
    }

    static ByteBuffer bufferedImageToNativeBuffer(BufferedImage colorImg, BufferedImage maskImg, boolean flipVertical)
    {
        ByteBuffer buffer = BufferUtils.createByteBuffer(colorImg.getWidth() * colorImg.getHeight() * 4);
        IntBuffer intBuffer = buffer.asIntBuffer();

        getScanlineStream(colorImg, maskImg, flipVertical).forEach(scanline ->
        {
            int y = scanline.getKey();
            int[] scanlineData = scanline.getValue();

            for (int x = 0; x < colorImg.getWidth(); x++)
            {
                intBuffer.put(y * colorImg.getWidth() + x, scanlineData[x]);
            }
        });

        return buffer;
    }

    static ByteBuffer bufferedImageToNativeBuffer(BufferedImage colorImg, BufferedImage maskImg, boolean flipVertical,
        Function<ByteBuffer, BiConsumer<Integer, Color>> bufferWrapperFunction, int mappedColorLength)
    {
        ByteBuffer buffer = BufferUtils.createByteBuffer(colorImg.getWidth() * colorImg.getHeight() * mappedColorLength);
        BiConsumer<Integer, Color> wrappedBuffer = bufferWrapperFunction.apply(buffer);

        getScanlineStream(colorImg, maskImg, flipVertical).forEach(scanline ->
        {
            int y = scanline.getKey();
            int[] scanlineData = scanline.getValue();

            for (int x = 0; x < colorImg.getWidth(); x++)
            {
                wrappedBuffer.accept(
                    y * colorImg.getWidth() + x, new Color(scanlineData[x], true));
            }
        });

        return buffer;
    }

    static ByteBuffer hdrImageToNativeBuffer(Image colorImg, BufferedImage maskImg)
    {
        ByteBuffer buffer = BufferUtils.createByteBuffer(colorImg.width * colorImg.height * (maskImg == null ? 12 : 16));
        FloatBuffer floatBuffer = buffer.asFloatBuffer();

        // Use parallel stream to accelerate image copy
        IntStream scanlineRange = IntStream.range(0, colorImg.height);

        if (maskImg == null)
        {
            scanlineRange.forEach(y ->
            {
                int k = y * colorImg.width * 3;
                for (int x = 0; x < colorImg.width; x++)
                {
                    floatBuffer.put(colorImg.data[k]);
                    k++;
                    floatBuffer.put(colorImg.data[k]);
                    k++;
                    floatBuffer.put(colorImg.data[k]);
                    k++;
                }
            });
        }
        else
        {
            scanlineRange.forEach(y ->
            {
                int k = y * colorImg.width * 4;
                for (int x = 0; x < colorImg.width; x++)
                {
                    floatBuffer.put(colorImg.data[k]);
                    k++;
                    floatBuffer.put(colorImg.data[k]);
                    k++;
                    floatBuffer.put(colorImg.data[k]);
                    k++;

                    // Use green channel of the mask image for alpha
                    floatBuffer.put((float)((maskImg.getRGB(x, y) & 0x0000ff00) >>> 8) / 255.0f);
                }
            });
        }

        return buffer;
    }

    @Override
    public void close()
    {
        if (!closed)
        {
            glDeleteTextures(this.textureId);
            OpenGLContext.errorCheck();
            closed = true;
        }
    }

    @Override
    public void attachToDrawFramebuffer(int attachment, int level)
    {
        if (level < 0)
        {
            throw new IllegalArgumentException("Texture level cannot be negative.");
        }
        if (level > this.getMipmapLevelCount())
        {
            throw new IllegalArgumentException("Illegal level index: " + level + ".  The texture only has " + this.getMipmapLevelCount() + " levels.");
        }
        glFramebufferTexture(GL_DRAW_FRAMEBUFFER, attachment, this.textureId, level);
        OpenGLContext.errorCheck();
    }

    @Override
    public void attachToReadFramebuffer(int attachment, int level)
    {
        glFramebufferTexture(GL_READ_FRAMEBUFFER, attachment, this.textureId, level);
        OpenGLContext.errorCheck();
    }
}
