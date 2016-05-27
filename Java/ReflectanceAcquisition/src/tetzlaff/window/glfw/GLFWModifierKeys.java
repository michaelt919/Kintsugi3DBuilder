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
package tetzlaff.window.glfw;
import static org.lwjgl.glfw.GLFW.*;
import tetzlaff.window.ModifierKeys;

class GLFWModifierKeys implements ModifierKeys
{
	private int glfwCode;
	
	GLFWModifierKeys(int glfwCode)
	{
		this.glfwCode = glfwCode;
	}
	
	GLFWModifierKeys(boolean shiftMod, boolean controlMod, boolean altMod, boolean superMod)
	{
		glfwCode = 
			(shiftMod ? GLFW_MOD_SHIFT : 0) 	|
			(controlMod ? GLFW_MOD_CONTROL : 0) |
			(altMod ? GLFW_MOD_ALT : 0) 		|
			(superMod ? GLFW_MOD_SUPER : 0);
	}
	
	@Override
	public boolean getShiftModifier()
	{
		return (glfwCode & GLFW_MOD_SHIFT) != 0;
	}
	
	@Override
	public boolean getControlModifier()
	{
		return (glfwCode & GLFW_MOD_CONTROL) != 0;
	}
	
	@Override
	public boolean getAltModifier()
	{
		return (glfwCode & GLFW_MOD_ALT) != 0;
	}
	
	@Override
	public boolean getSuperModifier()
	{
		return (glfwCode & GLFW_MOD_SUPER) != 0;
	}
}
