package tetzlaff.reflectancefit;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.Texture;
/**
 * The CPU side of a specular reflectance parameter estimation implementation.
 * Most of the real computation is delegated to a graphics shader program.
 * @param <ContextType> The type of the graphics context to use with a particular instance.
 */
class SpecularFit<ContextType extends Context<ContextType>>
{
    private final ParameterizedFitBase<ContextType> base;
    private final Framebuffer<ContextType> framebuffer;

    SpecularFit(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int subdiv)
    {
        this.framebuffer = framebuffer;
        base = new ParameterizedFitBase<>(drawable, subdiv);
    }

    void fitImageSpace(Texture<ContextType> viewImages, Texture<ContextType> depthImages,
            Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, SubdivisionRenderingCallback callback)
    {
        base.drawable.program().setTexture("diffuseEstimate", diffuseEstimate);
        base.drawable.program().setTexture("normalEstimate", normalEstimate);
        base.fitImageSpace(framebuffer, viewImages, depthImages, callback);
    }
}
