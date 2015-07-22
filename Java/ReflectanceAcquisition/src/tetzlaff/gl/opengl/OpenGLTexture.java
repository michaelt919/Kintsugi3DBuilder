package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;
import tetzlaff.gl.Texture;

public abstract class OpenGLTexture implements Texture<OpenGLContext>, OpenGLFramebufferAttachment
{
	public static final int MAX_COMBINED_TEXTURE_IMAGE_UNITS;
	
	private int textureId;
	
	static
	{
		MAX_COMBINED_TEXTURE_IMAGE_UNITS = glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
		openGLErrorCheck();
	}
	
	protected OpenGLTexture() 
	{
		this.textureId = glGenTextures();
		openGLErrorCheck();
	}
	
	protected abstract int getOpenGLTextureTarget();
	protected abstract int getLevelCount();
	
	protected void bind()
	{
		glBindTexture(this.getOpenGLTextureTarget(), this.textureId);
		openGLErrorCheck();
	}
	
	protected int getTextureId()
	{
		return this.textureId;
	}
	
	public static int getTextureUnitCount()
	{
		return MAX_COMBINED_TEXTURE_IMAGE_UNITS;
	}
	
	void bindToTextureUnit(int textureUnitIndex)
	{
		if (textureUnitIndex < 0)
		{
			throw new IllegalArgumentException("Texture unit index cannot be negative.");
		}
		else if (textureUnitIndex > MAX_COMBINED_TEXTURE_IMAGE_UNITS)
		{
			throw new IllegalArgumentException("Texture unit index (" + textureUnitIndex + ") is greater than the maximum allowed index (" + 
					(MAX_COMBINED_TEXTURE_IMAGE_UNITS-1) + ").");
		}
		glActiveTexture(GL_TEXTURE0 + textureUnitIndex);
		openGLErrorCheck();
		this.bind();
	}
	
	static void unbindTextureUnit(int textureUnitIndex)
	{
		if (textureUnitIndex < 0)
		{
			throw new IllegalArgumentException("Texture unit index cannot be negative.");
		}
		else if (textureUnitIndex > MAX_COMBINED_TEXTURE_IMAGE_UNITS)
		{
			throw new IllegalArgumentException("Texture unit index (" + textureUnitIndex + ") is greater than the maximum allowed index (" + 
					(MAX_COMBINED_TEXTURE_IMAGE_UNITS-1) + ").");
		}
		glActiveTexture(GL_TEXTURE0 + textureUnitIndex);
		openGLErrorCheck();
		glBindTexture(GL_TEXTURE_2D, 0);
		openGLErrorCheck();
	}

	@Override
	public void delete() 
	{
		glDeleteTextures(this.textureId);
		openGLErrorCheck();
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
		openGLErrorCheck();
	}

	@Override
	public void attachToReadFramebuffer(int attachment, int level) 
	{
		glFramebufferTexture(GL_READ_FRAMEBUFFER, attachment, this.textureId, level);
		openGLErrorCheck();
	}
}
