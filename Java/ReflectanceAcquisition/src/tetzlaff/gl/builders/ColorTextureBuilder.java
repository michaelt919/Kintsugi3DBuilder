package tetzlaff.gl.builders;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

/**
 * Implements the builder design pattern for creating color textures.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture will be associated with.
 * @param <TextureType> The type of texture to be created.
 */
public interface ColorTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
	/**
	 * Sets the internal format to an uncompressed format.
	 * @param format The uncompressed color format to use.
	 * @return The calling builder object.
	 */
	ColorTextureBuilder<ContextType, TextureType> setInternalFormat(ColorFormat format);
	
	/**
	 * Sets the internal format to an compressed format.
	 * @param format The compression format to use.
	 * @return The calling builder object.
	 */
	ColorTextureBuilder<ContextType, TextureType> setInternalFormat(CompressionFormat format);
}
