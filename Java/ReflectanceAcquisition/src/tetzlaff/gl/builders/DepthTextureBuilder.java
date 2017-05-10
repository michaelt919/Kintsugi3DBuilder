package tetzlaff.gl.builders;

import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

public interface DepthTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
	DepthTextureBuilder<ContextType, TextureType> setInternalPrecision(int precision);
	DepthTextureBuilder<ContextType, TextureType> setFloatingPointEnabled(boolean enabled);

	DepthTextureBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations);
	DepthTextureBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled);
	DepthTextureBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled);
	DepthTextureBuilder<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy);
}
