package tetzlaff.gl.opengl;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.imageio.ImageIO;

import tetzlaff.gl.builders.base.ColorTextureBuilderBase;
import tetzlaff.gl.builders.base.DepthStencilTextureBuilderBase;
import tetzlaff.gl.builders.base.DepthTextureBuilderBase;
import tetzlaff.gl.builders.base.StencilTextureBuilderBase;
import tetzlaff.gl.core.*;
import tetzlaff.gl.core.ColorFormat.DataType;
import tetzlaff.gl.types.AbstractDataType;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL44.*;

final class OpenGLTexture3D extends OpenGLTexture implements Texture3D<OpenGLContext>
{
    private int openGLTextureTarget;
    private int mipmapLevelCount;
    private final int width;
    private final int height;
    private int depth;
    private boolean useMipmaps;
    private boolean staleMipmaps;

    static class ColorBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture3D>
    {
        private final int textureTarget;
        private final int width;
        private final int height;
        private final int depth;

        ColorBuilder(OpenGLContext context, int textureTarget, int width, int height, int depth)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        @Override
        public OpenGLTexture3D createTexture()
        {
            if (this.isInternalFormatCompressed())
            {
                return new OpenGLTexture3D(
                        this.context,
                        this.textureTarget,
                        this.getMultisamples(),
                        this.getInternalCompressionFormat(),
                        this.width,
                        this.height,
                        this.depth,
                        GL_RGBA,
                        this.areMultisampleLocationsFixed(),
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
                        this.getMaxMipmapLevel(),
                        this.getMaxAnisotropy());
            }
            else
            {
                return new OpenGLTexture3D(
                        this.context,
                        this.textureTarget,
                        this.getMultisamples(),
                        this.getInternalColorFormat(),
                        this.width,
                        this.height,
                        this.depth,
                        (this.getInternalColorFormat().dataType == DataType.SIGNED_INTEGER ||
                            this.getInternalColorFormat().dataType == DataType.UNSIGNED_INTEGER) ? GL_RGBA_INTEGER : GL_RGBA,
                        this.areMultisampleLocationsFixed(),
                        this.isLinearFilteringEnabled(),
                        this.areMipmapsEnabled(),
                        this.getMaxMipmapLevel(),
                        this.getMaxAnisotropy());
            }
        }
    }

    static class DepthBuilder extends DepthTextureBuilderBase<OpenGLContext, OpenGLTexture3D>
    {
        private final int textureTarget;
        private final int width;
        private final int height;
        private final int depth;

        DepthBuilder(OpenGLContext context, int textureTarget, int width, int height, int depth)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        @Override
        public OpenGLTexture3D createTexture()
        {
            return new OpenGLTexture3D(
                    this.context,
                    this.textureTarget,
                    this.getMultisamples(),
                    TextureType.DEPTH,
                    this.getInternalPrecision(),
                    this.width,
                    this.height,
                    this.depth,
                    GL_DEPTH_COMPONENT,
                    this.areMultisampleLocationsFixed(),
                    this.isLinearFilteringEnabled(),
                    this.areMipmapsEnabled(),
                    this.getMaxMipmapLevel(),
                    this.getMaxAnisotropy());
        }
    }

    static class StencilBuilder extends StencilTextureBuilderBase<OpenGLContext, OpenGLTexture3D>
    {
        private final int textureTarget;
        private final int width;
        private final int height;
        private final int depth;

        StencilBuilder(OpenGLContext context, int textureTarget, int width, int height, int depth)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        @Override
        public OpenGLTexture3D createTexture()
        {
            return new OpenGLTexture3D(
                    this.context,
                    this.textureTarget,
                    this.getMultisamples(),
                    TextureType.STENCIL,
                    this.getInternalPrecision(),
                    this.width,
                    this.height,
                    this.depth,
                    GL_STENCIL_INDEX,
                    this.areMultisampleLocationsFixed(),
                    this.isLinearFilteringEnabled(),
                    this.areMipmapsEnabled(),
                    this.getMaxMipmapLevel(),
                    this.getMaxAnisotropy());
        }
    }

    static class DepthStencilBuilder extends DepthStencilTextureBuilderBase<OpenGLContext, OpenGLTexture3D>
    {
        private final int textureTarget;
        private final int width;
        private final int height;
        private final int depth;

        DepthStencilBuilder(OpenGLContext context, int textureTarget, int width, int height, int depth)
        {
            super(context);
            this.textureTarget = textureTarget;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        @Override
        public OpenGLTexture3D createTexture()
        {
            return new OpenGLTexture3D(
                    this.context,
                    this.textureTarget,
                    this.getMultisamples(),
                    this.isFloatingPointEnabled() ? TextureType.FLOATING_POINT_DEPTH_STENCIL : TextureType.DEPTH_STENCIL,
                    this.isFloatingPointEnabled() ? 40 : 32,
                    this.width,
                    this.height,
                    this.depth,
                    GL_DEPTH_STENCIL,
                    this.areMultisampleLocationsFixed(),
                    this.isLinearFilteringEnabled(),
                    this.areMipmapsEnabled(),
                    this.getMaxMipmapLevel(),
                    this.getMaxAnisotropy());
        }
    }

    private OpenGLTexture3D(OpenGLContext context, int openGLTextureTarget, int multisamples, ColorFormat colorFormat, int width, int height, int layerCount, int format,
            boolean fixedSampleLocations, boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
    {
        // Create and allocate a 3D texture or 2D texture array
        super(context, colorFormat);

        this.width = width;
        this.height = height;

        init(openGLTextureTarget, multisamples, OpenGLContext.getOpenGLInternalColorFormat(colorFormat), width, height, layerCount, format,
                fixedSampleLocations, useLinearFiltering, useMipmaps, maxMipmapLevel, maxAnisotropy);
    }

    private OpenGLTexture3D(OpenGLContext context, int openGLTextureTarget, int multisamples, CompressionFormat compressionFormat, int width, int height, int layerCount, int format,
            boolean fixedSampleLocations, boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
    {
        // Create and allocate a 3D texture or 2D texture array
        super(context, compressionFormat);

        this.width = width;
        this.height = height;

        init(openGLTextureTarget, multisamples, OpenGLContext.getOpenGLCompressionFormat(compressionFormat), width, height, layerCount, format,
                fixedSampleLocations, useLinearFiltering, useMipmaps, maxMipmapLevel, maxAnisotropy);
    }

    private OpenGLTexture3D(OpenGLContext context, int openGLTextureTarget, int multisamples, TextureType textureType, int precision, int width, int height, int layerCount, int format,
            boolean fixedSampleLocations, boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
    {
        // Create and allocate a 3D texture or 2D texture array
        super(context, textureType);

        this.width = width;
        this.height = height;

        int internalFormat;
        switch(textureType)
        {
        case DEPTH:
            internalFormat = OpenGLContext.getOpenGLInternalDepthFormat(precision);
            break;
        case STENCIL:
            internalFormat = OpenGLContext.getOpenGLInternalStencilFormat(precision);
            break;
        case DEPTH_STENCIL:
            internalFormat = GL_DEPTH24_STENCIL8;
            break;
        case FLOATING_POINT_DEPTH_STENCIL:
            internalFormat = GL_DEPTH32F_STENCIL8;
            break;
        case COLOR:
        default:
            internalFormat = OpenGLContext.getOpenGLInternalColorFormat(
                ColorFormat.createCustom(precision, precision, precision, precision, DataType.NORMALIZED_FIXED_POINT));
            break;
        }

        init(openGLTextureTarget, multisamples, internalFormat, width, height, layerCount, format,
                fixedSampleLocations, useLinearFiltering, useMipmaps, maxMipmapLevel, maxAnisotropy);
    }

    private void init(int textureTarget, int multisamples, int internalFormat, int width, int height, int layerCount, int format,
            boolean fixedSampleLocations, boolean useLinearFiltering, boolean useMipmaps, int maxMipmapLevel, float maxAnisotropy)
    {
        this.openGLTextureTarget = textureTarget;
        this.depth = layerCount;
        this.useMipmaps = useMipmaps;

        this.bind();

        if (textureTarget == GL_TEXTURE_2D && multisamples > 1)
        {
            glTexImage3DMultisample(this.openGLTextureTarget, multisamples, internalFormat, width, height, layerCount, fixedSampleLocations);
            OpenGLContext.errorCheck();
        }
        else
        {
            // Last four parameters are meaningless, but are subject to certain validation conditions
            glTexImage3D(this.openGLTextureTarget, 0, internalFormat, width, height, layerCount, 0, format, GL_UNSIGNED_BYTE, 0);
            OpenGLContext.errorCheck();
        }

        if (useMipmaps)
        {
            // Calculate the number of mipmap levels
            this.mipmapLevelCount = 0;
            int dim = Math.max(this.width, this.height);
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

        this.initFilteringAndMipmaps(useLinearFiltering, useMipmaps, maxMipmapLevel, false);

        glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        OpenGLContext.errorCheck();
        
        glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        OpenGLContext.errorCheck();
        
        glTexParameteri(textureTarget, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        OpenGLContext.errorCheck();

        if (maxAnisotropy > 1.0f)
        {
            glTexParameterf(textureTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
            OpenGLContext.errorCheck();
        }
    }

    private BufferedImage validateAndScaleImage(int layerIndex, BufferedImage img) throws IOException
    {
        if(img == null)
        {
            throw new IOException("Error: Unsupported image format.");
        }

        if (layerIndex < 0 || layerIndex >= this.depth)
        {
            throw new IllegalArgumentException("The layer index specified (" + layerIndex + ") is out of bounds (layer count: " + this.depth + ").");
        }

        if (this.width == img.getWidth() && this.height == img.getHeight())
        {
            return img;
        }
        else
        {
            BufferedImage resized = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
            Graphics resizedGraphics = resized.createGraphics();
            resizedGraphics.drawImage(img.getScaledInstance(this.width, this.height, Image.SCALE_SMOOTH), 0, 0 , null);
            resizedGraphics.dispose();
            return resized;
        }
    }

    @Override
    public void loadLayer(int layerIndex, InputStream fileStream, boolean flipVertical) throws IOException
    {
        this.bind();

        ByteBuffer buffer = OpenGLTexture.bufferedImageToNativeBuffer(validateAndScaleImage(layerIndex, ImageIO.read(fileStream)),
            null, flipVertical);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        OpenGLContext.errorCheck();

        glTexSubImage3D(this.openGLTextureTarget, 0, 0, 0, layerIndex, this.width, this.height, 1,
            GL_BGRA, GL_UNSIGNED_BYTE, buffer);
        OpenGLContext.errorCheck();

        if (this.useMipmaps)
        {
            this.staleMipmaps = true;
        }
    }

    @Override
    public void loadLayer(int layerIndex, File file, boolean flipVertical) throws IOException
    {
        this.loadLayer(layerIndex, new FileInputStream(file), flipVertical);
    }

    @Override
    public <MappedType> void loadLayer(int layerIndex, InputStream fileStream, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
    {
        this.bind();

        int format = OpenGLContext.getPixelDataFormatFromDimensions(
            mappedType.getComponentCount(),
            !this.isInternalFormatCompressed() &&
                (this.getInternalUncompressedColorFormat().dataType == DataType.SIGNED_INTEGER
                    || this.getInternalUncompressedColorFormat().dataType == DataType.UNSIGNED_INTEGER));
        int type = OpenGLContext.getDataTypeConstant(mappedType);

        Function<ByteBuffer, Consumer<? super MappedType>> bufferWrapperFunctionPartial = mappedType::wrapByteBuffer;
        ByteBuffer buffer = OpenGLTexture.bufferedImageToNativeBuffer(
            validateAndScaleImage(layerIndex, ImageIO.read(fileStream)), null, flipVertical,
            byteBuffer ->
            {
                Consumer<? super MappedType> partiallyWrappedBuffer = bufferWrapperFunctionPartial.apply(byteBuffer);
                return color -> partiallyWrappedBuffer.accept(mappingFunction.apply(color));
            },
            mappedType.getSizeInBytes());

        glPixelStorei(GL_UNPACK_ALIGNMENT, OpenGLTexture.getUnpackAlignment(format, type));
        OpenGLContext.errorCheck();

        glTexSubImage3D(this.openGLTextureTarget, 0, 0, 0, layerIndex, this.width, this.height, 1,
            format, type, buffer);
        OpenGLContext.errorCheck();

        if (this.useMipmaps)
        {
            this.staleMipmaps = true;
        }
    }

    @Override
    public <MappedType> void loadLayer(int layerIndex, File file, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
    {
        this.loadLayer(layerIndex, new FileInputStream(file), flipVertical, mappedType, mappingFunction);
    }

    @Override
    public void loadLayer(int layerIndex, InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
    {
        this.bind();

        ByteBuffer buffer = bufferedImageToNativeBuffer(
            validateAndScaleImage(layerIndex, ImageIO.read(imageStream)),
            validateAndScaleImage(layerIndex, ImageIO.read(maskStream)),
            flipVertical);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        OpenGLContext.errorCheck();

        glTexSubImage3D(this.openGLTextureTarget, 0, 0, 0, layerIndex, this.width, this.height, 1,
            GL_BGRA, GL_UNSIGNED_BYTE, buffer);
        OpenGLContext.errorCheck();

        if (this.useMipmaps)
        {
            this.staleMipmaps = true;
        }
    }

    @Override
    public void loadLayer(int layerIndex, File imageFile, File maskFile, boolean flipVertical) throws IOException
    {
        this.loadLayer(layerIndex, new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical);
    }

    @Override
    public <MappedType> void loadLayer(int layerIndex, InputStream imageStream, InputStream maskStream, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
    {
        this.bind();

        int format = OpenGLContext.getPixelDataFormatFromDimensions(
            mappedType.getComponentCount(),
            !this.isInternalFormatCompressed() &&
                (this.getInternalUncompressedColorFormat().dataType == DataType.SIGNED_INTEGER
                    || this.getInternalUncompressedColorFormat().dataType == DataType.UNSIGNED_INTEGER));
        int type = OpenGLContext.getDataTypeConstant(mappedType);

        Function<ByteBuffer, Consumer<? super MappedType>> bufferWrapperFunctionPartial = mappedType::wrapByteBuffer;
        ByteBuffer buffer = OpenGLTexture.bufferedImageToNativeBuffer(
            validateAndScaleImage(layerIndex, ImageIO.read(imageStream)),
            validateAndScaleImage(layerIndex, ImageIO.read(maskStream)),
            flipVertical,
            byteBuffer ->
            {
                Consumer<? super MappedType> partiallyWrappedBuffer = bufferWrapperFunctionPartial.apply(byteBuffer);
                return color -> partiallyWrappedBuffer.accept(mappingFunction.apply(color));
            },
            mappedType.getSizeInBytes());

        glPixelStorei(GL_UNPACK_ALIGNMENT, OpenGLTexture.getUnpackAlignment(format, type));
        OpenGLContext.errorCheck();

        glTexSubImage3D(this.openGLTextureTarget, 0, 0, 0, layerIndex, this.width, this.height, 1,
            format, type, buffer);
        OpenGLContext.errorCheck();

        if (this.useMipmaps)
        {
            this.staleMipmaps = true;
        }
    }

    @Override
    public <MappedType> void loadLayer(int layerIndex, File imageFile, File maskFile, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
    {
        this.loadLayer(layerIndex, new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical, mappedType, mappingFunction);
    }

    @Override
    public void generateMipmaps()
    {
        // Create mipmaps
        glGenerateMipmap(this.openGLTextureTarget);
        OpenGLContext.errorCheck();
        
        this.staleMipmaps = false;
    }

    @Override
    void bindToTextureUnit(int textureUnitIndex)
    {
        super.bindToTextureUnit(textureUnitIndex);

        // TODO use GL_GENERATE_MIPMAP texture parameter instead
        if(this.staleMipmaps)
        {
            // Create mipmaps
            glGenerateMipmap(this.openGLTextureTarget);
            OpenGLContext.errorCheck();

            this.staleMipmaps = false;
        }
    }

    @Override
    protected int getOpenGLTextureTarget()
    {
        return this.openGLTextureTarget;
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
    public int getDepth()
    {
        return this.depth;
    }

    @Override
    public int getMipmapLevelCount()
    {
        return this.mipmapLevelCount;
    }

    @Override
    public OpenGLFramebufferAttachment getLayerAsFramebufferAttachment(int layerIndex)
    {
        int textureId = this.getTextureId();
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

    @Override
    public void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT, TextureWrapMode wrapR)
    {
        this.bind();
        switch(wrapS)
        {
        case None:
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            OpenGLContext.errorCheck();
            break;
        case MirrorOnce:
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_S, GL_MIRROR_CLAMP_TO_EDGE);
            OpenGLContext.errorCheck();
            break;
        case Repeat:
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_S, GL_REPEAT);
            OpenGLContext.errorCheck();
            break;
        case MirroredRepeat:
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_S, GL_MIRRORED_REPEAT);
            OpenGLContext.errorCheck();
            break;
        }
        switch(wrapT)
        {
        case None:
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            OpenGLContext.errorCheck();
            break;
        case MirrorOnce:
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_T, GL_MIRROR_CLAMP_TO_EDGE);
            OpenGLContext.errorCheck();
            break;
        case Repeat:
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_T, GL_REPEAT);
            OpenGLContext.errorCheck();
            break;
        case MirroredRepeat:
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_T, GL_MIRRORED_REPEAT);
            OpenGLContext.errorCheck();
            break;
        }
        switch(wrapR)
        {
        case None:
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
            OpenGLContext.errorCheck();
            break;
        case MirrorOnce:
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_R, GL_MIRROR_CLAMP_TO_EDGE);
            OpenGLContext.errorCheck();
            break;
        case Repeat:
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_R, GL_REPEAT);
            OpenGLContext.errorCheck();
            break;
        case MirroredRepeat:
            glTexParameteri(openGLTextureTarget, GL_TEXTURE_WRAP_R, GL_MIRRORED_REPEAT);
            OpenGLContext.errorCheck();
            break;
        }
    }
}
