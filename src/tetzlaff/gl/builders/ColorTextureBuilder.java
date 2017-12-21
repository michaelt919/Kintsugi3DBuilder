package tetzlaff.gl.builders;

import tetzlaff.gl.core.ColorFormat;
import tetzlaff.gl.core.CompressionFormat;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Texture;

public interface ColorTextureBuilder<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> extends TextureBuilder<ContextType, TextureType>
{
    ColorTextureBuilder<ContextType, TextureType> setInternalFormat(ColorFormat format);
    ColorTextureBuilder<ContextType, TextureType> setInternalFormat(CompressionFormat format);

    @Override
    ColorTextureBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations);
    @Override
    ColorTextureBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled);
    @Override
    ColorTextureBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled);
    @Override
    ColorTextureBuilder<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy);
}
