package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

public class OpenGLTexture2D extends OpenGLTexture
{
	private int levelCount;
	
	OpenGLTexture2D(int internalFormat, int width, int height, int format, boolean useLinearFiltering, boolean useMipmaps) 
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		super();
		this.bind();
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL_UNSIGNED_BYTE, 0);
		openGLErrorCheck();
		this.init(width, height, useLinearFiltering, useMipmaps);
	}
	
	OpenGLTexture2D(int internalFormat, int width, int height, int format) 
	{
		this(internalFormat, width, height, format, false, false);
	}
	
	public OpenGLTexture2D(InputStream fileStream, boolean useLinearFiltering, boolean useMipmaps) throws IOException
	{
		super();
		this.bind();
		
		BufferedImage img = ImageIO.read(fileStream);
		ByteBuffer buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);
		for (int y = 0; y < img.getHeight(); y++)
		{
			for (int x = 0; x < img.getWidth(); x++)
			{
				byte a = (byte)((img.getRGB(x, y) >>> 16) & 0xFF);
				byte r = (byte)((img.getRGB(x, y) >>> 16) & 0xFF);
				byte g = (byte)((img.getRGB(x, y) >>> 8) & 0xFF);
				byte b = (byte)(img.getRGB(x, y) & 0xFF);
				
				buffer.put(r);
				buffer.put(g);
				buffer.put(b);
				buffer.put(a);
			}
		}
		buffer.flip();
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, img.getWidth(), img.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
		openGLErrorCheck();
		this.init(img.getWidth(), img.getHeight(), useLinearFiltering, useMipmaps);
	}
	
	public OpenGLTexture2D(InputStream fileStream) throws IOException
	{
		this(fileStream, false, false);
	}
	
	public OpenGLTexture2D(String filename, boolean useLinearFiltering, boolean useMipmaps) throws IOException
	{
		this(new FileInputStream(filename), useLinearFiltering, useMipmaps);
	}
	
	public OpenGLTexture2D(String filename) throws IOException
	{
		this(filename, false, false);
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
