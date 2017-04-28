package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Texture;
import tetzlaff.gl.TextureType;

abstract class OpenGLTexture implements Texture<OpenGLContext>, OpenGLFramebufferAttachment
{
	protected final OpenGLContext context;
	
	private int textureId;
	
	private ColorFormat colorFormat = null;
	private CompressionFormat compressionFormat = null;
	private TextureType textureType;
	
	OpenGLTexture(OpenGLContext context, TextureType textureType)
	{
		this.context = context;
		this.textureType = textureType;
		this.textureId = glGenTextures();
		this.context.openGLErrorCheck();
	}
	
	OpenGLTexture(OpenGLContext context, ColorFormat colorFormat) 
	{
		this(context, TextureType.COLOR);
		this.colorFormat = colorFormat;
	}
	
	OpenGLTexture(OpenGLContext context, CompressionFormat compressionFormat) 
	{
		this(context, TextureType.COLOR);
		this.compressionFormat = compressionFormat;
	}
	
	@Override
	public OpenGLContext getContext()
	{
		return this.context;
	}
	
	@Override
	public ColorFormat getInternalUncompressedColorFormat()
	{
		return this.colorFormat;
	}
	
	@Override
	public CompressionFormat getInternalCompressedColorFormat()
	{
		return this.compressionFormat;
	}
	
	@Override
	public boolean isInternalFormatCompressed()
	{
		return this.compressionFormat != null;
	}
	
	@Override
	public TextureType getTextureType()
	{
		return textureType;
	}
	
	abstract int getOpenGLTextureTarget();
	
	void bind()
	{
		glBindTexture(this.getOpenGLTextureTarget(), this.textureId);
		this.context.openGLErrorCheck();
	}
	
	int getTextureId()
	{
		return this.textureId;
	}
	
	void bindToTextureUnit(int textureUnitIndex)
	{
		if (textureUnitIndex < 0)
		{
			throw new IllegalArgumentException("Texture unit index cannot be negative.");
		}
		else if (textureUnitIndex > this.context.getMaxCombinedTextureImageUnits())
		{
			throw new IllegalArgumentException("Texture unit index (" + textureUnitIndex + ") is greater than the maximum allowed index (" + 
					(this.context.getMaxCombinedTextureImageUnits()-1) + ").");
		}
		glActiveTexture(GL_TEXTURE0 + textureUnitIndex);
		this.context.openGLErrorCheck();
		this.bind();
	}

	@Override
	public void close() 
	{
		glDeleteTextures(this.textureId);
		this.context.openGLErrorCheck();
	}

	@Override
	public void attachToDrawFramebuffer(int attachment, int level) 
	{
		if (level < 0)
		{
			throw new IllegalArgumentException("Texture level cannot be negative.");
		}
		if (level > this.getMipmapLevelCount())
		{
			throw new IllegalArgumentException("Illegal level index: " + level + ".  The texture only has " + this.getMipmapLevelCount() + " levels.");
		}
		glFramebufferTexture(GL_DRAW_FRAMEBUFFER, attachment, this.textureId, level);
		this.context.openGLErrorCheck();
	}

	@Override
	public void attachToReadFramebuffer(int attachment, int level) 
	{
		glFramebufferTexture(GL_READ_FRAMEBUFFER, attachment, this.textureId, level);
		this.context.openGLErrorCheck();
	}
}
