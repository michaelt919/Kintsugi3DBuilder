package tetzlaff.reflectancefit;

import java.io.IOException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.Texture;

class DiffuseFit<ContextType extends Context<ContextType>>
{
    private final ParameterizedFitBase<ContextType> base;
    private final Framebuffer<ContextType> framebuffer;

    DiffuseFit(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int subdiv)
    {
        this.framebuffer = framebuffer;
        base = new ParameterizedFitBase<>(drawable, subdiv);
    }

    void fitImageSpace(Texture<ContextType> viewImages, Texture<ContextType> depthImages, SubdivisionRenderingCallback callback)
        throws IOException
    {
        base.fitImageSpace(framebuffer, viewImages, depthImages, callback);
    }
}
