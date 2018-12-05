package tetzlaff.reflectancefit;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.Texture;

/**
 * The CPU side of a diffuse reflectance parameter estimation implementation.
 * Most of the real computation is delegated to a graphics shader program.
 * @param <ContextType> The type of the graphics context to use with a particular instance.
 */
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
    {
        base.fitImageSpace(framebuffer, viewImages, depthImages, callback);
    }
}
