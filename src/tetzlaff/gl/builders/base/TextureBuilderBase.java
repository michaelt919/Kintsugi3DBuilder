package tetzlaff.gl.builders.base;

import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;
import tetzlaff.gl.builders.TextureBuilder;

public abstract class TextureBuilderBase<ContextType extends Context<ContextType>, TextureType extends Texture<ContextType>> implements TextureBuilder<ContextType, TextureType>
{
    protected final ContextType context;
    private int multisamples = 1;
    private boolean fixedMultisampleLocations = true;
    private boolean mipmapsEnabled = false;
    private boolean linearFilteringEnabled = false;
    private float maxAnisotropy = 1.0f;

    protected int getMultisamples()
    {
        return multisamples;
    }

    protected boolean areMultisampleLocationsFixed()
    {
        return fixedMultisampleLocations;
    }

    protected boolean areMipmapsEnabled()
    {
        return mipmapsEnabled;
    }

    protected boolean isLinearFilteringEnabled()
    {
        return linearFilteringEnabled;
    }

    protected float getMaxAnisotropy()
    {
        return maxAnisotropy;
    }

    protected TextureBuilderBase(ContextType context)
    {
        this.context = context;
    }

    @Override
    public TextureBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations)
    {
        multisamples = samples;
        fixedMultisampleLocations = fixedSampleLocations;
        return this;
    }

    @Override
    public TextureBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled)
    {
        mipmapsEnabled = enabled;
        return this;
    }

    @Override
    public TextureBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled)
    {
        linearFilteringEnabled = enabled;
        return this;
    }

    @Override
    public TextureBuilder<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy)
    {
        this.maxAnisotropy = maxAnisotropy;
        return this;
    }
}
