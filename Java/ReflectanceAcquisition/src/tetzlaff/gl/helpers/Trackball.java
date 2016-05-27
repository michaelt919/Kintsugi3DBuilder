/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
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

/**
 * A class for a virtual "trackball" - that is, a user interface control that maps 2D mouse motions into 3D rotations, 
 * as if the cursor was rotating an invisible trackball controlling the on-screen rotation.
 * @author Michael Tetzlaff
 *
 */
public class Trackball implements CursorPositionListener, MouseButtonPressListener, MouseButtonReleaseListener, ScrollListener
{
	/**
	 * The button used for "primary" trackball actions - that is, rotating the trackball vertically and horizontally.
	 */
	private int primaryButtonIndex;
	
	/**
	 * The button used for "secondary" trackball actions - that is, rotating within the screen plane and zooming in and out.
	 * This can be set to a negative number to disable secondary trackball actions.
	 */
	private int secondaryButtonIndex;
	
	/**
	 * The sensitivity of the trackball, which controls the speed of rotation.
	 */
	private float sensitivity;
	
	/**
	 * Maintains the x-coordinate where a drag started.
	 */
	private float startX = Float.NaN;
	
	/**
	 * Maintains the y-coordinate where a drag started.
	 */
	private float startY = Float.NaN;
	
	/**
	 * Determines the scale at which to map mouse movements into rotation, based on the sensitivity and the window resolution.
	 * Moving the cursor across the smaller dimension of the window should always result in the same amount of rotation, invariant of the window size.
	 */
	private float mouseScale = Float.NaN;
	
	/**
	 * The rotation matrix at the beginning of the current drag.
	 */
	private Matrix4 oldTrackballMatrix;
	
	/**
	 * The current rotation matrix.
	 */
	private Matrix4 trackballMatrix;
	
	/**
	 * The base-2 logarithm of the scale at the beginning of the current scale.
	 */
	private float oldLogScale;
	
	/**
	 * The base-2 logarithm of the current scale.
	 */
	private float logScale;
	
	/**
	 * The current scale.
	 */
	private float scale;
	
	/**
	 * Creates a new virtual trackball.
	 * @param sensitivity The sensitivity of the trackball.
	 * @param primaryButtonIndex The button to be used for "primary" trackball actions - that is, rotating the trackball vertically and horizontally.
	 * @param secondaryButtonIndex The button to be used for "secondary" trackball actions - that is, rotating within the screen plane and zooming in and out.
	 * This can be set to a negative number to disable secondary trackball actions.
	 * @param zoomWithWheel Whether or not to enable zooming with the mouse wheel.
	 */
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
	
	/**
	 * Creates a new virtual trackball, with the conventional primary mouse button (usually the left mouse button) used as the primary trackball button,
	 * and the conventional secondary mouse button (usually the right mouse button) used as the secondary trackball button.
	 * Zooming with the mouse wheel will be enabled.
	 * @param sensitivity
	 */
	public Trackball(float sensitivity)
	{
		this(sensitivity, 0, 1, true);
	}
	
	/**
	 * Adds this trackball as a listener to the specified window.
	 * In other words, this methods makes it so that mouse drags in the window will affect the trackball.
	 * @param window The window for which to make this trackball a listener.
	 */
	public void addAsWindowListener(Window window)
	{
		window.addCursorPositionListener(this);
		window.addMouseButtonPressListener(this);
		window.addMouseButtonReleaseListener(this);
		window.addScrollListener(this);
	}
	
	/**
	 * Gets the current rotation matrix.
	 * @return The current rotation matrix.
	 */
	public Matrix4 getRotationMatrix()
	{
		return this.trackballMatrix;
	}
	
	/**
	 * Gets the current scale.
	 * @return The current scale.
	 */
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

	@Override
	public void mouseButtonReleased(Window window, int buttonIndex, ModifierKeys mods) 
	{
		if (buttonIndex == this.primaryButtonIndex || buttonIndex == this.secondaryButtonIndex)
		{
			this.oldTrackballMatrix = this.trackballMatrix;
			this.oldLogScale = this.logScale;
		}
	}

	@Override
	public void scroll(Window window, double xoffset, double yoffset) 
	{
		this.logScale = this.logScale + sensitivity / 256.0f * (float)(yoffset);
		this.scale = (float)Math.pow(2, this.logScale);
	}
}
