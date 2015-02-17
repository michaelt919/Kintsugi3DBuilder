package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;
import tetzlaff.gl.AlphaBlendingFunction;
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
	
	private int blendFuncEnumToInt(AlphaBlendingFunction.Weight func)
	{
		switch(func)
		{
		case DST_ALPHA: return GL_DST_ALPHA;
		case DST_COLOR: return GL_DST_COLOR;
		case ONE: return GL_ONE;
		case ONE_MINUS_DST_ALPHA: return GL_ONE_MINUS_DST_ALPHA;
		case ONE_MINUS_DST_COLOR: return GL_ONE_MINUS_DST_COLOR;
		case ONE_MINUS_SRC_ALPHA: return GL_ONE_MINUS_SRC_ALPHA;
		case ONE_MINUS_SRC_COLOR: return GL_ONE_MINUS_SRC_COLOR;
		case SRC_ALPHA: return GL_ONE_MINUS_SRC_ALPHA;
		case SRC_COLOR: return GL_ONE_MINUS_SRC_COLOR;
		case ZERO: return GL_ZERO;
		default: throw new IllegalArgumentException();
		}
	}
	
	@Override
	public void setAlphaBlendingFunction(AlphaBlendingFunction func)
	{
		glEnable(GL_BLEND);
		glBlendFunc(blendFuncEnumToInt(func.sourceWeightFunction), blendFuncEnumToInt(func.destinationWeightFunction));
	}
	
	@Override
	public void disableAlphaBlending()
	{
		glDisable(GL_BLEND);
	}
}
