package tetzlaff.texturefit;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.Texture;
import tetzlaff.texturefit.ParameterizedFitBase.SubdivisionRenderingCallback;

class DiffuseFit<ContextType extends Context<ContextType>>
{
    private final ParameterizedFitBase<ContextType> base;
    private final Framebuffer<ContextType> framebuffer;

    DiffuseFit(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int viewCount, int subdiv)
    {
        this.framebuffer = framebuffer;
        base = new ParameterizedFitBase<>(drawable, viewCount, subdiv);
    }

    void fitImageSpace(Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages, SubdivisionRenderingCallback callback)
        throws IOException
    {
        base.fitImageSpace(framebuffer, viewImages, depthImages, shadowImages, callback);
    }

    void fitTextureSpace(File preprocessDirectory,  SubdivisionRenderingCallback callback) throws IOException
    {
        base.fitTextureSpace(framebuffer, preprocessDirectory, callback);
    }
}
