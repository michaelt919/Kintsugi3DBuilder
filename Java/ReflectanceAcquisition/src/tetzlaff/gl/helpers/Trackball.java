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
	private int primaryButtonIndex;
	private int secondaryButtonIndex;
	private float sensitivity;
	
	private float startX = Float.NaN;
	private float startY = Float.NaN;
	
	private float mouseScale = Float.NaN;
	
	private Matrix4 oldTrackballMatrix;
	private Matrix4 trackballMatrix;
	
	private float oldLogScale;
	private float logScale;
	private float scale;
	
	public Trackball(int primaryButtonIndex, int secondaryButtonIndex, float sensitivity)
	{
		this.primaryButtonIndex = primaryButtonIndex;
		this.secondaryButtonIndex = secondaryButtonIndex;
		this.sensitivity = sensitivity;
		this.oldTrackballMatrix = Matrix4.identity();
		this.trackballMatrix = Matrix4.identity();
		this.oldLogScale = 0.0f;
		this.logScale = 0.0f;
		this.scale = 1.0f;
	}
	
	public Trackball(float sensitivity)
	{
		this(0, 1, sensitivity);
	}
	
	public void addAsWindowListener(Window window)
	{
		window.addCursorPositionListener(this);
		window.addMouseButtonPressListener(this);
		window.addMouseButtonReleaseListener(this);
	}
	
	public Matrix4 getRotationMatrix()
	{
		return this.trackballMatrix;
	}
	
	public float getScale()
	{
		return this.scale;
	}

	@Override
	public void mouseButtonPressed(Window window, int buttonIndex, ModifierKeys mods) 
	{
		if (buttonIndex == this.primaryButtonIndex || buttonIndex == this.secondaryButtonIndex)
		{
			CursorPosition pos = window.getCursorPosition();
			WindowSize size = window.getWindowSize();
			this.startX = (float)pos.x;
			this.startY = (float)pos.y;
			this.mouseScale = (float)Math.PI * this.sensitivity / Math.min(size.width, size.height);
		}
	}	

	@Override
	public void cursorMoved(Window window, double xpos, double ypos) 
	{
		if (window.getMouseButtonState(primaryButtonIndex) == MouseButtonState.Pressed)
		{
			if (!Float.isNaN(startX) && !Float.isNaN(startY) && !Float.isNaN(mouseScale) && !Float.isNaN(mouseScale))
			{
				Vector3 rotationVector = 
					new Vector3(
						(float)(ypos - this.startY),
						(float)(xpos - this.startX), 
						0.0f
					);
					
				this.trackballMatrix = 
					Matrix4.rotateAxis(
						rotationVector.normalized(), 
						this.mouseScale * rotationVector.length()
					)
					.times(this.oldTrackballMatrix);
			}
		}
		else if (window.getMouseButtonState(secondaryButtonIndex) == MouseButtonState.Pressed)
		{
			if (!Float.isNaN(startX) && !Float.isNaN(startY) && !Float.isNaN(mouseScale) && !Float.isNaN(mouseScale))
			{
				this.trackballMatrix = 
					Matrix4.rotateZ(this.mouseScale * (xpos - this.startX))
						.times(this.oldTrackballMatrix);
				
				this.logScale = this.oldLogScale + this.mouseScale * (float)(ypos - this.startY);
				this.scale = (float)Math.pow(2, this.logScale);
			}
		}
	}

	@Override
	public void mouseButtonReleased(Window window, int buttonIndex, ModifierKeys mods) 
	{
		if (buttonIndex == this.primaryButtonIndex || buttonIndex == this.secondaryButtonIndex)
		{
			this.oldTrackballMatrix = this.trackballMatrix;
			this.oldLogScale = this.logScale;
		}
	}
}
