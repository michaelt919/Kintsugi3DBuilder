package tetzlaff.gl.builders;

import java.io.IOException;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Cubemap;
import tetzlaff.gl.CubemapFace;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;

public interface ColorCubemapBuilder <ContextType extends Context<ContextType>, TextureType extends Cubemap<ContextType>> 
extends ColorTextureBuilder<ContextType, TextureType>
{
	ColorCubemapBuilder<ContextType, TextureType> loadFace(CubemapFace face, NativeVectorBuffer data) throws IOException;
	
	ColorCubemapBuilder<ContextType, TextureType> setInternalFormat(ColorFormat format);
	ColorCubemapBuilder<ContextType, TextureType> setInternalFormat(CompressionFormat format);
	
	ColorCubemapBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations);
	ColorCubemapBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled);
	ColorCubemapBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled);
	ColorCubemapBuilder<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy);
}
