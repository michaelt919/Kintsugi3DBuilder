package tetzlaff.gl.builders;

import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

public interface DepthStencilTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
	DepthStencilTextureBuilder<ContextType, TextureType> setFloatingPointDepthEnabled(boolean enabled);
}
