package tetzlaff.gl.glfw;

import java.nio.IntBuffer;

import org.lwjgl.*;
import org.lwjgl.opengl.*;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferSize;

import static org.lwjgl.glfw.GLFW.*;

public abstract class WindowContextBase<ContextType extends WindowContextBase<ContextType>> implements Context<ContextType>
{
    private final long handle;

    protected WindowContextBase(long handle)
    {
        this.handle = handle;
    }

    @Override
    public void makeContextCurrent()
    {
        glfwMakeContextCurrent(handle);
        GL.createCapabilities(false);
    }

    public void swapDefaultFramebuffer()
    {
        glfwSwapBuffers(handle);
    }

    public FramebufferSize getDefaultFramebufferSize()
    {
        IntBuffer widthBuffer = BufferUtils.createByteBuffer(4).asIntBuffer();
        IntBuffer heightBuffer = BufferUtils.createByteBuffer(4).asIntBuffer();
        glfwGetFramebufferSize(handle, widthBuffer, heightBuffer);
        int width = widthBuffer.get(0);
        int height = heightBuffer.get(0);
        return new FramebufferSize(width, height);
    }

    @Override
    public void close()
    {
        glfwDestroyWindow(handle);
    }
}
