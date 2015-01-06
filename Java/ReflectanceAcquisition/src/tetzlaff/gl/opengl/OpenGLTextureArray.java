package tetzlaff.gl.opengl;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;

public class OpenGLTextureArray extends OpenGLLayeredTexture 
{
	public OpenGLTextureArray(int width, int height, int layerCount, boolean useLinearFiltering, boolean useMipmaps)
	{
		super(width, height, layerCount, useLinearFiltering, useMipmaps);
	}

	public OpenGLTextureArray(int width, int height, int layerCount) 
	{
		super(width, height, layerCount);
	}
	
	public static OpenGLTextureArray createDepthTextureArray(int width, int height, int layerCount, boolean useLinearFiltering, boolean useMipmaps)
	{
		return new OpenGLTextureArray(GL_DEPTH_COMPONENT16, width, height, layerCount, GL_DEPTH_COMPONENT, GL_FLOAT, useLinearFiltering, useMipmaps);
	}
	
	public static OpenGLTextureArray createDepthTextureArray(int width, int height, int layerCount)
	{
		return createDepthTextureArray(width, height, layerCount, false, false);
	}

	OpenGLTextureArray(int internalFormat, int width, int height, int layerCount, int format, int type, boolean useLinearFiltering, boolean useMipmaps) 
	{
		super(internalFormat, width, height, layerCount, format, type, useLinearFiltering, useMipmaps);
	}

	OpenGLTextureArray(int internalFormat, int width, int height, int layerCount, int format, boolean useLinearFiltering, boolean useMipmaps) 
	{
		super(internalFormat, width, height, layerCount, format, useLinearFiltering, useMipmaps);
	}

	OpenGLTextureArray(int internalFormat, int width, int height, int layerCount, int format) 
	{
		super(internalFormat, width, height, layerCount, format);
	}

	@Override
	protected int getOpenGLTextureTarget() 
	{
		return GL_TEXTURE_2D_ARRAY;
	}

}
