package tetzlaff.mvc.old.controllers.impl;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.window.CursorPosition;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.MouseButtonState;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.WindowSize;
import tetzlaff.gl.window.listeners.CursorPositionListener;
import tetzlaff.gl.window.listeners.MouseButtonPressListener;
import tetzlaff.gl.window.listeners.ScrollListener;
import tetzlaff.models.ReadonlyCameraModel;
import tetzlaff.mvc.old.controllers.CameraController;
import tetzlaff.mvc.old.models.TrackballModel;

public class TrackballController implements CameraController, CursorPositionListener, MouseButtonPressListener, ScrollListener
{
	private int inversion = 1;
	private boolean enabled = true;
	private int primaryButtonIndex;
	private int secondaryButtonIndex;
	private float sensitivity;
	
	private float startX = Float.NaN;
	private float startY = Float.NaN;
	private float mouseScale = Float.NaN;
	
	private Matrix4 oldTrackballMatrix;
	private float oldLogScale;
	
	private final TrackballModel model;
	
	public static interface Builder
	{
		Builder setSensitivity(float sensitivity);
		Builder setPrimaryButtonIndex(int primaryButtonIndex);
		Builder setSecondaryButtonIndex(int secondaryButtonIndex);
		Builder setModel(TrackballModel model);
		TrackballController create();
	}
	
	private static class BuilderImpl implements Builder
	{
		private float sensitivity = 1.0f;
		private int primaryButtonIndex = 0;
		private int secondaryButtonIndex = 1;
		private TrackballModel model;
		
		public Builder setSensitivity(float sensitivity)
		{
			this.sensitivity = sensitivity;
			return this;
		}
		
		public Builder setPrimaryButtonIndex(int primaryButtonIndex)
		{
			this.primaryButtonIndex = primaryButtonIndex;
			return this;
		}
		
		public Builder setSecondaryButtonIndex(int secondaryButtonIndex)
		{
			this.secondaryButtonIndex = secondaryButtonIndex;
			return this;
		}
		
		public Builder setModel(TrackballModel model)
		{
			this.model = model;
			return this;
		}
		
		public TrackballController create()
		{
			if (this.model == null)
			{
				this.model = new TrackballModel();
			}
			
			return new TrackballController(model, sensitivity, primaryButtonIndex, secondaryButtonIndex);
		}
	}
	
	public static Builder getBuilder()
	{
		return new BuilderImpl();
	}
	
	private TrackballController(TrackballModel model, float sensitivity, int primaryButtonIndex, int secondaryButtonIndex)
	{
		this.primaryButtonIndex = primaryButtonIndex;
		this.secondaryButtonIndex = secondaryButtonIndex;
		this.sensitivity = sensitivity;
		this.model = model;
	}
	
	@Override
	public void addAsWindowListener(Window<?> window)
	{
		window.addCursorPositionListener(this);
		window.addMouseButtonPressListener(this);
		window.addScrollListener(this);
	}
	
	@Override
	public ReadonlyCameraModel getCameraModel()
	{
		return this.model;
	}

	@Override
	public void mouseButtonPressed(Window<?> window, int buttonIndex, ModifierKeys mods) 
	{
		if (enabled && (buttonIndex == this.primaryButtonIndex || buttonIndex == this.secondaryButtonIndex))
		{
			CursorPosition pos = window.getCursorPosition();
			WindowSize size = window.getWindowSize();
			this.startX = (float)pos.x;
			this.startY = (float)pos.y;
			this.mouseScale = (float)Math.PI * this.sensitivity / Math.min(size.width, size.height);
			this.oldTrackballMatrix = model.getTrackballMatrix();
			this.oldLogScale = model.getLogScale();
		}
	}	

	@Override
	public void cursorMoved(Window<?> window, double xpos, double ypos) 
	{
		if (enabled)
		{
			if (this.primaryButtonIndex >= 0 && window.getMouseButtonState(primaryButtonIndex) == MouseButtonState.Pressed)
			{
				if (!Float.isNaN(startX) && !Float.isNaN(startY) && !Float.isNaN(mouseScale) && !Float.isNaN(mouseScale) && (xpos != this.startX || ypos != this.startY))
				{
					Vector3 rotationVector = 
						new Vector3(
							(float)(ypos - this.startY),
							(float)(xpos - this.startX), 
							0.0f
						);
					
					this.model.setTrackballMatrix(
						Matrix4.rotateAxis(
							rotationVector.normalized(), 
							this.mouseScale * rotationVector.length() * this.inversion
						)
						.times(this.oldTrackballMatrix));
				}
			}
			else if (this.secondaryButtonIndex >= 0 && window.getMouseButtonState(secondaryButtonIndex) == MouseButtonState.Pressed)
			{
				if (!Float.isNaN(startX) && !Float.isNaN(startY) && !Float.isNaN(mouseScale) && !Float.isNaN(mouseScale))
				{
					this.model.setTrackballMatrix(
						Matrix4.rotateZ(this.mouseScale * (xpos - this.startX) * this.inversion)
							.times(this.oldTrackballMatrix));
					
					this.model.setLogScale(this.oldLogScale + this.mouseScale * (float)(ypos - this.startY));
				}
			}
		}
	}

	@Override
	public void scroll(Window<?> window, double xoffset, double yoffset) 
	{
		if (enabled)
		{
			model.setLogScale(model.getLogScale() + sensitivity / 256.0f * (float)(yoffset));
		}
	}
	
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
	
	public void setInverted(boolean inverted)
	{
		this.inversion = inverted ? -1 : 1;
	}
}
