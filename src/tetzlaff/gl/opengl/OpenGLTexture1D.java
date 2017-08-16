package tetzlaff.gl.opengl;

import java.nio.ByteBuffer;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Texture1D;
import tetzlaff.gl.TextureWrapMode;
import tetzlaff.gl.builders.base.ColorTextureBuilderBase;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL44.*;

// mipmaps

class OpenGLTexture1D extends OpenGLTexture implements Texture1D<OpenGLContext>
{
    private int textureTarget;
    private int width;
    private int levelCount;

    static class OpenGLTexture1DFromBufferBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture1D>
    {
        private int textureTarget;
        private int width;
        private int format;
        private int type;
        private ByteBuffer buffer;

        OpenGLTexture1DFromBufferBuilder(OpenGLContext context, int textureTarget, int width, int format, int type, ByteBuffer buffer)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.width = width;
            this.format = format;
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
                        this.format,
                        this.type,
                        this.buffer,
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
                        this.getMaxAnisotropy());
            }
            else
            {
                return new OpenGLTexture1D(
                        this.context,
                        this.textureTarget,
                        this.getInternalColorFormat(),
                        this.width,
                        this.format,
                        this.type,
                        this.buffer,
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
                        this.getMaxAnisotropy());
            }
        }
    }

    private OpenGLTexture1D(OpenGLContext context, int textureTarget, ColorFormat colorFormat, int width, int format, int type, ByteBuffer buffer,
            boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, colorFormat);

        init(context, textureTarget, context.getOpenGLInternalColorFormat(colorFormat), width, format, type, buffer, useLinearFiltering, useMipmaps, maxAnisotropy);
    }

    private OpenGLTexture1D(OpenGLContext context, int textureTarget, CompressionFormat compressionFormat, int width, int format, int type, ByteBuffer buffer,
            boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy)
    {
        // Create an empty texture to be used as a render target for a framebuffer.
        super(context, compressionFormat);

        init(context, textureTarget, context.getOpenGLCompressionFormat(compressionFormat), width, format, type, buffer, useLinearFiltering, useMipmaps, maxAnisotropy);
    }

    private void init(OpenGLContext context, int textureTarget, int internalFormat, int width, int format, int type, ByteBuffer buffer,
            boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy)
    {
        this.textureTarget = textureTarget;
        this.bind();
        this.width = width;

        if (type == GL_UNSIGNED_SHORT_5_6_5 || type == GL_UNSIGNED_SHORT_5_6_5_REV || type == GL_UNSIGNED_SHORT_4_4_4_4 ||
                 type == GL_UNSIGNED_SHORT_4_4_4_4_REV || type == GL_UNSIGNED_SHORT_5_5_5_1 || type == GL_UNSIGNED_SHORT_1_5_5_5_REV)
        {
            glPixelStorei(GL_UNPACK_ALIGNMENT, 2);
            this.context.openGLErrorCheck();
        }
        else if (format == GL_RGBA || format == GL_BGRA || format == GL_RGBA_INTEGER || format == GL_RGBA_INTEGER || type == GL_UNSIGNED_INT || type == GL_INT || type == GL_FLOAT ||
                type == GL_UNSIGNED_INT_8_8_8_8 || type == GL_UNSIGNED_INT_8_8_8_8_REV || type == GL_UNSIGNED_INT_10_10_10_2 || type == GL_UNSIGNED_INT_2_10_10_10_REV)
        {
            glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
            this.context.openGLErrorCheck();
        }
        else if (type == GL_UNSIGNED_SHORT || type == GL_SHORT)
        {
            glPixelStorei(GL_UNPACK_ALIGNMENT, 2);
            this.context.openGLErrorCheck();
        }
        else
        {
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            this.context.openGLErrorCheck();
        }

        glTexImage1D(textureTarget, 0, internalFormat, width, 0, format, type, buffer);
        this.context.openGLErrorCheck();

        if (useMipmaps)
        {
            // Create mipmaps
            glGenerateMipmap(textureTarget);
            this.context.openGLErrorCheck();

            // Calculate the number of mipmap levels
            this.levelCount = 0;
            int dim = width;
            while (dim > 0)
            {
                this.levelCount++;
                dim /= 2;
            }

            if (useLinearFiltering)
            {
                glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                this.context.openGLErrorCheck();
                glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                this.context.openGLErrorCheck();
            }
            else
            {
                glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
                this.context.openGLErrorCheck();
                glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                this.context.openGLErrorCheck();
            }
        }
        else
        {
            // No mipmaps
            this.levelCount = 1;

            if (useLinearFiltering)
            {
                glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                this.context.openGLErrorCheck();
                glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                this.context.openGLErrorCheck();
            }
            else
            {
                glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                this.context.openGLErrorCheck();
                glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                this.context.openGLErrorCheck();
            }
        }

        glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        this.context.openGLErrorCheck();

        if (maxAnisotropy > 1.0f)
        {
            glTexParameterf(textureTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
            this.context.openGLErrorCheck();
        }
    }

    @Override
    public int getWidth()
    {
        return this.width;
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
    public void setTextureWrap(TextureWrapMode wrap)
    {
        this.bind();
        switch(wrap)
        {
        case None:
            glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            this.context.openGLErrorCheck();
            break;
        case MirrorOnce:
            glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_MIRROR_CLAMP_TO_EDGE);
            this.context.openGLErrorCheck();
            break;
        case Repeat:
            glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_REPEAT);
            this.context.openGLErrorCheck();
            break;
        case MirroredRepeat:
            glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_MIRRORED_REPEAT);
            this.context.openGLErrorCheck();
            break;
        }
    }
}
