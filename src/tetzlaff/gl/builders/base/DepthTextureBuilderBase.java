package tetzlaff.gl.builders.base;

import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;
import tetzlaff.gl.builders.DepthTextureBuilder;

public abstract class DepthTextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> 
    extends TextureBuilderBase<ContextType, TextureType> implements DepthTextureBuilder<ContextType, TextureType>
{
    private int precision = 8;
    private boolean floatingPoint;

    protected int getInternalPrecision()
    {
        return this.precision;
    }

    protected boolean isFloatingPointEnabled()
    {
        return this.floatingPoint;
    }

    protected DepthTextureBuilderBase(ContextType context)
    {
        super(context);
    }

    @Override
    public DepthTextureBuilderBase<ContextType, TextureType> setInternalPrecision(int precision)
    {
        this.precision = precision;;
        return this;
    }

    @Override
    public DepthTextureBuilderBase<ContextType, TextureType> setFloatingPointEnabled(boolean enabled)
    {
        this.floatingPoint = enabled;
        return this;
    }

    @Override
    public DepthTextureBuilderBase<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations)
    {
        super.setMultisamples(samples, fixedSampleLocations);
        return this;
    }

    @Override
    public DepthTextureBuilderBase<ContextType, TextureType> setMipmapsEnabled(boolean enabled)
    {
        super.setMipmapsEnabled(enabled);
        return this;
    }

    @Override
    public DepthTextureBuilderBase<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled)
    {
        super.setLinearFilteringEnabled(enabled);
        return this;
    }

    @Override
    public DepthTextureBuilderBase<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy)
    {
        super.setMaxAnisotropy(maxAnisotropy);
        return this;
    }
}
