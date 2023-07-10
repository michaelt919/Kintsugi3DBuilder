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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import tetzlaff.gl.builders.*;
import tetzlaff.gl.core.*;
import tetzlaff.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import tetzlaff.gl.opengl.OpenGLTexture1D.OpenGLTexture1DFromBufferBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.*;
import tetzlaff.gl.opengl.OpenGLTexture3D.ColorBuilder;
import tetzlaff.gl.opengl.OpenGLTexture3D.DepthBuilder;
import tetzlaff.gl.opengl.OpenGLTexture3D.DepthStencilBuilder;
import tetzlaff.gl.opengl.OpenGLTexture3D.StencilBuilder;
import tetzlaff.gl.types.AbstractDataType;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

class OpenGLTextureFactory implements TextureFactory<OpenGLContext>
{
    private final OpenGLContext context;
    private final Map<SamplerType, OpenGLNullTexture> nullTextures = new EnumMap<>(SamplerType.class);

    OpenGLTextureFactory(OpenGLContext context)
    {
        this.context = context;
    }

    @Override
    public ColorTextureBuilder<OpenGLContext, ? extends Texture1D<OpenGLContext>> build1DColorTexture(ReadonlyNativeVectorBuffer data)
    {
        return new OpenGLTexture1DFromBufferBuilder(context, GL_TEXTURE_1D, data.getCount(), data.getDimensions(),
            OpenGLContext.getDataTypeConstant(data.getDataType()), data.getBuffer());
    }

    @Override
    public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> build2DColorTextureFromBuffer(int width, int height, ReadonlyNativeVectorBuffer data)
    {
        return new OpenGLTexture2DFromBufferBuilder(context, GL_TEXTURE_2D, width, height, data.getDimensions(),
            OpenGLContext.getDataTypeConstant(data.getDataType()), data.getBuffer());
    }

    @Override
    public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> build2DColorTextureFromImageWithMask(
        BufferedImage colorImage, BufferedImage maskImage, boolean flipVertical)
    {
        return new OpenGLTexture2DFromFileBuilder(context, GL_TEXTURE_2D, colorImage, maskImage, flipVertical);
    }

    @Override
    public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> build2DColorTextureFromStreamWithMask(
        InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
    {
        return new OpenGLTexture2DFromFileBuilder(context, GL_TEXTURE_2D, imageStream, maskStream, flipVertical);
    }

    @Override
    public <MappedType> ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> build2DColorTextureFromImageWithMask(
        BufferedImage colorImage, BufferedImage maskImage, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction)
    {
        return new OpenGLTexture2DMappedFromFileBuilder<>(context, GL_TEXTURE_2D, colorImage, maskImage, flipVertical, mappedType, mappingFunction);
    }

    @Override
    public <MappedType> ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> build2DColorTextureFromStreamWithMask(
        InputStream imageStream, InputStream maskStream, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
    {
        return new OpenGLTexture2DMappedFromFileBuilder<>(context, GL_TEXTURE_2D, imageStream, maskStream, flipVertical, mappedType, mappingFunction);
    }

    @Override
    public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> build2DColorHDRTextureFromStreamWithMask(
        BufferedInputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
    {
        return new OpenGLTexture2DFromHDRFileBuilder(context, GL_TEXTURE_2D, imageStream, maskStream, flipVertical);
    }

    @Override
    public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> build2DColorTexture(int width, int height)
    {
        return new OpenGLTexture2DColorBuilder(context, GL_TEXTURE_2D, width, height);
    }

    @Override
    public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> build1DColorTextureArray(int width, int height)
    {
        return new OpenGLTexture2DColorBuilder(context, GL_TEXTURE_1D_ARRAY, width, height);
    }

    @Override
    public DepthTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> build2DDepthTexture(int width, int height)
    {
        return new OpenGLTexture2DDepthBuilder(context, GL_TEXTURE_2D, width, height);
    }

    @Override
    public StencilTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> build2DStencilTexture(int width, int height)
    {
        return new OpenGLTexture2DStencilBuilder(context, GL_TEXTURE_2D, width, height);
    }

    @Override
    public DepthStencilTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> build2DDepthStencilTexture(int width, int height)
    {
        return new OpenGLTexture2DDepthStencilBuilder(context, GL_TEXTURE_2D, width, height);
    }

    // Supporting code:
    // Author: Stefan Gustavson (stegu@itn.liu.se) 2004
    // You may use, modify and redistribute this code free of charge,
    // provided that my name and this notice appears intact.
    // https://github.com/ashima/webgl-noise
    // Modified by Michael Tetzlaff

    private static final int[] PERM = {151,160,137,91,90,15,
        131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
        190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
        88,237,149,56,87,174,20,125,136,171,168, 68,175,74,165,71,134,139,48,27,166,
        77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
        102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,
        135,130,116,188,159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,
        5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
        223,183,170,213,119,248,152, 2,44,154,163, 70,221,153,101,155,167, 43,172,9,
        129,22,39,253, 19,98,108,110,79,113,224,232,178,185, 112,104,218,246,97,228,
        251,34,242,193,238,210,144,12,191,179,162,241, 81,51,145,235,249,14,239,107,
        49,192,214, 31,181,199,106,157,184, 84,204,176,115,121,50,45,127, 4,150,254,
        138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180};

    private static final int[][] GRAD3 = {{0,1,1},{0,1,-1},{0,-1,1},{0,-1,-1},
        {1,0,1},{1,0,-1},{-1,0,1},{-1,0,-1},
        {1,1,0},{1,-1,0},{-1,1,0},{-1,-1,0}, // 12 cube edges
        {1,0,-1},{-1,0,-1},{0,-1,1},{0,1,1}}; // 4 more to make 16

    @Override
    public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> buildPerlinNoiseTexture()
    {
        ByteBuffer pixels = ByteBuffer.allocateDirect(256*256*4);
        for(int i = 0; i<256; i++)
        {
            for(int j = 0; j<256; j++)
            {
                int offset = (i*256+j)*4;
                byte value = (byte) PERM[(j+ PERM[i]) & 0xFF];
                pixels.put(offset, (byte)(GRAD3[value & 0x0F][0] * 64 + 64));   // Gradient x
                pixels.put(offset+1, (byte)(GRAD3[value & 0x0F][1] * 64 + 64)); // Gradient y
                pixels.put(offset+2, (byte)(GRAD3[value & 0x0F][2] * 64 + 64)); // Gradient z
                pixels.put(offset+3, value);                     // Permuted index
            }
        }
        return new OpenGLTexture2DFromBufferBuilder(context, GL_TEXTURE_2D, 256, 256, 4, GL_UNSIGNED_BYTE, pixels);
    }

    // End of supporting code


    @Override
    public ColorTextureBuilder<OpenGLContext, ? extends Texture3D<OpenGLContext>> build2DColorTextureArray(int width, int height, int length)
    {
        return new ColorBuilder(context, GL_TEXTURE_2D_ARRAY, width, height, length);
    }

    @Override
    public DepthTextureBuilder<OpenGLContext, ? extends Texture3D<OpenGLContext>> build2DDepthTextureArray(int width, int height, int length)
    {
        return new DepthBuilder(context, GL_TEXTURE_2D_ARRAY, width, height, length);
    }

    @Override
    public StencilTextureBuilder<OpenGLContext, ? extends Texture3D<OpenGLContext>> build2DStencilTextureArray(int width, int height, int length)
    {
        return new StencilBuilder(context, GL_TEXTURE_2D_ARRAY, width, height, length);
    }

    @Override
    public DepthStencilTextureBuilder<OpenGLContext, ? extends Texture3D<OpenGLContext>> build2DDepthStencilTextureArray(int width, int height, int length)
    {
        return new DepthStencilBuilder(context, GL_TEXTURE_2D_ARRAY, width, height, length);
    }

    @Override
    public ColorCubemapBuilder<OpenGLContext, ? extends Cubemap<OpenGLContext>> buildColorCubemap(int faceSize)
    {
        return new OpenGLCubemap.ColorBuilder(context, GL_TEXTURE_CUBE_MAP, faceSize);
    }

    @Override
    public DepthTextureBuilder<OpenGLContext, ? extends Cubemap<OpenGLContext>> buildDepthCubemap(int faceSize)
    {
        return new OpenGLCubemap.DepthBuilder(context, GL_TEXTURE_CUBE_MAP, faceSize);
    }

    @Override
    public StencilTextureBuilder<OpenGLContext, ? extends Cubemap<OpenGLContext>> buildStencilCubemap(int faceSize)
    {
        return new OpenGLCubemap.StencilBuilder(context, GL_TEXTURE_CUBE_MAP, faceSize);
    }

    @Override
    public DepthStencilTextureBuilder<OpenGLContext, ? extends Cubemap<OpenGLContext>> buildDepthStencilCubemap(int faceSize)
    {
        return new OpenGLCubemap.DepthStencilBuilder(context, GL_TEXTURE_CUBE_MAP, faceSize);
    }

    @Override
    public Texture<OpenGLContext> getNullTexture(SamplerType samplerType)
    {
        if (nullTextures.containsKey(samplerType))
        {
            return nullTextures.get(samplerType);
        }
        else
        {
            OpenGLNullTexture nullTex = new OpenGLNullTexture(context, samplerType);
            nullTextures.put(samplerType, nullTex);
            return nullTex;
        }
    }
}
