package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL44.*;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;
import tetzlaff.gl.TextureType;
import tetzlaff.gl.TextureWrapMode;
import tetzlaff.helpers.RadianceImageLoader;

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
	
	void initFilteringAndMipmaps(boolean useLinearFiltering, boolean useMipmaps)
	{
		if (useMipmaps)
		{
			// Create mipmaps
			glGenerateMipmap(this.getOpenGLTextureTarget());
	        this.context.openGLErrorCheck();
			
			if (useLinearFiltering)
			{
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		        this.context.openGLErrorCheck();
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		        this.context.openGLErrorCheck();
			}
			else
			{
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
		        this.context.openGLErrorCheck();
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		        this.context.openGLErrorCheck();
			}
		}
		else
		{
			if (useLinearFiltering)
			{
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		        this.context.openGLErrorCheck();
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		        this.context.openGLErrorCheck();
			}
			else
			{
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		        this.context.openGLErrorCheck();
				glTexParameteri(this.getOpenGLTextureTarget(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		        this.context.openGLErrorCheck();
			}
		}
	}
	
	static int getUnpackAlignment(int format, int dataType)
	{
		if (dataType == GL_UNSIGNED_SHORT_5_6_5 || dataType == GL_UNSIGNED_SHORT_5_6_5_REV || dataType == GL_UNSIGNED_SHORT_4_4_4_4 ||
				dataType == GL_UNSIGNED_SHORT_4_4_4_4_REV || dataType == GL_UNSIGNED_SHORT_5_5_5_1 || dataType == GL_UNSIGNED_SHORT_1_5_5_5_REV)
		{
			return 2;
		}
		else if (format == GL_RGBA || format == GL_BGRA || format == GL_RGBA_INTEGER || format == GL_RGBA_INTEGER || dataType == GL_UNSIGNED_INT || dataType == GL_INT || dataType == GL_FLOAT ||
				dataType == GL_UNSIGNED_INT_8_8_8_8 || dataType == GL_UNSIGNED_INT_8_8_8_8_REV || dataType == GL_UNSIGNED_INT_10_10_10_2 || dataType == GL_UNSIGNED_INT_2_10_10_10_REV)
		{
			return 4;
		}
		else if (dataType == GL_UNSIGNED_SHORT || dataType == GL_SHORT)
		{
			return 2;
		}
		else
		{
			return 1;
		}
	}
	
	static int getSpecialInternalFormat(OpenGLContext context, TextureType textureType, int precision)
	{
		switch(textureType)
		{
		case DEPTH: 
			return context.getOpenGLInternalDepthFormat(precision);
		case STENCIL:
			return context.getOpenGLInternalStencilFormat(precision);
		case DEPTH_STENCIL:
			return GL_DEPTH24_STENCIL8;
		case FLOATING_POINT_DEPTH_STENCIL:
			return GL_DEPTH32F_STENCIL8;
		case COLOR:
		default:
			return context.getOpenGLInternalColorFormat(
				new ColorFormat(precision, precision, precision, precision, ColorFormat.DataType.NORMALIZED_FIXED_POINT));
		}
	}
	
	static int translateWrapMode(TextureWrapMode wrapMode)
	{
		switch(wrapMode)
		{
		case None: return GL_CLAMP_TO_EDGE;
		case MirrorOnce: return GL_MIRROR_CLAMP_TO_EDGE;
		case Repeat: return GL_REPEAT;
		case MirroredRepeat: return GL_MIRRORED_REPEAT;
		default: return 0;
		}
	}
	
	static ByteBuffer bufferedImageToNativeBuffer(BufferedImage colorImg, BufferedImage maskImg, boolean flipVertical)
	{
		ByteBuffer buffer = BufferUtils.createByteBuffer(colorImg.getWidth() * colorImg.getHeight() * 4);
		IntBuffer intBuffer = buffer.asIntBuffer();
		
		if (maskImg == null)
		{
			if (flipVertical)
			{
				for (int y = colorImg.getHeight() - 1; y >= 0; y--)
				{
					for (int x = 0; x < colorImg.getWidth(); x++)
					{
						intBuffer.put(colorImg.getRGB(x, y));
					}
				}
			}
			else
			{
				for (int y = 0; y < colorImg.getHeight(); y++)
				{
					for (int x = 0; x < colorImg.getWidth(); x++)
					{
						intBuffer.put(colorImg.getRGB(x, y));
					}
				}
			}
		}
		else
		{
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
		}
		
		return buffer;
	}
	
	static ByteBuffer hdrImageToNativeBuffer(RadianceImageLoader.Image colorImg, BufferedImage maskImg)
	{
		ByteBuffer buffer = BufferUtils.createByteBuffer(colorImg.width * colorImg.height * (maskImg == null ? 12 : 16));
		FloatBuffer floatBuffer = buffer.asFloatBuffer();

		int k = 0;
		if (maskImg == null)
		{
			for (int y = 0; y < colorImg.height; y++)
			{
				for (int x = 0; x < colorImg.width; x++)
				{
					floatBuffer.put(colorImg.data[k++]);
					floatBuffer.put(colorImg.data[k++]);
					floatBuffer.put(colorImg.data[k++]);
				}
			}
		}
		else
		{
			for (int y = 0; y < colorImg.height; y++)
			{
				for (int x = 0; x < colorImg.width; x++)
				{
					floatBuffer.put(colorImg.data[k++]);
					floatBuffer.put(colorImg.data[k++]);
					floatBuffer.put(colorImg.data[k++]);
					
					// Use green channel of the mask image for alpha
					floatBuffer.put((float)((maskImg.getRGB(x, y) & 0x0000ff00) >>> 8) / 255.0f);
				}
			}
		}
		
		return buffer;
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
