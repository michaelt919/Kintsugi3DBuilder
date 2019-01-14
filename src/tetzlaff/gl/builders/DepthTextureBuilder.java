package tetzlaff.gl.builders;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Texture;

public interface DepthTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
    DepthTextureBuilder<ContextType, TextureType> setInternalPrecision(int precision);
    DepthTextureBuilder<ContextType, TextureType> setFloatingPointEnabled(boolean enabled);

    @Override
    DepthTextureBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations);
    @Override
    DepthTextureBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled);
    @Override
    DepthTextureBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled);
    @Override
    DepthTextureBuilder<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy);
}