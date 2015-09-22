package tetzlaff.gl.builders;

import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

/**
 * Implements the builder design pattern for creating stencil textures.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture will be associated with.
 * @param <TextureType> The type of texture to be created.
 */
public interface StencilTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
	/**
	 * The number of bits that should be used to represent the stencil component.
	 * The GL should select a format that has at least this much precision, if possible.
	 * If not, the format with the most available precision should be used.
	 * @param precision The number of bits to use.
	 * @return The calling builder object.
	 */
	StencilTextureBuilder<ContextType, TextureType> setInternalPrecision(int precision);
}
