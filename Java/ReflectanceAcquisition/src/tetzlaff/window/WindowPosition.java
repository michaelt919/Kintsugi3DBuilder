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
package tetzlaff.window;

/**
 * A class for representing the position of a window on the screen.
 * @author Michael Tetzlaff
 *
 */
public class WindowPosition 
{
	/**
	 * The x-coordinate of the window position, in logical pixels.
	 */
	public final int x;
	
	/**
	 * The y-coordinate of the window position, in logical pixels.
	 */
	public final int y;
	
	/**
	 * Creates a new object for representing a window position.
	 * @param x The x-coordinate of the window position.
	 * @param y The y-coordinate of the window position.
	 */
	public WindowPosition(int x, int y) 
	{
		this.x = x;
		this.y = y;
	}
}
