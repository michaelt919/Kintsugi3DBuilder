package tetzlaff.gl;

import java.awt.image.BufferedImage;
import java.io.*;

import tetzlaff.gl.builders.*;
import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;

/**
 * An interface for any OpenGL-like graphics context.
 * An implementation of this interface serves several purposes:
 * (1) it provides query functions for obtaining information about the GL state, and
 * (2) it serves as an access point for modifying global GL options like depth testing, multisampling, and back-face culling,
 * (3) it provides synchronization functions like flush(), finish(), and swapBuffers(),
 * (4) it acts as a factory for creating GL objects such as buffers, textures, and shaders.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> For concrete types, this type parameter should match the implementing class itself.
 * Abstract implementations should be parameterized so that subclasses can fulfill this constraint.
 * This type parameter ensures that all the objects created by this context are mutually compatible with each other and the context itself.
 */
public interface Context<ContextType extends Context<ContextType>>
{
    void makeContextCurrent();

    void flush();
    void finish();
    void swapBuffers();

    ContextState<ContextType> getState();
    FramebufferSize getFramebufferSize();

    Shader<ContextType> createShader(ShaderType type, String source);
    Shader<ContextType> createShader(ShaderType type, File file) throws FileNotFoundException;
    ProgramBuilder<ContextType> getShaderProgramBuilder();

    Framebuffer<ContextType> getDefaultFramebuffer();
    FramebufferObjectBuilder<ContextType> buildFramebufferObject(int width, int height);

    VertexBuffer<ContextType> createVertexBuffer();

    default VertexBuffer<ContextType> createRectangle()
    {
        return this.createVertexBuffer().setData(NativeVectorBufferFactory.getInstance().createFromFloatArray(2, 4, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f), false);
    }

    IndexBuffer<ContextType> createIndexBuffer();
    UniformBuffer<ContextType> createUniformBuffer();

    Drawable<ContextType> createDrawable(Program<ContextType> program);

    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
        build2DColorTextureFromStreamWithMask(InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;
    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
        build2DColorHDRTextureFromStreamWithMask(BufferedInputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;

    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
        build2DColorTextureFromImageWithMask(BufferedImage colorImage, BufferedImage maskImage, boolean flipVertical);

    default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
        build2DColorTextureFromStream(InputStream imageStream, boolean flipVertical) throws IOException
    {
        return build2DColorTextureFromStreamWithMask(imageStream, null, flipVertical);
    }

    default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
        build2DColorTextureFromFileWithMask(File imageFile, File maskFile, boolean flipVertical) throws IOException
    {
        if (imageFile.getName().endsWith(".hdr"))
        {
            return build2DColorHDRTextureFromStreamWithMask(new BufferedInputStream(new FileInputStream(imageFile)), new FileInputStream(maskFile), flipVertical);
        }
        else
        {
            return build2DColorTextureFromStreamWithMask(new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical);
        }
    }

    default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
        build2DColorHDRTextureFromStream(BufferedInputStream imageStream, boolean flipVertical) throws IOException
    {
        return build2DColorHDRTextureFromStreamWithMask(imageStream, null, flipVertical);
    }

    default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
        build2DColorTextureFromFile(File imageFile, boolean flipVertical) throws IOException
    {
        if (imageFile.getName().endsWith(".hdr"))
        {
            return build2DColorHDRTextureFromStream(new BufferedInputStream(new FileInputStream(imageFile)), flipVertical);
        }
        else
        {
            return build2DColorTextureFromStream(new FileInputStream(imageFile), flipVertical);
        }
    }

    default ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>>
        build2DColorTextureFromImage(BufferedImage colorImage, boolean flipVertical)
    {
        return build2DColorTextureFromImageWithMask(colorImage, null, flipVertical);
    }

    ColorTextureBuilder<ContextType, ? extends Texture1D<ContextType>> build1DColorTexture(NativeVectorBuffer data);

    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DColorTextureFromBuffer(int width, int height, NativeVectorBuffer data);
    ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DColorTexture(int width, int height);
    DepthTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DDepthTexture(int width, int height);
    StencilTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DStencilTexture(int width, int height);
    DepthStencilTextureBuilder<ContextType, ? extends Texture2D<ContextType>> build2DDepthStencilTexture(int width, int height);

    TextureBuilder<ContextType, ? extends Texture2D<ContextType>> buildPerlinNoiseTexture();

    ColorTextureBuilder<ContextType, ? extends Texture3D<ContextType>> build2DColorTextureArray(int width, int height, int length);
    DepthTextureBuilder<ContextType, ? extends Texture3D<ContextType>> build2DDepthTextureArray(int width, int height, int length);
    StencilTextureBuilder<ContextType, ? extends Texture3D<ContextType>> build2DStencilTextureArray(int width, int height, int length);
    DepthStencilTextureBuilder<ContextType, ? extends Texture3D<ContextType>> build2DDepthStencilTextureArray(int width, int height, int length);

    ColorCubemapBuilder<ContextType, ? extends Cubemap<ContextType>> buildColorCubemap(int faceSize);
    DepthTextureBuilder<ContextType, ? extends Cubemap<ContextType>> buildDepthCubemap(int faceSize);
    StencilTextureBuilder<ContextType, ? extends Cubemap<ContextType>> buildStencilCubemap(int faceSize);
    DepthStencilTextureBuilder<ContextType, ? extends Cubemap<ContextType>> buildDepthStencilCubemap(int faceSize);
}
