package tetzlaff.gl.helpers;

import tetzlaff.window.CursorPosition;
import tetzlaff.window.ModifierKeys;
import tetzlaff.window.MouseButtonState;
import tetzlaff.window.Window;
import tetzlaff.window.WindowSize;
import tetzlaff.window.listeners.CursorPositionListener;
import tetzlaff.window.listeners.MouseButtonPressListener;
import tetzlaff.window.listeners.MouseButtonReleaseListener;
import tetzlaff.window.listeners.ScrollListener;
import tetzlaff.window.listeners.WindowFocusGainedListener;
import tetzlaff.window.listeners.WindowFocusLostListener;

public class Trackball implements CursorPositionListener, MouseButtonPressListener, MouseButtonReleaseListener, ScrollListener, WindowFocusGainedListener, WindowFocusLostListener
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
	
	private boolean focus = false;
	
	public Trackball(float sensitivity, int primaryButtonIndex, int secondaryButtonIndex, boolean zoomWithWheel)
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
		this(sensitivity, 0, 1, true);
	}
	
	public void addAsWindowListener(Window window)
	{
		window.addCursorPositionListener(this);
		window.addMouseButtonPressListener(this);
		window.addMouseButtonReleaseListener(this);
		window.addScrollListener(this);
		window.addWindowFocusGainedListener(this);
		window.addWindowFocusLostListener(this);
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
		if (focus && (buttonIndex == this.primaryButtonIndex || buttonIndex == this.secondaryButtonIndex))
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
		if (focus)
		{
			if (this.primaryButtonIndex >= 0 && window.getMouseButtonState(primaryButtonIndex) == MouseButtonState.Pressed)
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
			else if (this.secondaryButtonIndex >= 0 && window.getMouseButtonState(secondaryButtonIndex) == MouseButtonState.Pressed)
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
	}

	@Override
	public void mouseButtonReleased(Window window, int buttonIndex, ModifierKeys mods) 
	{
		if (focus && (buttonIndex == this.primaryButtonIndex || buttonIndex == this.secondaryButtonIndex))
		{
			this.oldTrackballMatrix = this.trackballMatrix;
			this.oldLogScale = this.logScale;
		}
	}

	@Override
	public void scroll(Window window, double xoffset, double yoffset) 
	{
		if (focus)
		{
			this.logScale = this.logScale + sensitivity / 256.0f * (float)(yoffset);
			this.scale = (float)Math.pow(2, this.logScale);
		}
	}

	@Override
	public void windowFocusLost(Window window) 
	{
		this.focus = false;
		System.out.println("focus lost");
	}

	@Override
	public void windowFocusGained(Window window) 
	{
		this.focus = true;
		System.out.println("focus gained");
	}
}
