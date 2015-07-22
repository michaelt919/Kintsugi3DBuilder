package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;
import tetzlaff.gl.AlphaBlendingFunction;
import tetzlaff.gl.Context;
import tetzlaff.gl.opengl.OpenGLFramebufferObject.OpenGLFramebufferObjectBuilder;

public abstract class OpenGLContext implements Context
{
	@Override
	public void flush()
	{
		glFlush();
		openGLErrorCheck();
	}
	
	@Override
	public void finish()
	{
		glFinish();
		openGLErrorCheck();
	}
	
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
		openGLErrorCheck();
		glBlendFunc(blendFuncEnumToInt(func.sourceWeightFunction), blendFuncEnumToInt(func.destinationWeightFunction));
		openGLErrorCheck();
	}
	
	@Override
	public void disableAlphaBlending()
	{
		glDisable(GL_BLEND);
		openGLErrorCheck();
	}
	
	private int getInteger(int queryId)
	{
		int queryResult = glGetInteger(queryId);
		openGLErrorCheck();
		return queryResult;
	}
	
	public int getMaxCombinedVertexUniformComponents()
	{
		return getInteger(GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS);
	}
	
	public int getMaxCombinedFragmentUniformComponents()
	{
		return getInteger(GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS);
	}
	
	public int getMaxUniformBlockSize()
	{
		return getInteger(GL_MAX_UNIFORM_BLOCK_SIZE);
	}
	
	public int getMaxVertexUniformComponents()
	{
		return getInteger(GL_MAX_VERTEX_UNIFORM_COMPONENTS);
	}
	
	public int getMaxFragmentUniformComponents()
	{
		return getInteger(GL_MAX_FRAGMENT_UNIFORM_COMPONENTS);
	}
	
	public int getMaxArrayTextureLayers()
	{
		return getInteger(GL_MAX_ARRAY_TEXTURE_LAYERS);
	}
	
	@Override
	public OpenGLFramebufferObjectBuilder getFramebufferObjectBuilder(int width, int height)
	{
		return new OpenGLFramebufferObjectBuilder(width, height);
	}
}
