package tetzlaff.gl.glfw;

import java.util.function.Function;

import tetzlaff.gl.core.DoubleFramebuffer;

public interface ContextFactory<ContextType extends WindowContextBase<ContextType>>
{
    ContextType createContext(long glfwHandle);
    ContextType createContext(long glfwHandle, Function<ContextType, DoubleFramebuffer<ContextType>> createDefaultFramebuffer);
}
