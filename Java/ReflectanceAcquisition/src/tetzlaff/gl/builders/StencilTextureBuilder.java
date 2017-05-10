package tetzlaff.gl.builders;

import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

public interface StencilTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
	StencilTextureBuilder<ContextType, TextureType> setInternalPrecision(int precision);

	StencilTextureBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations);
	StencilTextureBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled);
	StencilTextureBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled);
	StencilTextureBuilder<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy);
}
