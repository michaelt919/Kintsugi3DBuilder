package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import tetzlaff.gl.AlphaBlendingFunction;
import tetzlaff.gl.ContextBase;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.builders.DepthStencilTextureBuilder;
import tetzlaff.gl.builders.DepthTextureBuilder;
import tetzlaff.gl.builders.FramebufferObjectBuilder;
import tetzlaff.gl.builders.StencilTextureBuilder;
import tetzlaff.gl.builders.TextureBuilder;
import tetzlaff.gl.opengl.OpenGLFramebufferObject.OpenGLFramebufferObjectBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.OpenGLTexture2DColorBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.OpenGLTexture2DDepthBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.OpenGLTexture2DStencilBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.OpenGLTexture2DDepthStencilBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.OpenGLTexture2DFromBufferBuilder;
import tetzlaff.gl.opengl.OpenGLTexture2D.OpenGLTexture2DFromFileBuilder;
import tetzlaff.gl.opengl.OpenGLTexture3D.OpenGLTexture3DColorBuilder;
import tetzlaff.gl.opengl.OpenGLTexture3D.OpenGLTexture3DDepthBuilder;
import tetzlaff.gl.opengl.OpenGLTexture3D.OpenGLTexture3DStencilBuilder;
import tetzlaff.gl.opengl.OpenGLTexture3D.OpenGLTexture3DDepthStencilBuilder;

public abstract class OpenGLContext extends ContextBase<OpenGLContext>
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
	public FramebufferObjectBuilder<OpenGLContext> getFramebufferObjectBuilder(int width, int height)
	{
		return new OpenGLFramebufferObjectBuilder(this, width, height);
	}
	
	@Override
	public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> get2DColorTextureBuilder(InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
	{
		return new OpenGLTexture2DFromFileBuilder(this, GL_TEXTURE_2D, imageStream, maskStream, flipVertical);
	}
	
	@Override
	public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> get2DColorTextureBuilder(int width, int height)
	{
		return new OpenGLTexture2DColorBuilder(this, GL_TEXTURE_2D, width, height);
	}
	
	@Override
	public DepthTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> get2DDepthTextureBuilder(int width, int height)
	{
		return new OpenGLTexture2DDepthBuilder(this, GL_TEXTURE_2D, width, height);
	}
	
	@Override
	public StencilTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> get2DStencilTextureBuilder(int width, int height)
	{
		return new OpenGLTexture2DStencilBuilder(this, GL_TEXTURE_2D, width, height);
	}
	
	@Override
	public DepthStencilTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> get2DDepthStencilTextureBuilder(int width, int height)
	{
		return new OpenGLTexture2DDepthStencilBuilder(this, GL_TEXTURE_2D, width, height);
	}
	
	// Supporting code:
	// Author: Stefan Gustavson (stegu@itn.liu.se) 2004
	// You may use, modify and redistribute this code free of charge,
	// provided that my name and this notice appears intact.
	// https://github.com/ashima/webgl-noise
	// Modified by Michael Tetzlaff
	
	private static final int[] perm = {151,160,137,91,90,15,
			  131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
			  190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
			  88,237,149,56,87,174,20,125,136,171,168, 68,175,74,165,71,134,139,48,27,166,
			  77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
			  102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,
			  135,130,116,188,159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,
			  5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
			  223,183,170,213,119,248,152, 2,44,154,163, 70,221,153,101,155,167, 43,172,9,
			  129,22,39,253, 19,98,108,110,79,113,224,232,178,185, 112,104,218,246,97,228,
			  251,34,242,193,238,210,144,12,191,179,162,241, 81,51,145,235,249,14,239,107,
			  49,192,214, 31,181,199,106,157,184, 84,204,176,115,121,50,45,127, 4,150,254,
			  138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180};
	
	private static final int[][] grad3 = {{0,1,1},{0,1,-1},{0,-1,1},{0,-1,-1},
                   {1,0,1},{1,0,-1},{-1,0,1},{-1,0,-1},
                   {1,1,0},{1,-1,0},{-1,1,0},{-1,-1,0}, // 12 cube edges
                   {1,0,-1},{-1,0,-1},{0,-1,1},{0,1,1}}; // 4 more to make 16
	
	@Override
	public ColorTextureBuilder<OpenGLContext, ? extends Texture2D<OpenGLContext>> getPerlinNoiseTextureBuilder()
	{
		ByteBuffer pixels = ByteBuffer.allocateDirect(256*256*4);
		for(int i = 0; i<256; i++)
		{
		    for(int j = 0; j<256; j++) 
		    {
		    	int offset = (i*256+j)*4;
		    	byte value = (byte)perm[(j+perm[i]) & 0xFF];
		    	pixels.put(offset, (byte)(grad3[value & 0x0F][0] * 64 + 64));   // Gradient x
		    	pixels.put(offset+1, (byte)(grad3[value & 0x0F][1] * 64 + 64)); // Gradient y
	    		pixels.put(offset+2, (byte)(grad3[value & 0x0F][2] * 64 + 64)); // Gradient z
	    		pixels.put(offset+3, value);                     // Permuted index
		    }
		}
		return new OpenGLTexture2DFromBufferBuilder(this, GL_TEXTURE_2D, 256, 256, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
	}

	// End of supporting code

	
	@Override
	public ColorTextureBuilder<OpenGLContext, ? extends Texture3D<OpenGLContext>> get2DColorTextureArrayBuilder(int width, int height, int length)
	{
		return new OpenGLTexture3DColorBuilder(this, GL_TEXTURE_2D_ARRAY, width, height, length);
	}
	
	@Override
	public DepthTextureBuilder<OpenGLContext, ? extends Texture3D<OpenGLContext>> get2DDepthTextureArrayBuilder(int width, int height, int length)
	{
		return new OpenGLTexture3DDepthBuilder(this, GL_TEXTURE_2D_ARRAY, width, height, length);
	}
	
	@Override
	public StencilTextureBuilder<OpenGLContext, ? extends Texture3D<OpenGLContext>> get2DStencilTextureArrayBuilder(int width, int height, int length)
	{
		return new OpenGLTexture3DStencilBuilder(this, GL_TEXTURE_2D_ARRAY, width, height, length);
	}
	
	@Override
	public DepthStencilTextureBuilder<OpenGLContext, ? extends Texture3D<OpenGLContext>> get2DDepthStencilTextureArrayBuilder(int width, int height, int length)
	{
		return new OpenGLTexture3DDepthStencilBuilder(this, GL_TEXTURE_2D_ARRAY, width, height, length);
	}
}
