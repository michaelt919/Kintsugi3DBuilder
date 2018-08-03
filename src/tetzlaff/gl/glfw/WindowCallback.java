package tetzlaff.gl.glfw;

import org.lwjgl.glfw.*;
import tetzlaff.gl.window.Key;
import tetzlaff.gl.window.WindowListenerManagerInstance;
import tetzlaff.gl.window.listeners.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

// Other misc. callbacks
// Keyboard callbacks
// Mouse callbacks
// Window event callbacks
// Internal classes for wrapping GLFW callbacks

class WindowCallback extends WindowListenerManagerInstance
{
    private final WindowImpl<?> window;

    WindowCallback(WindowImpl<?> window)
    {
        this.window = window;

        createAnonymousInnerCallbacks();
    }

    // A reference to these callbacks must be maintained to prevent Java from garbage collecting them.
    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWWindowPosCallback windowPosCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWWindowSizeCallback windowSizeCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWWindowCloseCallback windowCloseCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWWindowRefreshCallback windowRefreshCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWWindowFocusCallback windowFocusCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWWindowIconifyCallback windowIconifyCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWFramebufferSizeCallback framebufferSizeCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWKeyCallback keyCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWCharCallback charCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWCharModsCallback charModsCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWMouseButtonCallback mouseButtonCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWCursorPosCallback cursorPosCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWCursorEnterCallback cursorEnterCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWScrollCallback scrollCallback;

    @SuppressWarnings({ "unused", "FieldCanBeLocal" })
    private GLFWDropCallback dropCallback;

    private void createAnonymousInnerCallbacks()
    {
        // Window position callback
        glfwSetWindowPosCallback(window.getHandle(),
                windowPosCallback = new GLFWWindowPosCallback()
                {
                    @Override
                    public void invoke(long windowHandle, int xpos, int ypos)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            for (WindowPositionListener listener : getWindowPosListeners())
                            {
                                listener.windowMoved(window, xpos, ypos);
                            }
                        }
                    }
                });


        glfwSetWindowSizeCallback(window.getHandle(),
                windowSizeCallback = new GLFWWindowSizeCallback()
                {
                    @Override
                    public void invoke(long windowHandle, int width, int height)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            for (WindowSizeListener listener : getWindowSizeListeners())
                            {
                                listener.windowResized(window, width, height);
                            }
                        }
                    }
                });


        glfwSetWindowCloseCallback(window.getHandle(),
                windowCloseCallback = new GLFWWindowCloseCallback()
                {
                    @Override
                    public void invoke(long windowHandle)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            for (WindowCloseListener listener : getWindowCloseListeners())
                            {
                                listener.windowClosing(window);
                            }
                        }
                    }
                });

        glfwSetWindowRefreshCallback(window.getHandle(),
                windowRefreshCallback = new GLFWWindowRefreshCallback()
                {
                    @Override
                    public void invoke(long windowHandle)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            for (WindowRefreshListener listener : getWindowRefreshListeners())
                            {
                                listener.windowRefreshed(window);
                            }
                        }
                    }
                });

        glfwSetWindowFocusCallback(window.getHandle(),
                windowFocusCallback = new GLFWWindowFocusCallback()
                {
                    @Override
                    public void invoke(long windowHandle, int focused)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            if (focused == GL_TRUE)
                            {
                                for (WindowFocusGainedListener listener : getWindowFocusGainedListeners())
                                {
                                    listener.windowFocusGained(window);
                                }
                            }
                            else
                            {
                                for (WindowFocusLostListener listener : getWindowFocusLostListeners())
                                {
                                    listener.windowFocusLost(window);
                                }
                            }
                        }
                    }
                });

        glfwSetWindowIconifyCallback(window.getHandle(),
                windowIconifyCallback = new GLFWWindowIconifyCallback()
                {
                    @Override
                    public void invoke(long windowHandle, int iconified)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            if (iconified == GL_TRUE)
                            {
                                for (WindowIconifiedListener listener : getWindowIconifiedListeners())
                                {
                                    listener.windowIconified(window);
                                }
                            }
                            else
                            {
                                for (WindowRestoredListener listener : getWindowRestoredListeners())
                                {
                                    listener.windowRestored(window);
                                }
                            }
                        }
                    }
                });

        glfwSetFramebufferSizeCallback(window.getHandle(),
                framebufferSizeCallback = new GLFWFramebufferSizeCallback()
                {
                    @Override
                    public void invoke(long windowHandle, int width, int height)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            for (FramebufferSizeListener listener : getFramebufferSizeListeners())
                            {
                                listener.framebufferResized(window, width, height);
                            }
                        }
                    }
                });

        glfwSetKeyCallback(window.getHandle(),
                keyCallback = new GLFWKeyCallback()
                {
                    @Override
                    public void invoke(long windowHandle, int keycode, int scancode, int action, int mods)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            Key key = KeyCodeMaps.codeToKey(keycode);

                            if (action == GLFW_PRESS)
                            {
                                for (KeyPressListener listener : getKeyPressListeners())
                                {
                                    listener.keyPressed(window, key, new ModifierKeys(mods));
                                }

                                for (KeyTypeListener listener : getKeyTypeListeners())
                                {
                                    listener.keyTyped(window, key, new ModifierKeys(mods));
                                }
                            }
                            else if (action == GLFW_RELEASE)
                            {
                                for (KeyReleaseListener listener : getKeyReleaseListeners())
                                {
                                    listener.keyReleased(window, key, new ModifierKeys(mods));
                                }
                            }
                            else if (action == GLFW_REPEAT)
                            {
                                for (KeyTypeListener listener : getKeyTypeListeners())
                                {
                                    listener.keyTyped(window, key, new ModifierKeys(mods));
                                }
                            }
                        }
                    }
                });

        glfwSetCharCallback(window.getHandle(),
                charCallback = new GLFWCharCallback()
                {
                    @Override
                    public void invoke(long windowHandle, int codepoint)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            for (CharacterListener listener : getCharacterListeners())
                            {
                                listener.characterTyped(window, (char)codepoint);
                            }
                        }
                    }
                });

        glfwSetCharModsCallback(window.getHandle(),
                charModsCallback = new GLFWCharModsCallback()
                {
                    @Override
                    public void invoke(long windowHandle, int codepoint, int mods)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            for (CharacterModifiersListener listener : getCharModsListeners())
                            {
                                listener.characterTypedWithModifiers(window, (char)codepoint, new ModifierKeys(mods));
                            }
                        }
                    }
                });

        glfwSetMouseButtonCallback(window.getHandle(),
                mouseButtonCallback = new GLFWMouseButtonCallback()
                {
                    @Override
                    public void invoke(long windowHandle, int button, int action, int mods)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            if (action == GLFW_PRESS)
                            {
                                for (MouseButtonPressListener listener : getMouseButtonPressListeners())
                                {
                                    listener.mouseButtonPressed(window, button, new ModifierKeys(mods));
                                }
                            }
                            else if (action == GLFW_RELEASE)
                            {
                                for (MouseButtonReleaseListener listener : getMouseButtonReleaseListeners())
                                {
                                    listener.mouseButtonReleased(window, button, new ModifierKeys(mods));
                                }
                            }
                        }
                    }
                });

        glfwSetCursorPosCallback(window.getHandle(),
                cursorPosCallback = new GLFWCursorPosCallback()
                {
                    @Override
                    public void invoke(long windowHandle, double xpos, double ypos)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            for (CursorPositionListener listener : getCursorPosListeners())
                            {
                                listener.cursorMoved(window, xpos, ypos);
                            }
                        }
                    }
                });

        glfwSetCursorEnterCallback(window.getHandle(),
                cursorEnterCallback = new GLFWCursorEnterCallback()
                {
                    @Override
                    public void invoke(long windowHandle, int entered)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            if (entered == GL_TRUE)
                            {
                                for (CursorEnteredListener listener : getCursorEnterListeners())
                                {
                                    listener.cursorEntered(window);
                                }
                            }
                            else
                            {
                                for (CursorExitedListener listener : getCursorExitListeners())
                                {
                                    listener.cursorExited(window);
                                }
                            }
                        }
                    }
                });

        glfwSetScrollCallback(window.getHandle(),
                scrollCallback = new GLFWScrollCallback()
                {
                    @Override
                    public void invoke(long windowHandle, double xoffset, double yoffset)
                    {
                        if (windowHandle == window.getHandle())
                        {
                            for (ScrollListener listener : getScrollListeners())
                            {
                                listener.scroll(window, xoffset, yoffset);
                            }
                        }
                    }
                });

        glfwSetDropCallback(window.getHandle(), new GLFWDropCallback()
        {
            @Override
            public void invoke(long window, int count, long names)
            {
                // Not supported
            }
        });
    }
}
