package tetzlaff.gl.helpers;

import tetzlaff.window.CursorPosition;
import tetzlaff.window.ModifierKeys;
import tetzlaff.window.MouseButtonState;
import tetzlaff.window.Window;
import tetzlaff.window.WindowSize;
import tetzlaff.window.glfw.WindowListenerManager;
import tetzlaff.window.listeners.CursorPositionListener;
import tetzlaff.window.listeners.MouseButtonPressListener;
import tetzlaff.window.listeners.MouseButtonReleaseListener;

public class Trackball implements CursorPositionListener, MouseButtonPressListener, MouseButtonReleaseListener
{
	private int buttonIndex;
	private float sensitivity;
	
	private float startX = Float.NaN;
	private float startY = Float.NaN;
	
	private float scale = Float.NaN;
	
	private Matrix4 oldRotationMatrix;
	private Matrix4 rotationMatrix;
	
	public Trackball(int buttonIndex, float sensitivity)
	{
		this.buttonIndex = buttonIndex;
		this.sensitivity = sensitivity;
		this.oldRotationMatrix = Matrix4.identity();
		this.rotationMatrix = Matrix4.identity();
	}
	
	public Trackball(float sensitivity)
	{
		this(0, sensitivity);
	}
	
	public void addAsWindowListener(Window window)
	{
		window.addCursorPositionListener(this);
		window.addMouseButtonPressListener(this);
		window.addMouseButtonReleaseListener(this);
	}
	
	public Matrix4 getRotationMatrix()
	{
		return this.rotationMatrix;
	}

	@Override
	public void mouseButtonPressed(Window window, int buttonIndex, ModifierKeys mods) 
	{
		if (buttonIndex == this.buttonIndex)
		{
			CursorPosition pos = window.getCursorPosition();
			WindowSize size = window.getWindowSize();
			this.startX = (float)pos.x;
			this.startY = (float)pos.y;
			this.scale = (float)Math.PI * this.sensitivity / Math.min(size.width, size.height);
		}
	}	

	@Override
	public void cursorMoved(Window window, double xpos, double ypos) 
	{
		if (window.getMouseButtonState(buttonIndex) == MouseButtonState.Pressed)
		{
			if (!Float.isNaN(startX) && !Float.isNaN(startY) && !Float.isNaN(scale) && !Float.isNaN(scale))
			{
				Vector3 rotationVector = 
					new Vector3(
						(float)(ypos - this.startY),
						(float)(xpos - this.startX), 
						0.0f
					);
					
				this.rotationMatrix = 
					Matrix4.rotateAxis(
						rotationVector.normalized(), 
						this.scale * rotationVector.length()
					)
					.times(this.oldRotationMatrix);
			}
		}
	}

	@Override
	public void mouseButtonReleased(Window window, int buttonIndex, ModifierKeys mods) 
	{
		if (buttonIndex == this.buttonIndex)
		{
			this.oldRotationMatrix = this.rotationMatrix;
		}
	}
}
