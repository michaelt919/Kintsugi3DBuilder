package tetzlaff.texturefit;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.Texture;
import tetzlaff.texturefit.ParameterizedFitBase.SubdivisionRenderingCallback;

class AdjustFit<ContextType extends Context<ContextType>>
{
    private final ParameterizedFitBase<ContextType> base;

    AdjustFit(Drawable<ContextType> drawable, int viewCount, int subdiv)
    {
        base = new ParameterizedFitBase<>(drawable, viewCount, subdiv);
    }

    void fitImageSpace(Framebuffer<ContextType> framebuffer, Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages,
        Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, Texture<ContextType> specularEstimate, Texture<ContextType> roughnessEstimate,
        Texture<ContextType> peakEstimate, Texture<ContextType> errorTexture, SubdivisionRenderingCallback callback) throws IOException
    {
        base.drawable.program().setTexture("diffuseEstimate", diffuseEstimate);
        base.drawable.program().setTexture("normalEstimate", normalEstimate);
        base.drawable.program().setTexture("specularEstimate", specularEstimate);
        base.drawable.program().setTexture("roughnessEstimate", roughnessEstimate);
        base.drawable.program().setTexture("peakEstimate", peakEstimate);
        base.drawable.program().setTexture("errorTexture", errorTexture);
        base.fitImageSpace(framebuffer, viewImages, depthImages, shadowImages, callback);
    }

    void fitTextureSpace(Framebuffer<ContextType> framebuffer, File preprocessDirectory,
        Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, Texture<ContextType> specularEstimate,
        Texture<ContextType> roughnessEstimate, Texture<ContextType> peakEstimate,
        Texture<ContextType> errorTexture, SubdivisionRenderingCallback callback) throws IOException
    {
        base.drawable.program().setTexture("diffuseEstimate", diffuseEstimate);
        base.drawable.program().setTexture("normalEstimate", normalEstimate);
        base.drawable.program().setTexture("specularEstimate", specularEstimate);
        base.drawable.program().setTexture("roughnessEstimate", roughnessEstimate);
        base.drawable.program().setTexture("peakEstimate", peakEstimate);
        base.drawable.program().setTexture("errorTexture", errorTexture);
        base.fitTextureSpace(framebuffer, preprocessDirectory, callback);
    }
}
