package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;

import tetzlaff.gl.Context;

public abstract class OpenGLContext implements Context
{
	@Override
	public void enableDepthTest()
	{
		glEnable(GL_DEPTH_TEST);
		openGLErrorCheck();
	}
	
	@Override
	public void disableDepthTest()
	{
		glDisable(GL_DEPTH_TEST);
		openGLErrorCheck();
	}
	
	@Override
	public void enableMultisampling()
	{
		glEnable(GL_MULTISAMPLE);
		openGLErrorCheck();
	}
	
	@Override
	public void disableMultisampling()
	{
		glDisable(GL_MULTISAMPLE);
		openGLErrorCheck();
	}
	
	@Override
	public void enableBackFaceCulling()
	{
		glEnable(GL_CULL_FACE);
		openGLErrorCheck();
	}
	
	@Override
	public void disableBackFaceCulling()
	{
		glDisable(GL_CULL_FACE);
		openGLErrorCheck();
	}
}
