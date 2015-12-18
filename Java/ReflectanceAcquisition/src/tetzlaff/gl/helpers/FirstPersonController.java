package tetzlaff.gl.helpers;

import tetzlaff.window.KeyCodes;
import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;
import tetzlaff.window.WindowSize;
import tetzlaff.window.listeners.CursorPositionListener;
import tetzlaff.window.listeners.KeyPressListener;
import tetzlaff.window.listeners.KeyReleaseListener;

public class FirstPersonController implements CameraController, KeyPressListener, KeyReleaseListener, CursorPositionListener
{
	private boolean enabled;
	
	private Matrix4 view;
	
	private Vector3 velocity;
	private Vector3 position;
	
	private double lastCursorX;
	private double lastCursorY;
	
	private float theta;
	private float phi;
	
	private float sensitivity = 1.0f;
	
	public FirstPersonController()
	{
		this.view = Matrix4.identity();
		
		this.velocity = new Vector3(0.0f, 0.0f, 0.0f);
		this.position = new Vector3(0.0f, 0.0f, 0.0f);
		
		this.lastCursorX = Float.NaN;
		this.lastCursorY = Float.NaN;
		
		this.theta = 0.0f;
		this.phi = 0.0f;
	}

	public void addAsWindowListener(Window window)
	{
		window.addKeyPressListener(this);
		window.addKeyReleaseListener(this);
		window.addCursorPositionListener(this);
	}

	public void setEnabled(boolean enabled) 
	{
		this.enabled = enabled;
	}

	@Override
	public Matrix4 getViewMatrix() 
	{
		return this.view;
	}

	@Override
	public void keyPressed(Window window, int keycode, ModifierKeys mods)
	{
		if (enabled)
		{
			if (keycode == KeyCodes.W)
			{
				velocity = velocity.plus(new Vector3(0.0f, 0.0f, -1.0f));
			}
			
			if (keycode == KeyCodes.S)
			{
				velocity = velocity.plus(new Vector3(0.0f, 0.0f, 1.0f));
			}
			
			if (keycode == KeyCodes.D)
			{
				velocity = velocity.plus(new Vector3(1.0f, 0.0f, 0.0f));
			}
			
			if (keycode == KeyCodes.A)
			{
				velocity = velocity.plus(new Vector3(-1.0f, 0.0f, 0.0f));
			}
			
			if (keycode == KeyCodes.E)
			{
				velocity = velocity.plus(new Vector3(0.0f, 1.0f, 0.0f));
			}
			
			if (keycode == KeyCodes.Q)
			{
				velocity = velocity.plus(new Vector3(0.0f, -1.0f, 0.0f));
			}
		}
	}

	@Override
	public void keyReleased(Window window, int keycode, ModifierKeys mods)
	{
		if (enabled)
		{
			if (keycode == KeyCodes.W)
			{
				velocity = velocity.minus(new Vector3(0.0f, 0.0f, -1.0f));
			}
			
			if (keycode == KeyCodes.S)
			{
				velocity = velocity.minus(new Vector3(0.0f, 0.0f, 1.0f));
			}
			
			if (keycode == KeyCodes.D)
			{
				velocity = velocity.minus(new Vector3(1.0f, 0.0f, 0.0f));
			}
			
			if (keycode == KeyCodes.A)
			{
				velocity = velocity.minus(new Vector3(-1.0f, 0.0f, 0.0f));
			}
			
			if (keycode == KeyCodes.E)
			{
				velocity = velocity.minus(new Vector3(0.0f, 1.0f, 0.0f));
			}
			
			if (keycode == KeyCodes.Q)
			{
				velocity = velocity.minus(new Vector3(0.0f, -1.0f, 0.0f));
			}
		}
	}

	@Override
	public void cursorMoved(Window window, double xpos, double ypos) 
	{
		if (enabled)
		{
			WindowSize size = window.getWindowSize();
			
			if (!Double.isNaN(lastCursorX) && !Double.isNaN(lastCursorY))
			{
				theta += sensitivity * 2 * Math.PI * (xpos - lastCursorX) / size.width;
				phi += sensitivity * Math.PI * (ypos - lastCursorY) / size.height;
				
				if (theta < 0.0f)
				{
					theta += 2 * Math.PI;
				}
				
				if (theta > 2 * Math.PI)
				{
					theta -= 2 * Math.PI;
				}
				
				if (phi > Math.PI / 2)
				{
					phi = (float)Math.PI / 2;
				}
				
				if (phi < -Math.PI / 2)
				{
					phi = -(float)Math.PI / 2;
				}
			}
			
			lastCursorX = xpos;
			lastCursorY = ypos;
		}
	}
	
	void update()
	{
		if (enabled)
		{
			Matrix3 rotation = Matrix3.rotateX(phi).times(Matrix3.rotateY(theta));
			position = position.plus(rotation.transpose().times(velocity));
			
			view = new Matrix4(rotation).times(Matrix4.translate(position));
		}
	}
}
