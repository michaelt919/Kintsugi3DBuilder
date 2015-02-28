package tetzlaff.gl.opengl;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;

public class OpenGLTextureArray extends OpenGLLayeredTexture 
{
	public OpenGLTextureArray(int width, int height, int layerCount, boolean useFloatingPointStorage, boolean useLinearFiltering, boolean useMipmaps)
	{
		super(useFloatingPointStorage ? GL_RGBA32F : GL_RGBA8, width, height, layerCount, GL_BGRA, 
				useFloatingPointStorage ? GL_FLOAT : GL_UNSIGNED_BYTE, useLinearFiltering, useMipmaps);
	}

	public OpenGLTextureArray(int width, int height, int layerCount) 
	{
		super(width, height, layerCount);
	}
	
	public static OpenGLTextureArray createDepthTextureArray(int width, int height, int layerCount, boolean useLinearFiltering, boolean useMipmaps)
	{
		return new OpenGLTextureArray(GL_DEPTH_COMPONENT16, width, height, layerCount, GL_DEPTH_COMPONENT, GL_UNSIGNED_SHORT, useLinearFiltering, useMipmaps);
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
