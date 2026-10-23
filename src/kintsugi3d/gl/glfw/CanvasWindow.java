/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.glfw;

import kintsugi3d.builder.util.OperatingSystem;
import kintsugi3d.gl.core.DoubleFramebuffer;
import kintsugi3d.gl.core.FramebufferSize;
import kintsugi3d.gl.exceptions.GLFWException;
import kintsugi3d.gl.window.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.Version.BuildType;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class CanvasWindow<ContextType extends WindowContextBase<ContextType>>
    extends CanvasBase<ContextType> implements PollableWindow, PollableCanvas3D<ContextType>
{
    private static final Logger LOG = LoggerFactory.getLogger(CanvasWindow.class);
    private final long handle;
    private final WindowListenerManager listenerManager;

    private final ContextType context;

    CanvasWindow(ContextFactory<ContextType> contextFactory,
        Function<ContextType, DoubleFramebuffer<ContextType>> createDefaultFramebuffer,
        WindowSpecification windowSpec)
    {
        System.setProperty("org.lwjgl.util.Debug", "true");
        System.setProperty("org.lwjgl.util.DebugLoader", "true");

        LOG.info("LWJGL version: {}.{}.{}{}",
            Version.VERSION_MAJOR, Version.VERSION_MINOR, Version.VERSION_REVISION,
            Version.BUILD_TYPE == BuildType.ALPHA ? "a" : ((Version.BUILD_TYPE == BuildType.BETA) ? "b" : "")
            /*Version.getVersion()*/ /* <== causes annoying exception breakpoints in Eclipse */);

        String glfwVersionString = glfwGetVersionString();
        LOG.info("GLFW version: {}", glfwVersionString);

        AtomicInteger glfwError = new AtomicInteger();

        glfwSetErrorCallback(GLFWErrorCallback.create((error, description) ->
        {
            glfwError.set(error);
            String descriptionString = GLFWErrorCallback.getDescription(description);

            if (error == GLFW_FORMAT_UNAVAILABLE)
            {
                // Suppress errors to avoid failing smoke tests that rely on fallback to OSMesa.
                LOG.warn("GLFW format unavailable.", new GLFWException(descriptionString));
            }
            else
            {
                throw new GLFWException(descriptionString);
            }
        }));

        if (OperatingSystem.getCurrentOS() == OperatingSystem.UNIX)
        {
            // Force X11 on Linux.
            // Wayland is default as of LWJGL 3.3.4+ but has issues that X11 doesn't have.
            glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11);
        }

        initGLFWWithWindowHints(windowSpec);

        long handleIfSuccessful = NULL;

        try
        {
            handleIfSuccessful = glfwCreateWindow(windowSpec.getWidth(), windowSpec.getHeight(), windowSpec.getTitle(), NULL, NULL);
        }
        catch (GLFWException e)
        {
            LOG.warn("Error creating GLFW window.", e);
        }

        if (handleIfSuccessful == NULL)
        {
            LOG.info("GLFW error code: {}", glfwError.get());

            if (glfwError.get() == GLFW_FORMAT_UNAVAILABLE && glfwVersionString.contains("OSMesa"))
            {
                LOG.info("Failed to create a native GLFW window; attempting fallback to OSMesa.");

                // Terminate GLFW to change OpenGL's contextAPI.
                glfwTerminate();

                System.setProperty("org.lwjgl.opengl.contextAPI", "OSMesa");

                System.out.println(Arrays.toString(Configuration.OPENGL_OSMESA_LIBRARY_NAME_DEFAULTS()));

                // Restart GLFW
                initGLFWWithWindowHints(windowSpec);

                // Retry with OSMesa
                // Among other things, this is needed for CI tests to run via GitHub Actions on free test runners
                glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_OSMESA_CONTEXT_API);
                glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_FALSE);
                handleIfSuccessful = glfwCreateWindow(windowSpec.getWidth(), windowSpec.getHeight(), windowSpec.getTitle(), NULL, NULL);
            }

            if (handleIfSuccessful == NULL)
            {
                throw new GLFWException("Failed to create the GLFW window");
            }
        }

        this.handle = handleIfSuccessful;

        this.listenerManager = new WindowCallback(this);

        // Query height and width of screen to set center point
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

        if (vidmode != null)
        {
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
        }
        else
        {
            LOG.error("Could not query screen size.");
        }

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);

        GL.createCapabilities(); // Make a valid OpenGL Context
        LOG.info("OpenGL version: {}", glGetString(GL_VERSION));
        LOG.info("OpenGL renderer: {}", glGetString(GL_RENDERER));

        this.context = createDefaultFramebuffer == null ?
            contextFactory.createContext(handle) : contextFactory.createContext(handle, createDefaultFramebuffer);

        if (windowSpec.getMultisamples() > 0)
        {
            context.getState().enableMultisampling();
        }
    }

    private static void initGLFWWithWindowHints(WindowSpecification windowSpec)
    {
        if (!glfwInit())
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
    public PollableCanvas3D<ContextType> getCanvas()
    {
        return this;
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
        CanvasSize winSize = getSize();
        FramebufferSize fbSize = context.getDefaultFramebufferSize();
        return winSize.width != fbSize.width || winSize.height != fbSize.height;
    }

    @Override
    public boolean isWindowClosing()
    {
        return glfwWindowShouldClose(handle);
    }

    @Override
    public void close()
    {
        glfwDestroyWindow(handle);
    }

    @Override
    public void requestWindowClose()
    {
        glfwSetWindowShouldClose(handle, true);
    }

    @Override
    public void cancelWindowClose()
    {
        glfwSetWindowShouldClose(handle, false);
    }

    @Override
    public CanvasSize getSize()
    {
        IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
        glfwGetWindowSize(handle, widthBuffer, heightBuffer);
        int width = widthBuffer.get(0);
        int height = heightBuffer.get(0);
        return new CanvasSize(width, height);
    }

    @Override
    public CanvasSize getSizeForDisplay()
    {
        return getSize();
    }

    @Override
    public CanvasPosition getPosition()
    {
        IntBuffer xBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer yBuffer = BufferUtils.createIntBuffer(1);
        glfwGetWindowPos(handle, xBuffer, yBuffer);
        int x = xBuffer.get(0);
        int y = yBuffer.get(0);
        return new CanvasPosition(x, y);
    }

    @Override
    public void setSize(int width, int height)
    {
        glfwSetWindowSize(handle, width, height);
    }

    @Override
    public void setPosition(int x, int y)
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
        DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(handle, xBuffer, yBuffer);
        double x = xBuffer.get(0);
        double y = yBuffer.get(0);
        return new CursorPosition(x, y);
    }

    @Override
    public kintsugi3d.gl.window.ModifierKeys getModifierKeys()
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
