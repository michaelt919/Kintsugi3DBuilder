package tetzlaff.gl.builders.base;

import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;
import tetzlaff.gl.builders.StencilTextureBuilder;

public abstract class StencilTextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> 
    extends TextureBuilderBase<ContextType, TextureType> implements StencilTextureBuilder<ContextType, TextureType>
{
    private int precision = 8;

    protected int getInternalPrecision()
    {
        return this.precision;
    }

    protected StencilTextureBuilderBase(ContextType context)
    {
        super(context);
    }

    @Override
    public StencilTextureBuilderBase<ContextType, TextureType> setInternalPrecision(int precision)
    {
        this.precision = precision;
        return this;
    }

    @Override
    public StencilTextureBuilderBase<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations)
    {
        super.setMultisamples(samples, fixedSampleLocations);
        return this;
    }

    @Override
    public StencilTextureBuilderBase<ContextType, TextureType> setMipmapsEnabled(boolean enabled)
    {
        super.setMipmapsEnabled(enabled);
        return this;
    }

    @Override
    public StencilTextureBuilderBase<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled)
    {
        super.setLinearFilteringEnabled(enabled);
        return this;
    }

    @Override
    public StencilTextureBuilderBase<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy)
    {
        super.setMaxAnisotropy(maxAnisotropy);
        return this;
    }
}
