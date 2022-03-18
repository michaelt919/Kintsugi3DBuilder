package tetzlaff.ibrelight.core;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.vecmath.Matrix4;

public interface RenderedComponent<ContextType extends Context<ContextType>> extends AutoCloseable
{
    void initialize() throws Exception;
    void reloadShaders() throws Exception;
    void draw(Framebuffer<ContextType> framebuffer, Matrix4 view);
}
