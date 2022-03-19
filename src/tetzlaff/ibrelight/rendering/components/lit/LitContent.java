package tetzlaff.ibrelight.rendering.components.lit;

import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.RenderedComponent;
import tetzlaff.ibrelight.rendering.resources.LightingResources;

public abstract class LitContent<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private LightingResources<ContextType> lightingResources;

    protected LightingResources<ContextType> getLightingResources()
    {
        return lightingResources;
    }

    void setLightingResources(LightingResources<ContextType> lightingResources)
    {
        this.lightingResources = lightingResources;
    }
}
