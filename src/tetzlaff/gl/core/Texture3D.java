package tetzlaff.gl.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import javax.imageio.ImageIO;

import tetzlaff.gl.types.AbstractDataType;

/**
 * An interface for a three-dimensional texture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture is associated with.
 */
public interface Texture3D<ContextType extends Context<ContextType>> extends Texture<ContextType>
{
    int getWidth();
    int getHeight();
    int getDepth();

    void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT, TextureWrapMode wrapR);

    void loadLayer(int layerIndex, BufferedImage image, BufferedImage mask, boolean flipVertical) throws IOException;

    default void loadLayer(int layerIndex, BufferedImage image, boolean flipVertical) throws IOException
    {
        loadLayer(layerIndex, image, null, flipVertical);
    }

    default void loadLayer(int layerIndex, InputStream fileStream, boolean flipVertical) throws IOException
    {
        this.loadLayer(layerIndex, ImageIO.read(fileStream), null, flipVertical);
    }

    default void loadLayer(int layerIndex, File file, boolean flipVertical) throws IOException
    {
        this.loadLayer(layerIndex, new FileInputStream(file), flipVertical);
    }

    default void loadLayer(int layerIndex, InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
    {
        this.loadLayer(layerIndex, ImageIO.read(imageStream), ImageIO.read(maskStream), flipVertical);
    }

    default void loadLayer(int layerIndex, File imageFile, File maskFile, boolean flipVertical) throws IOException
    {
        this.loadLayer(layerIndex, new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical);
    }

    <MappedType> void loadLayer(int layerIndex, InputStream fileStream, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException;
    <MappedType> void loadLayer(int layerIndex, InputStream imageStream, InputStream maskStream, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException;

    default <MappedType> void loadLayer(int layerIndex, File file, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
    {
        this.loadLayer(layerIndex, new FileInputStream(file), flipVertical, mappedType, mappingFunction);
    }

    default <MappedType> void loadLayer(int layerIndex, File imageFile, File maskFile, boolean flipVertical,
        AbstractDataType<? super MappedType> mappedType, Function<Color, MappedType> mappingFunction) throws IOException
    {
        this.loadLayer(layerIndex, new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical, mappedType, mappingFunction);
    }


    void generateMipmaps();

    FramebufferAttachment<ContextType> getLayerAsFramebufferAttachment(int layerIndex);
}
