package tetzlaff.window.glfw;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.List;

// Window event callbacks
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowIconifyCallback;
import org.lwjgl.glfw.GLFWWindowPosCallback;
import org.lwjgl.glfw.GLFWWindowRefreshCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;

// Keyboard callbacks
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCharModsCallback;

// Mouse callbacks
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;

// Other misc. callbacks
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;

// Internal classes for wrapping GLFW callbacks
import tetzlaff.window.listeners.CharacterListener;
import tetzlaff.window.listeners.CharacterModifiersListener;
import tetzlaff.window.listeners.CursorEnteredListener;
import tetzlaff.window.listeners.CursorExitedListener;
import tetzlaff.window.listeners.CursorPositionListener;
import tetzlaff.window.listeners.FramebufferSizeListener;
import tetzlaff.window.listeners.KeyPressListener;
import tetzlaff.window.listeners.KeyReleaseListener;
import tetzlaff.window.listeners.KeyRepeatListener;
import tetzlaff.window.listeners.MouseButtonPressListener;
import tetzlaff.window.listeners.MouseButtonReleaseListener;
import tetzlaff.window.listeners.ScrollListener;
import tetzlaff.window.listeners.WindowCloseListener;
import tetzlaff.window.listeners.WindowFocusGainedListener;
import tetzlaff.window.listeners.WindowFocusLostListener;
import tetzlaff.window.listeners.WindowIconifiedListener;
import tetzlaff.window.listeners.WindowPositionListener;
import tetzlaff.window.listeners.WindowRefreshListener;
import tetzlaff.window.listeners.WindowRestoredListener;
import tetzlaff.window.listeners.WindowSizeListener;

public class GLFWWindowCallback implements WindowListenerManager
{
	private GLFWWindow window;
	
	private List<WindowPositionListener> windowPosListeners;
	private List<WindowSizeListener> windowSizeListeners;
	private List<WindowCloseListener> windowCloseListeners;
	private List<WindowRefreshListener> windowRefreshListeners;
	private List<WindowFocusLostListener> windowFocusLostListeners;
	private List<WindowFocusGainedListener> windowFocusGainedListeners;
	private List<WindowIconifiedListener> windowIconifiedListeners;
	private List<WindowRestoredListener> windowRestoredListeners;
	private List<FramebufferSizeListener> framebufferSizeListeners;
	private List<KeyPressListener> keyPressListeners;
	private List<KeyReleaseListener> keyReleaseListeners;
	private List<KeyRepeatListener> keyRepeatListeners;
	private List<CharacterListener> characterListeners;
	private List<CharacterModifiersListener> charModsListeners;
	private List<MouseButtonPressListener> mouseButtonPressListeners;
	private List<MouseButtonReleaseListener> mouseButtonReleaseListeners;
	private List<CursorPositionListener> cursorPosListeners;
	private List<CursorEnteredListener> cursorEnterListeners;
	private List<CursorExitedListener> cursorExitListeners;
	private List<ScrollListener> scrollListeners;
	
	public GLFWWindowCallback(GLFWWindow window)
	{
		this.window = window;
		windowPosListeners = new ArrayList<WindowPositionListener>();
		windowSizeListeners = new ArrayList<WindowSizeListener>();
		windowCloseListeners = new ArrayList<WindowCloseListener>();
		windowRefreshListeners = new ArrayList<WindowRefreshListener>();
		windowFocusLostListeners = new ArrayList<WindowFocusLostListener>();
		windowFocusGainedListeners = new ArrayList<WindowFocusGainedListener>();
		windowIconifiedListeners = new ArrayList<WindowIconifiedListener>();
		windowRestoredListeners = new ArrayList<WindowRestoredListener>();
		framebufferSizeListeners = new ArrayList<FramebufferSizeListener>();
		keyPressListeners = new ArrayList<KeyPressListener>();
		keyReleaseListeners = new ArrayList<KeyReleaseListener>();
		keyRepeatListeners = new ArrayList<KeyRepeatListener>();
		characterListeners = new ArrayList<CharacterListener>();
		charModsListeners = new ArrayList<CharacterModifiersListener>();
		mouseButtonPressListeners = new ArrayList<MouseButtonPressListener>();
		mouseButtonReleaseListeners = new ArrayList<MouseButtonReleaseListener>();
		cursorPosListeners = new ArrayList<CursorPositionListener>();
		cursorEnterListeners = new ArrayList<CursorEnteredListener>();
		cursorExitListeners = new ArrayList<CursorExitedListener>();
		scrollListeners = new ArrayList<ScrollListener>();

		createAnonymousInnerCallbacks();
	}
	
	@Override
	public void addWindowPositionListener(WindowPositionListener listener)
	{
		windowPosListeners.add(listener);
	}
	
	@Override
	public void addWindowSizeListener(WindowSizeListener listener)
	{
		windowSizeListeners.add(listener);
	}
	
	@Override
	public void addWindowCloseListener(WindowCloseListener listener)
	{
		windowCloseListeners.add(listener);
	}
	
	@Override
	public void addWindowRefreshListener(WindowRefreshListener listener)
	{
		windowRefreshListeners.add(listener);
	}
	
	@Override
	public void addWindowFocusLostListener(WindowFocusLostListener listener)
	{
		windowFocusLostListeners.add(listener);
	}
	
	@Override
	public void addWindowFocusGainedListener(WindowFocusGainedListener listener)
	{
		windowFocusGainedListeners.add(listener);
	}
	
	@Override
	public void addWindowIconifiedListener(WindowIconifiedListener listener)
	{
		windowIconifiedListeners.add(listener);
	}
	
	@Override
	public void addWindowRestoredListener(WindowRestoredListener listener)
	{
		windowRestoredListeners.add(listener);
	}
	
	@Override
	public void addFramebufferSizeListener(FramebufferSizeListener listener)
	{
		framebufferSizeListeners.add(listener);
	}
	
	@Override
	public void addKeyPressListener(KeyPressListener listener)
	{
		keyPressListeners.add(listener);
	}
	
	@Override
	public void addKeyReleaseListener(KeyReleaseListener listener)
	{
		keyReleaseListeners.add(listener);
	}
	
	@Override
	public void addKeyRepeatListener(KeyRepeatListener listener)
	{
		keyRepeatListeners.add(listener);
	}
	
	@Override
	public void addCharacterListener(CharacterListener listener)
	{
		characterListeners.add(listener);
	}
	
	@Override
	public void addCharacterModifiersListener(CharacterModifiersListener listener)
	{
		charModsListeners.add(listener);
	}
	
	@Override
	public void addMouseButtonPressListener(MouseButtonPressListener listener)
	{
		mouseButtonPressListeners.add(listener);
	}
	
	@Override
	public void addMouseButtonReleaseListener(MouseButtonReleaseListener listener)
	{
		mouseButtonReleaseListeners.add(listener);
	}
	
	@Override
	public void addCursorPositionListener(CursorPositionListener listener)
	{
		cursorPosListeners.add(listener);
	}
	
	@Override
	public void addCursorEnteredListener(CursorEnteredListener listener)
	{
		cursorEnterListeners.add(listener);
	}
	
	@Override
	public void addCursorExitedListener(CursorExitedListener listener)
	{
		cursorExitListeners.add(listener);
	}
	
	@Override
	public void addScrollListener(ScrollListener listener)
	{
		scrollListeners.add(listener);
	}
	
	// A reference to these callbacks must be maintained to prevent Java from garbage collecting them.
	@SuppressWarnings("unused")
	private GLFWWindowPosCallback windowPosCallback;
	
	@SuppressWarnings("unused")
	private GLFWWindowSizeCallback windowSizeCallback;
	
	@SuppressWarnings("unused")
	private GLFWWindowCloseCallback windowCloseCallback;
	
	@SuppressWarnings("unused")
	private GLFWWindowRefreshCallback windowRefreshCallback;
	
	@SuppressWarnings("unused")
	private GLFWWindowFocusCallback windowFocusCallback;
	
	@SuppressWarnings("unused")
	private GLFWWindowIconifyCallback windowIconifyCallback;
	
	@SuppressWarnings("unused")
	private GLFWFramebufferSizeCallback framebufferSizeCallback;
	
	@SuppressWarnings("unused")
	private GLFWKeyCallback keyCallback;
	
	@SuppressWarnings("unused")
	private GLFWCharCallback charCallback;
	
	@SuppressWarnings("unused")
	private GLFWCharModsCallback charModsCallback;
	
	@SuppressWarnings("unused")
	private GLFWMouseButtonCallback mouseButtonCallback;
	
	@SuppressWarnings("unused")
	private GLFWCursorPosCallback cursorPosCallback;
	
	@SuppressWarnings("unused")
	private GLFWCursorEnterCallback cursorEnterCallback;
	
	@SuppressWarnings("unused")
	private GLFWScrollCallback scrollCallback;
	
	@SuppressWarnings("unused")
	private GLFWDropCallback dropCallback;
	
	private void createAnonymousInnerCallbacks() {
		
		// Window position callback
		glfwSetWindowPosCallback(window.getHandle(), 
				windowPosCallback = new GLFWWindowPosCallback() 
				{
					public void invoke(long windowHandle, int xpos, int ypos) 
					{
						if (windowHandle == window.getHandle())
						{
							for (WindowPositionListener listener : windowPosListeners)
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
							for (WindowSizeListener listener : windowSizeListeners)
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
							for (WindowCloseListener listener : windowCloseListeners)
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
							for (WindowRefreshListener listener : windowRefreshListeners)
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
								if(glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
									System.err.println("GLFW: Window focus gained.");
								}
								
								for (WindowFocusGainedListener listener : windowFocusGainedListeners)
								{
									listener.windowFocusGained(window);
								}
							}
							else
							{
								System.err.println("GLFW: Window focus lost.");
								for (WindowFocusLostListener listener : windowFocusLostListeners)
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
								for (WindowIconifiedListener listener : windowIconifiedListeners)
								{
									listener.windowIconified(window);
								}
							}
							else
							{
								for (WindowRestoredListener listener : windowRestoredListeners)
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
							for (FramebufferSizeListener listener : framebufferSizeListeners)
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
							if (action == GLFW_PRESS)
							{
								for (KeyPressListener listener : keyPressListeners)
								{
									listener.keyPressed(window, keycode, new GLFWModifierKeys(mods));
								}
							}
							else if (action == GLFW_RELEASE)
							{
								for (KeyReleaseListener listener : keyReleaseListeners)
								{
									listener.keyReleased(window, keycode, new GLFWModifierKeys(mods));
								}
							}
							else if (action == GLFW_REPEAT)
							{
								for (KeyRepeatListener listener : keyRepeatListeners)
								{
									listener.keyRepeated(window, keycode, new GLFWModifierKeys(mods));
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
							for (CharacterListener listener : characterListeners)
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
							for (CharacterModifiersListener listener : charModsListeners)
							{
								listener.characterTypedWithModifiers(window, (char)codepoint, new GLFWModifierKeys(mods));
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
								System.err.println("GLFW: Mouse pressed.");
								for (MouseButtonPressListener listener : mouseButtonPressListeners)
								{
									listener.mouseButtonPressed(window, button, new GLFWModifierKeys(mods));
								}
							}
							else if (action == GLFW_RELEASE)
							{
								System.err.println("GLFW: Mouse released.");
								for (MouseButtonReleaseListener listener : mouseButtonReleaseListeners)
								{
									listener.mouseButtonReleased(window, button, new GLFWModifierKeys(mods));
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
							for (CursorPositionListener listener : cursorPosListeners)
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
								System.err.println("GLFW: Mouse entered.");
								for (CursorEnteredListener listener : cursorEnterListeners)
								{
									listener.cursorEntered(window);
								}
							}
							else
							{
								System.err.println("GLFW: Mouse exited.");
								for (CursorExitedListener listener : cursorExitListeners)
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
							System.err.println("GLFW: Mouse scrolled.");
							for (ScrollListener listener : scrollListeners)
							{
								listener.scroll(window, xoffset, yoffset);
							}
						}
					}
				});

		glfwSetDropCallback(window.getHandle(), new GLFWDropCallback() {			
			@Override
			public void invoke(long window, int count, long names) {
				// Not supported				
			}
		});
	}
}
