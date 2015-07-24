package tetzlaff.gl.builders;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

public interface StencilTextureBuilder<ContextType extends Context<? super ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
	StencilTextureBuilder<ContextType, TextureType> setInternalPrecision(int precision);
}
