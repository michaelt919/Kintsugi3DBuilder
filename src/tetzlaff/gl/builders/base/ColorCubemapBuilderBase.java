package tetzlaff.gl.builders.base;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Cubemap;
import tetzlaff.gl.builders.ColorCubemapBuilder;

public abstract class ColorCubemapBuilderBase <ContextType extends Context<ContextType>, TextureType extends Cubemap<ContextType>> 
    extends ColorTextureBuilderBase<ContextType, TextureType>
    implements ColorCubemapBuilder<ContextType, TextureType>
{
    protected ColorCubemapBuilderBase(ContextType context)
    {
        super(context);
    }

    @Override
    public ColorCubemapBuilderBase<ContextType, TextureType> setInternalFormat(ColorFormat format)
    {
        super.setInternalFormat(format);
        return this;
    }

    @Override
    public ColorCubemapBuilderBase<ContextType, TextureType> setInternalFormat(CompressionFormat format)
    {
        super.setInternalFormat(format);
        return this;
    }

    @Override
    public ColorCubemapBuilderBase<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations)
    {
        super.setMultisamples(samples, fixedSampleLocations);
        return this;
    }

    @Override
    public ColorCubemapBuilderBase<ContextType, TextureType> setMipmapsEnabled(boolean enabled)
    {
        super.setMipmapsEnabled(enabled);
        return this;
    }

    @Override
    public ColorCubemapBuilderBase<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled)
    {
        super.setLinearFilteringEnabled(enabled);
        return this;
    }

    @Override
    public ColorCubemapBuilderBase<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy)
    {
        super.setMaxAnisotropy(maxAnisotropy);
        return this;
    }
}
