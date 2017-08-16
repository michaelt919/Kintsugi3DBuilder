package tetzlaff.gl.glfw;

public interface GLFWContextFactory<ContextType extends GLFWWindowContextBase<ContextType>>
{
    ContextType createContext(long glfwHandle);
}
