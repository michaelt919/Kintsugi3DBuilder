package tetzlaff.ibrelight.export.specularfit;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Texture2D;

public interface SpecularTextures<ContextType extends Context<ContextType>>
{
    Texture2D<ContextType> getReflectivityTexture();
    Texture2D<ContextType> getRoughnessTexture();
}
