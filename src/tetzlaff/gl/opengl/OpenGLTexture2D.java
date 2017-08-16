package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.ColorFormat.DataType;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.TextureType;
import tetzlaff.gl.TextureWrapMode;
import tetzlaff.gl.builders.base.ColorTextureBuilderBase;
import tetzlaff.gl.builders.base.DepthStencilTextureBuilderBase;
import tetzlaff.gl.builders.base.DepthTextureBuilderBase;
import tetzlaff.gl.builders.base.StencilTextureBuilderBase;
import tetzlaff.util.RadianceImageLoader;

// mipmaps

class OpenGLTexture2D extends OpenGLTexture implements Texture2D<OpenGLContext>
{
    private int textureTarget;
    private int width;
    private int height;
    private int multisamples;
    private int levelCount;

    static class OpenGLTexture2DFromFileBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private int textureTarget;
        private BufferedImage colorImg;
        private BufferedImage maskImg = null;
        private boolean flipVertical;

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
                        this.getMaxAnisotropy());
            }
        }
    }

    static class OpenGLTexture2DFromHDRFileBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private int textureTarget;
        private RadianceImageLoader.Image colorImg;
        private BufferedImage maskImg = null;

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
                        this.getMaxAnisotropy());
            }
        }
    }

    static class OpenGLTexture2DFromBufferBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private int textureTarget;
        private int width;
        private int height;
        private int format;
        private int type;
        private ByteBuffer buffer;

        OpenGLTexture2DFromBufferBuilder(OpenGLContext context, int textureTarget, int width, int height, int format, int type, ByteBuffer buffer)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.width = width;
            this.height = height;
            this.format = format;
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
                        this.format,
                        this.type,
                        this.buffer,
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
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
                        this.format,
                        this.type,
                        this.buffer,
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
                        this.getMaxAnisotropy());
            }
        }
    }

    static class OpenGLTexture2DColorBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private int textureTarget;
        private int width;
        private int height;

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
                        this.getMaxAnisotropy());
            }
        }
    }

    static class OpenGLTexture2DDepthBuilder extends DepthTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private int textureTarget;
        private int width;
        private int height;

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
                    this.getMaxAnisotropy());
        }
    }

    static class OpenGLTexture2DStencilBuilder extends StencilTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private int textureTarget;
        private int width;
        private int height;

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
                    this.getMaxAnisotropy());
        }
    }

    static class OpenGLTexture2DDepthStencilBuilder extends DepthStencilTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
    {
        private int textureTarget;
        private int width;
        private int height;

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
                    this.getMaxAnisotropy());
        }
    }

    private OpenGLTexture2D(OpenGLContext context, int textureTarget, ColorFormat colorFormat, int width, int height, int format, int type, ByteBuffer buffer,
            boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, colorFormat);
        init(context, textureTarget, context.getOpenGLInternalColorFormat(colorFormat), width, height, format, type, buffer,
                useLinearFiltering, useMipmaps, maxAnisotropy);
    }

    private OpenGLTexture2D(OpenGLContext context, int textureTarget, CompressionFormat compressionFormat, int width, int height, int format, int type, ByteBuffer buffer,
            boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, compressionFormat);
        init(context, textureTarget, context.getOpenGLCompressionFormat(compressionFormat), width, height, format, type, buffer,
                useLinearFiltering, useMipmaps, maxAnisotropy);
    }

    private OpenGLTexture2D(OpenGLContext context, int textureTarget, int multisamples, ColorFormat colorFormat, int width, int height, int format,
            boolean fixedMultisampleLocations, boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, colorFormat);
        init(context, textureTarget, multisamples, context.getOpenGLInternalColorFormat(colorFormat), width, height, format,
                fixedMultisampleLocations, useLinearFiltering, useMipmaps, maxAnisotropy);
    }

    private OpenGLTexture2D(OpenGLContext context, int textureTarget, int multisamples, CompressionFormat compressionFormat, int width, int height, int format,
            boolean fixedMultisampleLocations, boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, compressionFormat);
        init(context, textureTarget, multisamples, context.getOpenGLCompressionFormat(compressionFormat), width, height, format,
                fixedMultisampleLocations, useLinearFiltering, useMipmaps, maxAnisotropy);
    }

    private OpenGLTexture2D(OpenGLContext context, int textureTarget, int multisamples, TextureType textureType, int precision, int width, int height, int format,
            boolean fixedMultisampleLocations, boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, textureType);
        init(context, textureTarget, multisamples, getSpecialInternalFormat(context, textureType, precision), width, height, format,
                fixedMultisampleLocations, useLinearFiltering, useMipmaps, maxAnisotropy);
    }

    private void init(OpenGLContext context, int textureTarget, int multisamples, int internalFormat, int width, int height, int format,
            boolean fixedMultisampleLocations, boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        this.textureTarget = textureTarget;
        this.bind();
        this.width = width;
        this.height = height;
        if (textureTarget == GL_TEXTURE_2D && multisamples > 1)
        {
            this.textureTarget = GL_TEXTURE_2D_MULTISAMPLE;
            useLinearFiltering = false; // linear filtering not allowed with multisampling
            useMipmaps = false; // mipmaps not allowed with multisampling
            this.levelCount = 1;
            glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, multisamples, internalFormat, width, height, fixedMultisampleLocations);
            this.context.openGLErrorCheck();
            // TODO: multisample textures don't seem to work correctly
        }
        else
        {
            // Last four parameters are essentially meaningless, but are subject to certain validation conditions
            glTexImage2D(textureTarget, 0, internalFormat, width, height, 0, format, GL_UNSIGNED_BYTE, 0);
            this.context.openGLErrorCheck();
            this.initFilteringAndMipmaps(useLinearFiltering, useMipmaps);
        }

        if (maxAnisotropy > 1.0f)
        {
            glTexParameterf(textureTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
            this.context.openGLErrorCheck();
        }
    }

    private void init(OpenGLContext context, int textureTarget, int internalFormat, int width, int height, int format, int type, ByteBuffer buffer,
            boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy)
    {
        this.textureTarget = textureTarget;
        this.bind();
        this.width = width;
        this.height = height;

        glPixelStorei(GL_UNPACK_ALIGNMENT, OpenGLTexture.getUnpackAlignment(format, type));
        this.context.openGLErrorCheck();

        glTexImage2D(textureTarget, 0, internalFormat, width, height, 0, format, type, buffer);
        this.context.openGLErrorCheck();
        this.initFilteringAndMipmaps(useLinearFiltering, useMipmaps);

        if (maxAnisotropy > 1.0f)
        {
            glTexParameterf(textureTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
            this.context.openGLErrorCheck();
        }
    }

    void initFilteringAndMipmaps(boolean useLinearFiltering, boolean useMipmaps)
    {
        super.initFilteringAndMipmaps(useLinearFiltering, useMipmaps);

        if (useMipmaps)
        {
            // Calculate the number of mipmap levels
            this.levelCount = 0;
            int dim = Math.max(width, height);
            while (dim > 1)
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

        glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        this.context.openGLErrorCheck();
        
        glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        this.context.openGLErrorCheck();
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

    public int getMultisamples()
    {
        return this.multisamples;
    }

    @Override
    protected int getOpenGLTextureTarget()
    {
        return this.textureTarget;
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
            glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, numericWrapS);
            this.context.openGLErrorCheck();
        }

        if (numericWrapT != 0)
        {
            glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, numericWrapT);
            this.context.openGLErrorCheck();
        }
    }
}
