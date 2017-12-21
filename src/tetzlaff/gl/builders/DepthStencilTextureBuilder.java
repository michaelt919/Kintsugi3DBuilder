package tetzlaff.gl.builders;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Texture;

public interface DepthStencilTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
    DepthStencilTextureBuilder<ContextType, TextureType> setFloatingPointDepthEnabled(boolean enabled);

    @Override
    DepthStencilTextureBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations);
    @Override
    DepthStencilTextureBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled);
    @Override
    DepthStencilTextureBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled);
    @Override
    DepthStencilTextureBuilder<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy);
}
