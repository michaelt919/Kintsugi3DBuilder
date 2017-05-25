package tetzlaff.mvc.controllers.impl;

import tetzlaff.gl.vecmath.Matrix3;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.KeyCodes;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.gl.window.listeners.CursorPositionListener;
import tetzlaff.gl.window.listeners.KeyPressListener;
import tetzlaff.gl.window.listeners.KeyReleaseListener;
import tetzlaff.mvc.models.CameraModel;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.impl.BasicCameraModel;

public class FirstPersonController implements KeyPressListener, KeyReleaseListener, CursorPositionListener
{
	private boolean enabled;
	
	private CameraModel model;
	
	private Vector3 velocity;
	private Vector3 position;
	
	private double lastCursorX;
	private double lastCursorY;
	
	private float theta;
	private float phi;
	
	private boolean ignoreSensitivity = false;
	private float sensitivity = 0.5f;
	private float speed = 0.01f;
	
	public FirstPersonController()
	{
		this(new BasicCameraModel());
	}
	
	public FirstPersonController(CameraModel cameraModel)
	{
		this.model = cameraModel;
		
		this.velocity = new Vector3(0.0f, 0.0f, 0.0f);
		this.position = new Vector3(0.0f, 0.0f, 0.0f);
		
		this.lastCursorX = Float.NaN;
		this.lastCursorY = Float.NaN;
		
		this.theta = 0.0f;
		this.phi = 0.0f;
	}

	public void addAsWindowListener(Window<?> window)
	{
		window.addKeyPressListener(this);
		window.addKeyReleaseListener(this);
		window.addCursorPositionListener(this);
	}
	
	public boolean getEnabled()
	{
		return this.enabled;
	}

	public void setEnabled(boolean enabled) 
	{
		this.enabled = enabled;
		if (!enabled)
		{
			this.lastCursorX = Float.NaN;
			this.lastCursorY = Float.NaN;
			this.velocity = new Vector3(0.0f, 0.0f, 0.0f);
		}
	}
	
	public ReadonlyCameraModel getModel() 
	{
		return this.model;
	}

	@Override
	public void keyPressed(Window<?> window, int keycode, ModifierKeys mods)
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
		
		// Reset regardless of if enabled
		if (keycode == KeyCodes.X)
		{
			theta = 0.0f;
			phi = 0.0f;
			position = new Vector3(0.0f, 0.0f, 0.0f);
			this.model.setLookMatrix(Matrix4.identity());
		}
		
		ignoreSensitivity = mods.getControlModifier();
	}

	@Override
	public void keyReleased(Window<?> window, int keycode, ModifierKeys mods)
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
		
		ignoreSensitivity = mods.getControlModifier();
	}

	@Override
	public void cursorMoved(Window<?> window, double xpos, double ypos) 
	{
		if (enabled)
		{
			WindowSize size = window.getWindowSize();
			
			if (!Double.isNaN(lastCursorX) && !Double.isNaN(lastCursorY))
			{
				theta += (ignoreSensitivity ? 1.0f : sensitivity) * 2 * Math.PI * (xpos - lastCursorX) / size.width;
				phi += (ignoreSensitivity ? 1.0f : sensitivity) * Math.PI * (ypos - lastCursorY) / size.height;
				
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
	
	public void update()
	{
		if (enabled)
		{
			Matrix3 rotation = Matrix3.rotateX(phi).times(Matrix3.rotateY(theta));
			
			if (velocity.dot(velocity) > 0.0f)
			{
				position = position.plus(rotation.transpose().times(velocity.normalized().times(speed)));
			}
			
			this.model.setLookMatrix(new Matrix4(rotation).times(Matrix4.translate(position.negated())));
		}
	}
}
