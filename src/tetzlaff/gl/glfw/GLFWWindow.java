package tetzlaff.gl.glfw;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.exceptions.GLFWException;
import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.KeyCodes;
import tetzlaff.gl.window.KeyState;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.MouseButtonState;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.WindowListenerManager;
import tetzlaff.gl.window.WindowPosition;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.gl.window.listeners.CharacterListener;
import tetzlaff.gl.window.listeners.CharacterModifiersListener;
import tetzlaff.gl.window.listeners.CursorEnteredListener;
import tetzlaff.gl.window.listeners.CursorExitedListener;
import tetzlaff.gl.window.listeners.CursorPositionListener;
import tetzlaff.gl.window.listeners.FramebufferSizeListener;
import tetzlaff.gl.window.listeners.KeyPressListener;
import tetzlaff.gl.window.listeners.KeyReleaseListener;
import tetzlaff.gl.window.listeners.KeyRepeatListener;
import tetzlaff.gl.window.listeners.MouseButtonPressListener;
import tetzlaff.gl.window.listeners.MouseButtonReleaseListener;
import tetzlaff.gl.window.listeners.ScrollListener;
import tetzlaff.gl.window.listeners.WindowCloseListener;
import tetzlaff.gl.window.listeners.WindowFocusGainedListener;
import tetzlaff.gl.window.listeners.WindowFocusLostListener;
import tetzlaff.gl.window.listeners.WindowIconifiedListener;
import tetzlaff.gl.window.listeners.WindowPositionListener;
import tetzlaff.gl.window.listeners.WindowRefreshListener;
import tetzlaff.gl.window.listeners.WindowRestoredListener;
import tetzlaff.gl.window.listeners.WindowSizeListener;
import tetzlaff.interactive.EventPollable;

public class GLFWWindow<ContextType extends GLFWWindowContextBase<ContextType>> implements Window<ContextType>, EventPollable
{
	private long handle;
	private boolean isDestroyed;
	private WindowListenerManager listenerManager;
	
	private ContextType context;

	GLFWWindow(GLFWContextFactory<ContextType> contextFactory, int width, int height, String title, int x, int y, boolean resizable, int multisamples) 
	{
		glfwSetErrorCallback(GLFWErrorCallback.createString((error, description) ->
		{
			throw new GLFWException(description);
		}));
		
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
        this.listenerManager = callback;

        // Query height and width of screen to set center point
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (x < 0)
        {
        	x = (vidmode.width() - width) / 2;
        }
        if (y < 0)
        {
        	y = (vidmode.height() - height) / 2;
        }
        glfwSetWindowPos(handle, x, y);
 
        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);
        
        GL.createCapabilities(); // Make a valid OpenGL Context
        System.out.println("OpenGL version: " + GL11.glGetString(GL11.GL_VERSION));
        System.out.println("LWJGL version: " + 
        		String.valueOf(Version.VERSION_MAJOR) + '.' + Version.VERSION_MINOR + '.' + Version.VERSION_REVISION + 
        		(Version.BUILD_TYPE == Version.BuildType.ALPHA ? "a" : Version.BUILD_TYPE == Version.BuildType.BETA ? "b" : "")
        		/*Version.getVersion()*/ /* <== causes annoying exception breakpoints in Eclipse */);
        System.out.println("GLFW version: " + glfwGetVersionString());
        
        this.context = contextFactory.createContext(handle);
        
        if (multisamples > 0)
        {
        	context.enableMultisampling();
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
		return this.isWindowClosing();
	}
	
	@Override
	public boolean isHighDPI()
	{
		WindowSize winSize = getWindowSize();
		FramebufferSize fbSize = context.getFramebufferSize();
		return (winSize.width != fbSize.width || winSize.height != fbSize.height);
	}
	
	@Override
	public boolean isWindowClosing()
	{
		return glfwWindowShouldClose(handle) == GL_TRUE;
	}
	
	@Override
	public boolean isResourceClosed()
	{
		return this.isDestroyed;
	}
	
	@Override
	public void close()
	{
		if (glfwWindowShouldClose(handle) == GL_TRUE)
		{
			glfwDestroyWindow(handle);
			this.isDestroyed = true;
		}
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