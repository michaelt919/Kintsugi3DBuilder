/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.glfw;

import java.nio.ByteBuffer;
import java.util.function.Function;

import org.lwjgl.*;
import org.lwjgl.Version.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import tetzlaff.gl.core.DoubleFramebuffer;
import tetzlaff.gl.core.FramebufferSize;
import tetzlaff.gl.exceptions.GLFWException;
import tetzlaff.gl.window.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class WindowImpl<ContextType extends WindowContextBase<ContextType>>
    extends WindowBase<ContextType> implements PollableWindow<ContextType>
{
    private final long handle;
    private final WindowListenerManager listenerManager;

    private final ContextType context;

    WindowImpl(ContextFactory<ContextType> contextFactory,
        Function<ContextType, DoubleFramebuffer<ContextType>> createDefaultFramebuffer,
        WindowSpecification windowSpec)
    {
        glfwSetErrorCallback(GLFWErrorCallback.createString((error, description) ->
        {
            throw new GLFWException(description);
        }));

        if ( glfwInit() != GL_TRUE )
        {
            throw new GLFWException("Unable to initialize GLFW.");
        }

        glfwDefaultWindowHints();

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, windowSpec.isResizable() ? GL_TRUE : GL_FALSE);
        glfwWindowHint(GLFW_SAMPLES, windowSpec.getMultisamples());

        handle = glfwCreateWindow(windowSpec.getWidth(), windowSpec.getHeight(), windowSpec.getTitle(), NULL, NULL);
        if ( handle == NULL )
        {
            throw new GLFWException("Failed to create the GLFW window");
        }

        this.listenerManager = new WindowCallback(this);

        // Query height and width of screen to set center point
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

        int xAdj = windowSpec.getX();
        if (xAdj < 0)
        {
            xAdj = (vidmode.width() - windowSpec.getWidth()) / 2;
        }

        int yAdj = windowSpec.getY();
        if (yAdj < 0)
        {
            yAdj = (vidmode.height() - windowSpec.getHeight()) / 2;
        }

        glfwSetWindowPos(handle, xAdj, yAdj);

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);

        GL.createCapabilities(); // Make a valid OpenGL Context
        System.out.println("OpenGL version: " + glGetString(GL_VERSION));
        System.out.println("LWJGL version: " +
                String.valueOf(Version.VERSION_MAJOR) + '.' + Version.VERSION_MINOR + '.' + Version.VERSION_REVISION +
                (Version.BUILD_TYPE == BuildType.ALPHA ? "a" : Version.BUILD_TYPE == BuildType.BETA ? "b" : "")
                /*Version.getVersion()*/ /* <== causes annoying exception breakpoints in Eclipse */);
        System.out.println("GLFW version: " + glfwGetVersionString());

        this.context = createDefaultFramebuffer == null ?
            contextFactory.createContext(handle) : contextFactory.createContext(handle, createDefaultFramebuffer);

        if (windowSpec.getMultisamples() > 0)
        {
            context.getState().enableMultisampling();
        }
    }

    public static void closeAllWindows()
    {
        glfwTerminate();
    }

    long getHandle()
    {
        return handle;
    }

    @Override
    public ContextType getContext()
    {
        return this.context;
    }

    @Override
    protected WindowListenerManager getListenerManager()
    {
        return listenerManager;
    }

    @Override
    public void show()
    {
        glfwShowWindow(handle);
    }

    @Override
    public void hide()
    {
        glfwHideWindow(handle);
    }

    @Override
    public void focus()
    {
        // TODO requires newer LWJGL version.
    }

    @Override
    public void pollEvents()
    {
        glfwMakeContextCurrent(handle);
        glfwPollEvents();
    }

    @Override
    public boolean shouldTerminate()
    {
        return this.isWindowClosing();
    }

    @Override
    public boolean isHighDPI()
    {
        WindowSize winSize = getWindowSize();
        FramebufferSize fbSize = context.getDefaultFramebufferSize();
        return winSize.width != fbSize.width || winSize.height != fbSize.height;
    }

    @Override
    public boolean isWindowClosing()
    {
        return glfwWindowShouldClose(handle) == GL_TRUE;
    }

    @Override
    public void close()
    {
        glfwDestroyWindow(handle);
    }

    @Override
    public void requestWindowClose()
    {
        glfwSetWindowShouldClose(handle, GL_TRUE);
    }

    @Override
    public void cancelWindowClose()
    {
        glfwSetWindowShouldClose(handle, GL_FALSE);
    }

    @Override
    public WindowSize getWindowSize()
    {
        ByteBuffer widthBuffer = BufferUtils.createByteBuffer(Integer.BYTES);
        ByteBuffer heightBuffer = BufferUtils.createByteBuffer(Integer.BYTES);
        glfwGetWindowSize(handle, widthBuffer, heightBuffer);
        int width = widthBuffer.asIntBuffer().get(0);
        int height = heightBuffer.asIntBuffer().get(0);
        return new WindowSize(width, height);
    }

    @Override
    public WindowPosition getWindowPosition()
    {
        ByteBuffer xBuffer = BufferUtils.createByteBuffer(Integer.BYTES);
        ByteBuffer yBuffer = BufferUtils.createByteBuffer(Integer.BYTES);
        glfwGetWindowPos(handle, xBuffer, yBuffer);
        int x = xBuffer.asIntBuffer().get(0);
        int y = yBuffer.asIntBuffer().get(0);
        return new WindowPosition(x, y);
    }

    @Override
    public void setWindowSize(int width, int height)
    {
        glfwSetWindowSize(handle, width, height);
    }

    @Override
    public void setWindowPosition(int x, int y)
    {
        glfwSetWindowPos(handle, x, y);
    }

    @Override
    public void setWindowTitle(String title)
    {
        glfwSetWindowTitle(handle, title);
    }

    @Override
    public MouseButtonState getMouseButtonState(int buttonIndex)
    {
        switch (glfwGetMouseButton(handle, buttonIndex))
        {
        case GLFW_PRESS: return MouseButtonState.PRESSED;
        case GLFW_RELEASE: return MouseButtonState.RELEASED;
        default: return MouseButtonState.UNKNOWN;
        }
    }

    private KeyState getKeyState(int keycode)
    {
        switch (glfwGetKey(handle, keycode))
        {
            case GLFW_PRESS: return KeyState.PRESSED;
            case GLFW_RELEASE: return KeyState.RELEASED;
            default: return KeyState.UNKNOWN;
        }
    }

    @Override
    public KeyState getKeyState(Key key)
    {
        for (int code : KeyCodeMaps.keyToCodes(key))
        {
            if(getKeyState(code) == KeyState.PRESSED)
            {
                return KeyState.PRESSED;
            }
        }

        return KeyState.RELEASED;
    }

    @Override
    public CursorPosition getCursorPosition()
    {
        ByteBuffer xBuffer = BufferUtils.createByteBuffer(Double.BYTES);
        ByteBuffer yBuffer = BufferUtils.createByteBuffer(Double.BYTES);
        glfwGetCursorPos(handle, xBuffer, yBuffer);
        double x = xBuffer.asDoubleBuffer().get(0);
        double y = yBuffer.asDoubleBuffer().get(0);
        return new CursorPosition(x, y);
    }

    @Override
    public tetzlaff.gl.window.ModifierKeys getModifierKeys()
    {
        return new ModifierKeys(
            getKeyState(GLFW_KEY_LEFT_SHIFT) == KeyState.PRESSED || getKeyState(GLFW_KEY_RIGHT_SHIFT) == KeyState.PRESSED,
            getKeyState(GLFW_KEY_LEFT_CONTROL) == KeyState.PRESSED || getKeyState(GLFW_KEY_RIGHT_CONTROL) == KeyState.PRESSED,
            getKeyState(GLFW_KEY_LEFT_ALT) == KeyState.PRESSED || getKeyState(GLFW_KEY_RIGHT_ALT) == KeyState.PRESSED,
            getKeyState(GLFW_KEY_LEFT_SUPER) == KeyState.PRESSED || getKeyState(GLFW_KEY_RIGHT_SUPER) == KeyState.PRESSED
        );
    }

    @Override
    public boolean isFocused()
    {
        return glfwGetWindowAttrib(handle, GLFW_FOCUSED) == GLFW_TRUE;
    }
}
