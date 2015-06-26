package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

import tetzlaff.helpers.ZipWrapper;

public abstract class OpenGLLayeredTexture extends OpenGLTexture
{
	private int layerCount;
	private int mipmapCount;
	private int width;
	private int height;
	private boolean useMipmaps;
	private boolean staleMipmaps;
	
	protected OpenGLLayeredTexture(int internalFormat, int width, int height, int layerCount, int format, int type, boolean useLinearFiltering, boolean useMipmaps) 
	{
		// Create and allocate a 3D texture or 2D texture array
		super();
		
		this.width = width;
		this.height = height;
		this.layerCount = layerCount;
		this.useMipmaps = useMipmaps;
		
		this.bind();
		
		glTexImage3D(this.getOpenGLTextureTarget(), 0, internalFormat, width, height, layerCount, 0, format, type, 0);
		openGLErrorCheck();
		
		this.init(useLinearFiltering, useMipmaps);
	}
	
	protected OpenGLLayeredTexture(int internalFormat, int width, int height, int layerCount, int format, boolean useLinearFiltering, boolean useMipmaps) 
	{
		this(internalFormat, width, height, layerCount, format, GL_UNSIGNED_BYTE, useLinearFiltering, useMipmaps);
	}
	
	protected OpenGLLayeredTexture(int internalFormat, int width, int height, int layerCount, int format) 
	{
		this(internalFormat, width, height, layerCount, format, false, false);
	}

	protected OpenGLLayeredTexture(int width, int height, int layerCount, boolean useLinearFiltering, boolean useMipmaps) 
	{
		// Constructor for a texture that will be loaded from files by layer
		this(GL_RGBA8, width, height, layerCount, GL_BGRA, useLinearFiltering, useMipmaps);
	}
	
	protected OpenGLLayeredTexture(int width, int height, int layerCount)
	{
		this(width, height, layerCount, false, false);
	}
	
	public int getWidth()
	{
		return this.width;
	}
	
	public int getHeight()
	{
		return this.height;
	}
	
	public void loadLayer(int layerIndex, InputStream fileStream, boolean flipVertical) throws IOException
	{
		this.bind();
			
		BufferedImage img = ImageIO.read(fileStream);
		
		if (layerIndex < 0 || layerIndex >= this.layerCount)
		{
			throw new IllegalArgumentException("The layer index specified (" + layerIndex + ") is out of bounds (layer count: " + this.layerCount + ").");
		}
		else if (img.getWidth() != this.width || img.getHeight() != this.height)
		{
			throw new IllegalStateException("The texture to be loaded does not have the correct width and height.");
		}
		else
		{
			this.width = img.getWidth();
			this.height = img.getHeight();
		}
		
		ByteBuffer buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);
		IntBuffer intBuffer = buffer.asIntBuffer();
		if (flipVertical)
		{
			for (int y = img.getHeight() - 1; y >= 0; y--)
			{
				for (int x = 0; x < img.getWidth(); x++)
				{
					intBuffer.put(img.getRGB(x, y));
				}
			}
		}
		else
		{
			for (int y = 0; y < img.getHeight(); y++)
			{
				for (int x = 0; x < img.getWidth(); x++)
				{
					intBuffer.put(img.getRGB(x, y));
				}
			}
		}
		
		glTexSubImage3D(this.getOpenGLTextureTarget(), 0, 0, 0, layerIndex, img.getWidth(), img.getHeight(), 1, GL_BGRA, GL_UNSIGNED_BYTE, buffer);
		openGLErrorCheck();
		
		if (this.useMipmaps)
		{
			this.staleMipmaps = true;
		}
	}
	
	public void loadLayer(int layerIndex, ZipWrapper zipFile, boolean flipVertical) throws IOException
	{
		this.loadLayer(layerIndex, zipFile.getInputStream(), flipVertical);
	}

	public void loadLayer(int layerIndex, File file, boolean flipVertical) throws IOException
	{
		this.loadLayer(layerIndex, new FileInputStream(file), flipVertical);
	}
	
	public void loadLayer(int layerIndex, InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
	{
		this.bind();
			
		BufferedImage colorImg = ImageIO.read(imageStream);
		BufferedImage maskImg = ImageIO.read(maskStream);
		
		if (layerIndex < 0 || layerIndex >= this.layerCount)
		{
			throw new IllegalArgumentException("The layer index specified (" + layerIndex + ") is out of bounds (layer count: " + this.layerCount + ").");
		}
		else if (colorImg.getWidth() != this.width || colorImg.getHeight() != this.height)
		{
			throw new IllegalStateException("The texture to be loaded does not have the correct width and height.");
		}
		else if (maskImg.getWidth() != this.width || maskImg.getHeight() != this.height)
		{
			throw new IllegalStateException("The alpha mask to be loaded does not have the correct width and height.");
		}
		else
		{
			this.width = colorImg.getWidth();
			this.height = colorImg.getHeight();
		}
		
		ByteBuffer buffer = BufferUtils.createByteBuffer(colorImg.getWidth() * colorImg.getHeight() * 4);
		IntBuffer intBuffer = buffer.asIntBuffer();
		if (flipVertical)
		{
			for (int y = colorImg.getHeight() - 1; y >= 0; y--)
			{
				for (int x = 0; x < colorImg.getWidth(); x++)
				{
					// Use green channel of the mask image for alpha
					intBuffer.put((colorImg.getRGB(x, y) & 0x00ffffff) | ((maskImg.getRGB(x, y) & 0x0000ff00) << 16));
				}
			}
		}
		else
		{
			for (int y = 0; y < colorImg.getHeight(); y++)
			{
				for (int x = 0; x < colorImg.getWidth(); x++)
				{
					// Use green channel of the mask image for alpha
					intBuffer.put((colorImg.getRGB(x, y) & 0x00ffffff) | ((maskImg.getRGB(x, y) & 0x0000ff00) << 16));
				}
			}
		}
		
		glTexSubImage3D(this.getOpenGLTextureTarget(), 0, 0, 0, layerIndex, colorImg.getWidth(), colorImg.getHeight(), 1, GL_BGRA, GL_UNSIGNED_BYTE, buffer);
		openGLErrorCheck();
		
		if (this.useMipmaps)
		{
			this.staleMipmaps = true;
		}
	}
	
	public void loadLayer(int layerIndex, ZipWrapper imageZip, ZipWrapper maskZip, boolean flipVertical) throws IOException
	{
		this.loadLayer(layerIndex, imageZip.getInputStream(), maskZip.getInputStream(), flipVertical);
	}

	public void loadLayer(int layerIndex, File imageFile, File maskFile, boolean flipVertical) throws IOException
	{
		this.loadLayer(layerIndex, new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical);
	}
	
	public void generateMipmaps()
	{
		// Create mipmaps
		glGenerateMipmap(this.getOpenGLTextureTarget());
        openGLErrorCheck();
        
        this.staleMipmaps = false;
	}
	
	@Override
	void bindToTextureUnit(int textureUnitIndex)
	{
		super.bindToTextureUnit(textureUnitIndex);
		
		if(this.staleMipmaps)
		{
			// Create mipmaps
			glGenerateMipmap(this.getOpenGLTextureTarget());
	        openGLErrorCheck();
	        
	        this.staleMipmaps = false;
		}
	}
	
	private void init(boolean useLinearFiltering, boolean useMipmaps)
	{
		if (useMipmaps)
		{
			// Calculate the number of mipmap levels
			this.mipmapCount = 0;
			int dim = Math.max(this.width, this.height);
			while (dim > 0)
			{
				this.mipmapCount++;
				dim /= 2;
			}
			
			if (useLinearFiltering)
			{
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		        openGLErrorCheck();
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		        openGLErrorCheck();
			}
			else
			{
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
		        openGLErrorCheck();
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		        openGLErrorCheck();
			}
		}
		else
		{
			// No mipmaps
			this.mipmapCount = 1;
			
			if (useLinearFiltering)
			{
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		        openGLErrorCheck();
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		        openGLErrorCheck();
			}
			else
			{
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		        openGLErrorCheck();
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		        openGLErrorCheck();
			}
		}
	}

	@Override
	protected int getLevelCount() 
	{
		return this.mipmapCount;
	}
	
	public OpenGLFramebufferAttachment getLayerAsFramebufferAttachment(int layerIndex)
	{
		final int textureId = this.getTextureId();
		return new OpenGLFramebufferAttachment()
		{
			@Override
			public void attachToDrawFramebuffer(int attachment, int level) 
			{
				glFramebufferTextureLayer(GL_DRAW_FRAMEBUFFER, attachment, textureId, level, layerIndex);
				openGLErrorCheck();
			}

			@Override
			public void attachToReadFramebuffer(int attachment, int level) 
			{
				glFramebufferTextureLayer(GL_READ_FRAMEBUFFER, attachment, textureId, level, layerIndex);
				openGLErrorCheck();
			}
			
		};
	}
}
