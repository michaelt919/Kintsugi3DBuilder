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

public class OpenGLTexture2D extends OpenGLTexture
{
	private int width;
	private int height;
	private int levelCount;
	
	OpenGLTexture2D(int internalFormat, int width, int height, int format, int type, boolean useLinearFiltering, boolean useMipmaps) 
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		super();
		this.bind();
		this.width = width;
		this.height = height;
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, 0);
		openGLErrorCheck();
		this.init(width, height, useLinearFiltering, useMipmaps);
	}
	
	OpenGLTexture2D(int internalFormat, int width, int height, int format, int type) 
	{
		this(internalFormat, width, height, format, type, false, false);
	}
	
	private OpenGLTexture2D(ByteBuffer buffer, int internalFormat, int width, int height, int format, int type, boolean useLinearFiltering, boolean useMipmaps) 
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		super();
		this.bind();
		this.width = width;
		this.height = height;
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, buffer);
		openGLErrorCheck();
		this.init(width, height, useLinearFiltering, useMipmaps);
	}
	
	private OpenGLTexture2D(ByteBuffer buffer, int internalFormat, int width, int height, int format, int type) 
	{
		this(buffer, internalFormat, width, height, format, type, false, false);
	}
	
	public OpenGLTexture2D(InputStream fileStream, boolean flipVertical, boolean useLinearFiltering, boolean useMipmaps) throws IOException
	{
		super();
		this.bind();
		
		BufferedImage img = ImageIO.read(fileStream);
		this.width = img.getWidth();
		this.height = img.getHeight();
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
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, img.getWidth(), img.getHeight(), 0, GL_BGRA, GL_UNSIGNED_BYTE, buffer);
		openGLErrorCheck();
		this.init(img.getWidth(), img.getHeight(), useLinearFiltering, useMipmaps);
	}
	
	public OpenGLTexture2D(InputStream fileStream, boolean flipVertical) throws IOException
	{
		this(fileStream, flipVertical, false, false);
	}
	
	public OpenGLTexture2D(File file, boolean flipVertical, boolean useLinearFiltering, boolean useMipmaps) throws IOException
	{
		this(new FileInputStream(file), flipVertical, useLinearFiltering, useMipmaps);
	}
	
	public OpenGLTexture2D(File file, boolean flipVertical) throws IOException
	{
		this(file, flipVertical, false, false);
	}
	
	public OpenGLTexture2D(File file) throws IOException
	{
		this(file, false);
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
	
	public static OpenGLTexture2D createPerlinNoise()
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
		return new OpenGLTexture2D(pixels, GL_RGBA8, 256, 256, GL_RGBA, GL_UNSIGNED_BYTE);
	}

	// End of supporting code
	
	public int getWidth()
	{
		return this.width;
	}
	
	public int getHeight()
	{
		return this.height;
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
