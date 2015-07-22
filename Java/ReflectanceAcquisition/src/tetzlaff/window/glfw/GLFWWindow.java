package tetzlaff.window.glfw;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.glfw.GLFW.*;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.system.glfw.ErrorCallback.StrAdapter;
import org.lwjgl.system.glfw.GLFWvidmode;
import org.lwjgl.system.glfw.WindowCallback;

import tetzlaff.gl.Context;
import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.exceptions.GLFWException;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.interactive.EventPollable;
import tetzlaff.window.CursorPosition;
import tetzlaff.window.KeyCodes;
import tetzlaff.window.KeyState;
import tetzlaff.window.ModifierKeys;
import tetzlaff.window.MouseButtonState;
import tetzlaff.window.Window;
import tetzlaff.window.WindowPosition;
import tetzlaff.window.WindowSize;
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

public class GLFWWindow extends OpenGLContext implements Window, Context, EventPollable
{
	private long handle;
	private boolean isDestroyed;
	private WindowListenerManager listenerManager;

	public GLFWWindow(int width, int height, String title, int x, int y, boolean resizable, int multisamples) 
	{
		Sys.touch();
		glfwSetErrorCallback(new StrAdapter() 
		{
			@Override
			public void invoke(int error, String description) 
			{
				throw new GLFWException(description);
			}
		});
		 
        if ( glfwInit() != GL11.GL_TRUE )
        {
            throw new GLFWException("Unable to initialize GLFW.");
        }
 
        glfwDefaultWindowHints();
        
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, resizable ? GL_TRUE : GL_FALSE);
        glfwWindowHint(GLFW_SAMPLES, multisamples);
        
        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if ( handle == NULL )
        {
            throw new GLFWException("Failed to create the GLFW window");
        }
        
        GLFWWindowCallback callback = new GLFWWindowCallback(this);
        WindowCallback.set(handle, callback);
        this.listenerManager = callback;

        ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (x < 0)
        {
        	x = (GLFWvidmode.width(vidmode) - width) / 2;
        }
        if (y < 0)
        {
        	y = (GLFWvidmode.height(vidmode) - height) / 2;
        }
        glfwSetWindowPos(handle, x, y);
 
        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);
        
        GLContext.createFromCurrent();
        System.out.println("OpenGL version: " + GL11.glGetString(GL11.GL_VERSION));

        if (multisamples > 0)
        {
        	this.enableMultisampling();
        }
	}
	
	public GLFWWindow(int width, int height, String title, int x, int y, boolean resizable) 
	{
		this(width, height, title, x, y, resizable, 0);
	}
	
	public GLFWWindow(int width, int height, String title, boolean resizable, int multisamples) 
	{
		this(width, height, title, -1, -1, resizable, multisamples);
	}
	
	public GLFWWindow(int width, int height, String title, boolean resizable) 
	{
		this(width, height, title, -1, -1, resizable, 0);
	}
	
	public GLFWWindow(int width, int height, String title) 
	{
		this(width, height, title, false);
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
	public void pollEvents()
	{
        glfwMakeContextCurrent(handle);
		glfwPollEvents();
	}
	
	@Override
	public boolean shouldTerminate()
	{
		return this.isClosing();
	}
	
	@Override
	public boolean isClosing()
	{
		return glfwWindowShouldClose(handle) == GL_TRUE;
	}
	
	@Override
	public boolean isDestroyed()
	{
		return this.isDestroyed;
	}
	
	@Override
	public void destroy()
	{
		if (glfwWindowShouldClose(handle) == GL_TRUE)
		{
			glfwDestroyWindow(handle);
			this.isDestroyed = true;
		}
	}
	
	@Override
	public void requestClose()
	{
		glfwSetWindowShouldClose(handle, GL_TRUE);
	}
	
	@Override
	public void cancelClose()
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
		case GLFW_PRESS: return MouseButtonState.Pressed;
		case GLFW_RELEASE: return MouseButtonState.Released;
		default: return MouseButtonState.Unknown;
		}
	}

	@Override
	public KeyState getKeyState(int keycode) 
	{
		switch (glfwGetKey(handle, keycode))
		{
		case GLFW_PRESS: return KeyState.Pressed;
		case GLFW_RELEASE: return KeyState.Released;
		default: return KeyState.Unknown;
		}
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
	public ModifierKeys getModifierKeys() 
	{
		return new GLFWModifierKeys(
			getKeyState(KeyCodes.LEFT_SHIFT) == KeyState.Pressed || getKeyState(KeyCodes.RIGHT_SHIFT) == KeyState.Pressed,
			getKeyState(KeyCodes.LEFT_CONTROL) == KeyState.Pressed || getKeyState(KeyCodes.RIGHT_CONTROL) == KeyState.Pressed,
			getKeyState(KeyCodes.LEFT_ALT) == KeyState.Pressed || getKeyState(KeyCodes.RIGHT_ALT) == KeyState.Pressed,
			getKeyState(KeyCodes.LEFT_SUPER) == KeyState.Pressed || getKeyState(KeyCodes.RIGHT_SUPER) == KeyState.Pressed
		);
	}
	
	@Override
	public void makeContextCurrent()
	{
		glfwMakeContextCurrent(handle);
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

	@Override
	public void addWindowPositionListener(WindowPositionListener listener) 
	{
		listenerManager.addWindowPositionListener(listener);
	}

	@Override
	public void addWindowSizeListener(WindowSizeListener listener) 
	{
		listenerManager.addWindowSizeListener(listener);
	}

	@Override
	public void addWindowCloseListener(WindowCloseListener listener) 
	{
		listenerManager.addWindowCloseListener(listener);
	}

	@Override
	public void addWindowRefreshListener(WindowRefreshListener listener) 
	{
		listenerManager.addWindowRefreshListener(listener);
	}

	@Override
	public void addWindowFocusLostListener(WindowFocusLostListener listener) 
	{
		listenerManager.addWindowFocusLostListener(listener);
	}

	@Override
	public void addWindowFocusGainedListener(WindowFocusGainedListener listener) 
	{
		listenerManager.addWindowFocusGainedListener(listener);
	}

	@Override
	public void addWindowIconifiedListener(WindowIconifiedListener listener) 
	{
		listenerManager.addWindowIconifiedListener(listener);
	}

	@Override
	public void addWindowRestoredListener(WindowRestoredListener listener) 
	{
		listenerManager.addWindowRestoredListener(listener);
	}

	@Override
	public void addFramebufferSizeListener(FramebufferSizeListener listener) 
	{
		listenerManager.addFramebufferSizeListener(listener);
	}

	@Override
	public void addKeyPressListener(KeyPressListener listener)
	{
		listenerManager.addKeyPressListener(listener);
	}
	
	@Override
	public void addKeyReleaseListener(KeyReleaseListener listener)
	{
		listenerManager.addKeyReleaseListener(listener);
	}

	@Override
	public void addKeyRepeatListener(KeyRepeatListener listener)
	{
		listenerManager.addKeyRepeatListener(listener);
	}

	@Override
	public void addCharacterListener(CharacterListener listener) 
	{
		listenerManager.addCharacterListener(listener);
	}

	@Override
	public void addCharacterModifiersListener(CharacterModifiersListener listener) 
	{
		listenerManager.addCharacterModifiersListener(listener);
	}

	@Override
	public void addMouseButtonPressListener(MouseButtonPressListener listener)
	{
		listenerManager.addMouseButtonPressListener(listener);
	}

	@Override
	public void addMouseButtonReleaseListener(MouseButtonReleaseListener listener) 
	{
		listenerManager.addMouseButtonReleaseListener(listener);
	}

	@Override
	public void addCursorPositionListener(CursorPositionListener listener) 
	{
		listenerManager.addCursorPositionListener(listener);
	}

	@Override
	public void addCursorEnteredListener(CursorEnteredListener listener) 
	{
		listenerManager.addCursorEnteredListener(listener);
	}

	@Override
	public void addCursorExitedListener(CursorExitedListener listener) 
	{
		listenerManager.addCursorExitedListener(listener);
	}

	@Override
	public void addScrollListener(ScrollListener listener) 
	{
		listenerManager.addScrollListener(listener);
	}
}
