package tetzlaff.ibrelight.export.specularfit;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.gl.core.Texture2D;

public interface RoughnessOptimization<ContextType extends Context<ContextType>> extends SpecularTextures<ContextType>, AutoCloseable
{
    void clear();
    void execute();
    void saveTextures();

    @Override
    void close();
}
