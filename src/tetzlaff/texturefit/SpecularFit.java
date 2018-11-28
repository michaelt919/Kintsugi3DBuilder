package tetzlaff.texturefit;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.Texture;
import tetzlaff.texturefit.ParameterizedFitBase.SubdivisionRenderingCallback;

class SpecularFit<ContextType extends Context<ContextType>>
{
    private final ParameterizedFitBase<ContextType> base;
    private final Framebuffer<ContextType> framebuffer;

    SpecularFit(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, int viewCount, int subdiv)
    {
        this.framebuffer = framebuffer;
        base = new ParameterizedFitBase<>(drawable, viewCount, subdiv);
    }

    void fitImageSpace(Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages,
        Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, Texture<ContextType> peakEstimate,
        SubdivisionRenderingCallback callback) throws IOException
    {
        base.drawable.program().setTexture("diffuseEstimate", diffuseEstimate);
        base.drawable.program().setTexture("normalEstimate", normalEstimate);
        base.drawable.program().setTexture("peakEstimate", peakEstimate);
        base.fitImageSpace(framebuffer, viewImages, depthImages, shadowImages, callback);
    }

    void fitTextureSpace(File preprocessDirectory, Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate,
        Texture<ContextType> peakEstimate, SubdivisionRenderingCallback callback)
        throws IOException
    {
        base.drawable.program().setTexture("diffuseEstimate", diffuseEstimate);
        base.drawable.program().setTexture("normalEstimate", normalEstimate);
        base.drawable.program().setTexture("peakEstimate", peakEstimate);
        base.fitTextureSpace(framebuffer, preprocessDirectory, callback);
    }
}
