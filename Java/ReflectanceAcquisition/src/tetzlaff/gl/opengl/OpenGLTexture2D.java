package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;
// mipmaps
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

import tetzlaff.gl.ColorFormat.DataType;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.builders.base.ColorTextureBuilderBase;
import tetzlaff.gl.builders.base.DepthStencilTextureBuilderBase;
import tetzlaff.gl.builders.base.DepthTextureBuilderBase;
import tetzlaff.gl.builders.base.StencilTextureBuilderBase;

class OpenGLTexture2D extends OpenGLTexture implements Texture2D<OpenGLContext>
{
	private int textureTarget;
	private int width;
	private int height;
	private int multisamples;
	private int levelCount;
	
	static class OpenGLTexture2DFromFileBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
	{
		private int textureTarget;
		private BufferedImage colorImg;
		private BufferedImage maskImg = null;
		private boolean flipVertical;
		
		OpenGLTexture2DFromFileBuilder(OpenGLContext context, int textureTarget, InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
		{
			super(context);
			this.textureTarget = textureTarget;
			this.colorImg = ImageIO.read(imageStream);
			if (maskStream != null)
			{
				this.maskImg = ImageIO.read(maskStream);
				if (maskImg.getWidth() != colorImg.getWidth() || maskImg.getHeight() != maskImg.getHeight())
				{
					throw new IllegalArgumentException("Color image and mask image must have the same dimensions.");
				}
			}
			this.flipVertical = flipVertical;
		}
		
		@Override
		public OpenGLTexture2D createTexture() 
		{
			int width = colorImg.getWidth();
			int height = colorImg.getHeight();
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
			
			int colorFormat;
			if (this.isInternalFormatCompressed())
			{
				colorFormat = this.context.getOpenGLCompressionFormat(this.getInternalCompressionFormat());
			}
			else
			{
				colorFormat = this.context.getOpenGLInternalColorFormat(this.getInternalColorFormat());
			}
			
			return new OpenGLTexture2D(
					this.context,
					this.textureTarget, 
					colorFormat, 
					width,
					height,
					GL_BGRA,
					GL_UNSIGNED_BYTE,
					buffer,
					this.isLinearFilteringEnabled(),
					this.areMipmapsEnabled(),
					this.getMaxAnisotropy());
		}
	}
	
	static class OpenGLTexture2DFromBufferBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
	{
		private int textureTarget;
		private int width;
		private int height;
		private int format;
		private int type;
		private ByteBuffer buffer;
		
		OpenGLTexture2DFromBufferBuilder(OpenGLContext context, int textureTarget, int width, int height, int format, int type, ByteBuffer buffer)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.height = height;
			this.format = format;
			this.type = type;
			this.buffer = buffer;
		}

		@Override
		public OpenGLTexture2D createTexture() 
		{
			int colorFormat;
			if (this.isInternalFormatCompressed())
			{
				colorFormat = this.context.getOpenGLCompressionFormat(this.getInternalCompressionFormat());
			}
			else
			{
				colorFormat = this.context.getOpenGLInternalColorFormat(this.getInternalColorFormat());
			}
			
			return new OpenGLTexture2D(
					this.context,
					this.textureTarget, 
					colorFormat, 
					this.width,
					this.height,
					this.format,
					this.type,
					this.buffer,
					this.isLinearFilteringEnabled(),
					this.areMipmapsEnabled(),
					this.getMaxAnisotropy());
		}
	}
	
	static class OpenGLTexture2DColorBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
	{
		private int textureTarget;
		private int width;
		private int height;
		
		OpenGLTexture2DColorBuilder(OpenGLContext context, int textureTarget, int width, int height)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.height = height;
		}
		
		@Override
		public OpenGLTexture2D createTexture() 
		{
			int colorFormat;
			if (this.isInternalFormatCompressed())
			{
				colorFormat = this.context.getOpenGLCompressionFormat(this.getInternalCompressionFormat());
			}
			else
			{
				colorFormat = this.context.getOpenGLInternalColorFormat(this.getInternalColorFormat());
			}
			
			return new OpenGLTexture2D(
					this.context,
					this.textureTarget, 
					this.getMultisamples(),
					colorFormat, 
					this.width,
					this.height,
					(!this.isInternalFormatCompressed() && 
						(this.getInternalColorFormat().dataType == DataType.SIGNED_INTEGER || 
							this.getInternalColorFormat().dataType == DataType.UNSIGNED_INTEGER)) ? GL_RGBA_INTEGER : GL_RGBA,
					this.areMultisampleLocationsFixed(),
					this.isLinearFilteringEnabled(),
					this.areMipmapsEnabled(),
					this.getMaxAnisotropy());
		}
	}
	
	static class OpenGLTexture2DDepthBuilder extends DepthTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
	{
		private int textureTarget;
		private int width;
		private int height;
		
		OpenGLTexture2DDepthBuilder(OpenGLContext context, int textureTarget, int width, int height)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.height = height;
		}
		
		@Override
		public OpenGLTexture2D createTexture() 
		{
			return new OpenGLTexture2D(
					this.context,
					this.textureTarget, 
					this.getMultisamples(),
					this.context.getOpenGLInternalDepthFormat(this.getInternalPrecision()), 
					this.width,
					this.height,
					GL_DEPTH_COMPONENT,
					this.areMultisampleLocationsFixed(),
					this.isLinearFilteringEnabled(),
					this.areMipmapsEnabled(),
					this.getMaxAnisotropy());
		}
	}
	
	static class OpenGLTexture2DStencilBuilder extends StencilTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
	{
		private int textureTarget;
		private int width;
		private int height;
		
		OpenGLTexture2DStencilBuilder(OpenGLContext context, int textureTarget, int width, int height)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.height = height;
		}
		
		@Override
		public OpenGLTexture2D createTexture() 
		{
			return new OpenGLTexture2D(
					this.context,
					this.textureTarget, 
					this.getMultisamples(),
					this.context.getOpenGLInternalStencilFormat(this.getInternalPrecision()), 
					this.width,
					this.height,
					GL_STENCIL_INDEX,
					this.areMultisampleLocationsFixed(),
					this.isLinearFilteringEnabled(),
					this.areMipmapsEnabled(),
					this.getMaxAnisotropy());
		}
	}
	
	static class OpenGLTexture2DDepthStencilBuilder extends DepthStencilTextureBuilderBase<OpenGLContext, OpenGLTexture2D>
	{
		private int textureTarget;
		private int width;
		private int height;
		
		OpenGLTexture2DDepthStencilBuilder(OpenGLContext context, int textureTarget, int width, int height)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.height = height;
		}
		
		@Override
		public OpenGLTexture2D createTexture() 
		{
			return new OpenGLTexture2D(
					this.context,
					this.textureTarget, 
					this.getMultisamples(),
					this.isFloatingPointEnabled() ? GL_DEPTH32F_STENCIL8 : GL_DEPTH24_STENCIL8, 
					this.width,
					this.height,
					GL_DEPTH_STENCIL,
					this.areMultisampleLocationsFixed(),
					this.isLinearFilteringEnabled(),
					this.areMipmapsEnabled(),
					this.getMaxAnisotropy());
		}
	}
	
	private OpenGLTexture2D(OpenGLContext context, int textureTarget, int multisamples, int internalFormat, int width, int height, int format, 
			boolean fixedMultisampleLocations, boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy)
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		super(context);
		this.textureTarget = textureTarget;
		this.bind();
		this.width = width;
		this.height = height;
		if (textureTarget == GL_TEXTURE_2D && multisamples > 1)
		{
			this.textureTarget = GL_TEXTURE_2D_MULTISAMPLE;
			useLinearFiltering = false; // linear filtering not allowed with multisampling
			useMipmaps = false; // mipmaps not allowed with multisampling
			this.levelCount = 1;
			glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, multisamples, internalFormat, width, height, fixedMultisampleLocations);
			this.context.openGLErrorCheck();
			// TODO: multisample textures don't seem to work correctly
		}
		else
		{
			// Last four parameters are essentially meaningless, but are subject to certain validation conditions
			glTexImage2D(textureTarget, 0, internalFormat, width, height, 0, format, GL_UNSIGNED_BYTE, 0);
			this.context.openGLErrorCheck();
			this.init(useLinearFiltering, useMipmaps);
		}
		
		if (maxAnisotropy > 1.0f)
		{
			glTexParameterf(textureTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
	        this.context.openGLErrorCheck();
		}
	}
	
	private OpenGLTexture2D(OpenGLContext context, int textureTarget, int internalFormat, int width, int height, int format, int type, ByteBuffer buffer, 
			boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy) 
	{
		super(context);
		this.textureTarget = textureTarget;
		this.bind();
		this.width = width;
		this.height = height;
		glTexImage2D(textureTarget, 0, internalFormat, width, height, 0, format, type, buffer);
		this.context.openGLErrorCheck();
		this.init(useLinearFiltering, useMipmaps);
		
		if (maxAnisotropy > 1.0f)
		{
			glTexParameterf(textureTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
	        this.context.openGLErrorCheck();
		}
	}
	
	private void init(boolean useLinearFiltering, boolean useMipmaps)
	{
		if (useMipmaps)
		{
			// Create mipmaps
			glGenerateMipmap(this.textureTarget);
	        this.context.openGLErrorCheck();
	        
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
				glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		        this.context.openGLErrorCheck();
				glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		        this.context.openGLErrorCheck();
			}
			else
			{
				glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
		        this.context.openGLErrorCheck();
				glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		        this.context.openGLErrorCheck();
			}
		}
		else
		{
			// No mipmaps
			this.levelCount = 1;
			
			if (useLinearFiltering)
			{
				glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		        this.context.openGLErrorCheck();
				glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		        this.context.openGLErrorCheck();
			}
			else
			{
				glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		        this.context.openGLErrorCheck();
				glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		        this.context.openGLErrorCheck();
			}
		}
		
		glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        this.context.openGLErrorCheck();
        
		glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        this.context.openGLErrorCheck();
	}
	
	@Override
	public int getWidth()
	{
		return this.width;
	}
	
	@Override
	public int getHeight()
	{
		return this.height;
	}
	
	public int getMultisamples()
	{
		return this.multisamples;
	}

	@Override
	protected int getOpenGLTextureTarget() 
	{
		return this.textureTarget;
	}

	@Override
	protected int getLevelCount() 
	{
		return this.levelCount;
	}
}
