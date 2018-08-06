package tetzlaff.gl.core;

import java.util.function.Consumer;

public interface DoubleFramebufferObject<ContextType extends Context<ContextType>>
    extends DoubleFramebuffer<ContextType>, Resource
{
    void requestResize(int width, int height);
    void addSwapListener(Consumer<Framebuffer<ContextType>> listener);
}
