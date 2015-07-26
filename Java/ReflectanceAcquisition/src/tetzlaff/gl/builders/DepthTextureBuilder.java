package tetzlaff.gl.builders;

import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

public interface DepthTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
	DepthTextureBuilder<ContextType, TextureType> setInternalPrecision(int precision);
	DepthTextureBuilder<ContextType, TextureType> setFloatingPointEnabled(boolean enabled);
}
