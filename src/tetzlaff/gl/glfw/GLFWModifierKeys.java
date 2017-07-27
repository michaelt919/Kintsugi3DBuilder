package tetzlaff.gl.glfw;

import tetzlaff.gl.window.ModifierKeys;

import static org.lwjgl.glfw.GLFW.*;

public class GLFWModifierKeys implements ModifierKeys
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
