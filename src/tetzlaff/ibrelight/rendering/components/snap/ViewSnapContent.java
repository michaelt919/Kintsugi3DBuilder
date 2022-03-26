package tetzlaff.ibrelight.rendering.components.snap;

import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.RenderedComponent;

public abstract class ViewSnapContent<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private int snapViewIndex;

    protected int getSnapViewIndex()
    {
        return snapViewIndex;
    }

    void setSnapViewIndex(int snapViewIndex)
    {
        this.snapViewIndex = snapViewIndex;
    }
}
