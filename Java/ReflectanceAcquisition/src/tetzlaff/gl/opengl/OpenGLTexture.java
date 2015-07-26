package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import tetzlaff.gl.Texture;

abstract class OpenGLTexture implements Texture<OpenGLContext>, OpenGLFramebufferAttachment
{
	protected final OpenGLContext context;
	
	private int textureId;
	
	OpenGLTexture(OpenGLContext context) 
	{
		this.context = context;
		this.textureId = glGenTextures();
		this.context.openGLErrorCheck();
	}
	
	@Override
	public OpenGLContext getContext()
	{
		return this.context;
	}
	
	abstract int getOpenGLTextureTarget();
	abstract int getLevelCount();
	
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
	public void delete() 
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
		if (level > this.getLevelCount())
		{
			throw new IllegalArgumentException("Illegal level index: " + level + ".  The texture only has " + this.getLevelCount() + " levels.");
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
