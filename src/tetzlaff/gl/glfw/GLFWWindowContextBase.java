package tetzlaff.gl.glfw;

import java.nio.ByteBuffer;

import org.lwjgl.*;
import org.lwjgl.opengl.*;
import tetzlaff.gl.Context;
import tetzlaff.gl.FramebufferSize;

import static org.lwjgl.glfw.GLFW.*;

public abstract class GLFWWindowContextBase<ContextType extends GLFWWindowContextBase<ContextType>> implements Context<ContextType>
{
    private final long handle;

    protected GLFWWindowContextBase(long handle)
    {
        this.handle = handle;
    }

    @Override
    public void makeContextCurrent()
    {
        glfwMakeContextCurrent(handle);
        GL.createCapabilities(false);
    }

    @Override
    public void swapBuffers()
    {
        glfwSwapBuffers(handle);
    }

    @Override
    public FramebufferSize getFramebufferSize()
    {
        ByteBuffer widthBuffer = BufferUtils.createByteBuffer(4);
        ByteBuffer heightBuffer = BufferUtils.createByteBuffer(4);
        glfwGetFramebufferSize(handle, widthBuffer, heightBuffer);
        int width = widthBuffer.asIntBuffer().get(0);
        int height = heightBuffer.asIntBuffer().get(0);
        return new FramebufferSize(width, height);
    }
}
