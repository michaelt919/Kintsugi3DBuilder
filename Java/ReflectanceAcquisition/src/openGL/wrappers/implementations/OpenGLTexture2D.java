package openGL.wrappers.implementations;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static openGL.OpenGLHelper.*;

import java.io.IOException;
import java.io.InputStream;

import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;

public class OpenGLTexture2D extends OpenGLTexture
{
	private int levelCount;
	
	public OpenGLTexture2D(int internalFormat, int width, int height, boolean useLinearFiltering, boolean useMipmaps) 
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		super();
		this.bind();
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
		openGLErrorCheck();
		this.init(width, height, useLinearFiltering, useMipmaps);
	}
	
	public OpenGLTexture2D(int internalFormat, int width, int height) 
	{
		this(internalFormat, width, height, false, false);
	}
	
	public OpenGLTexture2D(int internalFormat, String fileFormat, InputStream fileStream, boolean flipVertical, boolean useLinearFiltering, boolean useMipmaps) throws IOException
	{
		// Use Slick just to load the texture from a file
		org.newdawn.slick.opengl.Texture texture = TextureLoader.getTexture(fileFormat, fileStream, flipVertical);
		this.setTextureId(texture.getTextureID());
		this.bind();
		this.init(texture.getTextureWidth(), texture.getTextureHeight(), useLinearFiltering, useMipmaps);
	}
	
	public OpenGLTexture2D(int internalFormat, String fileFormat, InputStream fileStream) throws IOException
	{
		this(internalFormat, fileFormat, fileStream, false, false, false);
	}
	
	public OpenGLTexture2D(int internalFormat, String fileFormat, String filename, boolean flipVertical, boolean useLinearFiltering, boolean useMipmaps) throws IOException
	{
		this(internalFormat, fileFormat, ResourceLoader.getResourceAsStream(filename), flipVertical, useLinearFiltering, useMipmaps);
	}
	
	public OpenGLTexture2D(int internalFormat, String fileFormat, String filename) throws IOException
	{
		this(internalFormat, fileFormat, filename, false, false, false);
	}
	
	private void init(int width, int height, boolean useLinearFiltering, boolean useMipmaps)
	{
		if (useMipmaps)
		{
			// Create mipmaps
			glGenerateMipmap(GL_TEXTURE_2D);
	        openGLErrorCheck();
	        
	        // Calculate the number of mipmap levels
			this.levelCount = 0;
			int dim = Math.max(width, height);
			while (dim > 0)
			{
				this.levelCount++;
				dim /= 2;
			}
			
			if (useLinearFiltering)
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		        openGLErrorCheck();
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		        openGLErrorCheck();
			}
			else
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
		        openGLErrorCheck();
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		        openGLErrorCheck();
			}
		}
		else
		{
			// No mipmaps
			this.levelCount = 1;
			
			if (useLinearFiltering)
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		        openGLErrorCheck();
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		        openGLErrorCheck();
			}
			else
			{
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		        openGLErrorCheck();
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		        openGLErrorCheck();
			}
		}
	}

	@Override
	protected int getOpenGLTextureTarget() 
	{
		return GL_TEXTURE_2D;
	}

	@Override
	protected int getLevelCount() 
	{
		return this.levelCount;
	}
}
