package tetzlaff.gl.builders;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Texture;

public interface StencilTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
    StencilTextureBuilder<ContextType, TextureType> setInternalPrecision(int precision);

    @Override
    StencilTextureBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations);
    @Override
    StencilTextureBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled);
    @Override
    StencilTextureBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled);
    @Override
    StencilTextureBuilder<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy);
}
