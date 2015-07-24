package tetzlaff.gl.builders;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

public interface TextureBuilder<ContextType extends Context<? super ContextType>, TextureType extends Texture<ContextType>>
{
	TextureBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations);
	TextureBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled);
	TextureBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled);
	
	TextureType createTexture();
}
