package tetzlaff.gl.core;

public interface DoubleFramebufferObject<ContextType extends Context<ContextType>>
    extends DoubleFramebuffer<ContextType>, Resource
{
    void resize(int width, int height);
}
