package tetzlaff.window.glfw;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.system.glfw.WindowCallback;

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

public class GLFWWindowCallback extends WindowCallback implements WindowListenerManager
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
	
	@Override
	public void windowPos(long windowHandle, int xpos, int ypos) 
	{
		if (windowHandle == this.window.getHandle())
		{
			for (WindowPositionListener listener : windowPosListeners)
			{
				listener.windowMoved(this.window, xpos, ypos);
			}
		}
	}

	@Override
	public void windowSize(long windowHandle, int width, int height) 
	{
		if (windowHandle == this.window.getHandle())
		{
			for (WindowSizeListener listener : windowSizeListeners)
			{
				listener.windowResized(this.window, width, height);
			}
		}
	}

	@Override
	public void windowClose(long windowHandle)
	{
		if (windowHandle == this.window.getHandle())
		{
			for (WindowCloseListener listener : windowCloseListeners)
			{
				listener.windowClosing(this.window);
			}
		}
		
	}

	@Override
	public void windowRefresh(long windowHandle) 
	{
		if (windowHandle == this.window.getHandle())
		{
			for (WindowRefreshListener listener : windowRefreshListeners)
			{
				listener.windowRefreshed(this.window);
			}
		}
	}

	@Override
	public void windowFocus(long windowHandle, int focused)
	{
		if (windowHandle == this.window.getHandle())
		{
			if (focused == GL_TRUE)
			{
				for (WindowFocusGainedListener listener : windowFocusGainedListeners)
				{
					listener.windowFocusGained(this.window);
				}
			}
			else
			{
				for (WindowFocusLostListener listener : windowFocusLostListeners)
				{
					listener.windowFocusLost(this.window);
				}
			}
		}
	}

	@Override
	public void windowIconify(long windowHandle, int iconified)
	{
		if (windowHandle == this.window.getHandle())
		{
			if (iconified == GL_TRUE)
			{
				for (WindowIconifiedListener listener : windowIconifiedListeners)
				{
					listener.windowIconified(this.window);
				}
			}
			else
			{
				for (WindowRestoredListener listener : windowRestoredListeners)
				{
					listener.windowRestored(this.window);
				}
			}
		}
	}

	@Override
	public void framebufferSize(long windowHandle, int width, int height) 
	{
		if (windowHandle == this.window.getHandle())
		{
			for (FramebufferSizeListener listener : framebufferSizeListeners)
			{
				listener.framebufferResized(this.window, width, height);
			}
		}
	}

	@Override
	public void key(long windowHandle, int keycode, int scancode, int action, int mods)
	{
		if (windowHandle == this.window.getHandle())
		{
			if (action == GLFW_PRESS)
			{
				for (KeyPressListener listener : keyPressListeners)
				{
					listener.keyPressed(this.window, keycode, new GLFWModifierKeys(mods));
				}
			}
			else if (action == GLFW_RELEASE)
			{
				for (KeyReleaseListener listener : keyReleaseListeners)
				{
					listener.keyReleased(this.window, keycode, new GLFWModifierKeys(mods));
				}
			}
			else if (action == GLFW_REPEAT)
			{
				for (KeyRepeatListener listener : keyRepeatListeners)
				{
					listener.keyRepeated(this.window, keycode, new GLFWModifierKeys(mods));
				}
			}
		}
	}

	@Override
	public void character(long windowHandle, int codepoint)
	{
		if (windowHandle == this.window.getHandle())
		{
			for (CharacterListener listener : characterListeners)
			{
				listener.characterTyped(this.window, (char)codepoint);
			}
		}
	}

	@Override
	public void charMods(long windowHandle, int codepoint, int mods)
	{
		if (windowHandle == this.window.getHandle())
		{
			for (CharacterModifiersListener listener : charModsListeners)
			{
				listener.characterTypedWithModifiers(this.window, (char)codepoint, new GLFWModifierKeys(mods));
			}
		}
	}

	@Override
	public void mouseButton(long windowHandle, int button, int action, int mods) 
	{
		if (windowHandle == this.window.getHandle())
		{
			if (action == GLFW_PRESS)
			{
				for (MouseButtonPressListener listener : mouseButtonPressListeners)
				{
					listener.mouseButtonPressed(this.window, button, new GLFWModifierKeys(mods));
				}
			}
			else if (action == GLFW_RELEASE)
			{
				for (MouseButtonReleaseListener listener : mouseButtonReleaseListeners)
				{
					listener.mouseButtonReleased(this.window, button, new GLFWModifierKeys(mods));
				}
			}
		}
	}

	@Override
	public void cursorPos(long windowHandle, double xpos, double ypos)
	{
		if (windowHandle == this.window.getHandle())
		{
			for (CursorPositionListener listener : cursorPosListeners)
			{
				listener.cursorMoved(this.window, xpos, ypos);
			}
		}
	}

	@Override
	public void cursorEnter(long windowHandle, int entered)
	{
		if (windowHandle == this.window.getHandle())
		{
			if (entered == GL_TRUE)
			{
				for (CursorEnteredListener listener : cursorEnterListeners)
				{
					listener.cursorEntered(this.window);
				}
			}
			else
			{
				for (CursorExitedListener listener : cursorExitListeners)
				{
					listener.cursorExited(this.window);
				}
			}
		}
	}

	@Override
	public void scroll(long windowHandle, double xoffset, double yoffset)
	{
		if (windowHandle == this.window.getHandle())
		{
			for (ScrollListener listener : scrollListeners)
			{
				listener.scroll(this.window, xoffset, yoffset);
			}
		}
	}

	@Override
	public void drop(long window, int count, long names) 
	{
		// Not supported
	}
}
