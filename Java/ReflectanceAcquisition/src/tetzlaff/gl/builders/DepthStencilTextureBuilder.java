package tetzlaff.gl.builders;

import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

/**
 * Implements the builder design pattern for creating depth+stencil textures.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture will be associated with.
 * @param <TextureType> The type of texture to be created.
 */
public interface DepthStencilTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
	/**
	 * Sets whether or not the depth component should be represented as a floating-point number.
	 * @param enabled true to use floating-point for the depth component, false to use fixed-point.
	 * @return The calling builder object.
	 */
	DepthStencilTextureBuilder<ContextType, TextureType> setFloatingPointDepthEnabled(boolean enabled);
}
