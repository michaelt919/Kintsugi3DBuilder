package tetzlaff.ibrelight.export.specularfit;

import tetzlaff.gl.core.Context;
import java.io.File;

public interface RoughnessOptimization<ContextType extends Context<ContextType>> extends SpecularTextures<ContextType>, AutoCloseable
{
    void clear();
    void execute();
    void saveTextures(File outputDirectory);

    @Override
    void close();
}
