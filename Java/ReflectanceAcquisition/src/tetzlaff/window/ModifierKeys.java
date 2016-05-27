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
 * A class for representing the states of all modifier keys.
 * @author Michael Tetzlaff
 *
 */
public interface ModifierKeys
{
	/**
	 * Gets whether the shift modifier is active.
	 * @return true if the shift modifier is active, false otherwise.
	 */
	boolean getShiftModifier();
	
	/**
	 * Gets whether the control/ctrl modifier is active.
	 * @return true if the control modifier is active, false otherwise.
	 */
	boolean getControlModifier();
	
	/**
	 * Gets whether the alt modifier is active.
	 * @return true if the alt modifier is active, false otherwise.
	 */
	boolean getAltModifier();
	
	/**
	 * Gets whether the "super" modifier is active.
	 * @return true if the super modifier is active, false otherwise.
	 */
	boolean getSuperModifier();
}
