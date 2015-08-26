package tetzlaff.gl.builders;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

public interface ColorTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
	ColorTextureBuilder<ContextType, TextureType> setInternalFormat(ColorFormat format);
	ColorTextureBuilder<ContextType, TextureType> setInternalFormat(CompressionFormat format);
}
