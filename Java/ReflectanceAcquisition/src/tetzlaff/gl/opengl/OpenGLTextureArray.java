package tetzlaff.gl.opengl;
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
