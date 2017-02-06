package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL44.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

import tetzlaff.gl.ColorFormat.DataType;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.TextureWrapMode;
import tetzlaff.gl.builders.base.ColorTextureBuilderBase;
import tetzlaff.gl.builders.base.DepthStencilTextureBuilderBase;
import tetzlaff.gl.builders.base.DepthTextureBuilderBase;
import tetzlaff.gl.builders.base.StencilTextureBuilderBase;
import tetzlaff.helpers.ZipWrapper;

class OpenGLTexture3D extends OpenGLTexture implements Texture3D<OpenGLContext>
{
	private int textureTarget;
	private int mipmapCount;
	private int width;
	private int height;
	private int depth;
	private boolean useMipmaps;
	private boolean staleMipmaps;
	
	public static class OpenGLTexture3DColorBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture3D>
	{
		private int textureTarget;
		private int width;
		private int height;
		private int depth;
		
		OpenGLTexture3DColorBuilder(OpenGLContext context, int textureTarget, int width, int height, int depth)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.height = height;
			this.depth = depth;
		}
		
		@Override
		public OpenGLTexture3D createTexture() 
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
			
			return new OpenGLTexture3D(
					this.context,
					this.textureTarget, 
					this.getMultisamples(),
					colorFormat, 
					this.width,
					this.height,
					this.depth,
					(!this.isInternalFormatCompressed() && 
						(this.getInternalColorFormat().dataType == DataType.SIGNED_INTEGER || 
							this.getInternalColorFormat().dataType == DataType.UNSIGNED_INTEGER)) ? GL_RGBA_INTEGER : GL_RGBA,
					this.areMultisampleLocationsFixed(),
					this.isLinearFilteringEnabled(),
					this.areMipmapsEnabled(),
					this.getMaxAnisotropy());
		}
	}
	
	public static class OpenGLTexture3DDepthBuilder extends DepthTextureBuilderBase<OpenGLContext, OpenGLTexture3D>
	{
		private int textureTarget;
		private int width;
		private int height;
		private int depth;
		
		OpenGLTexture3DDepthBuilder(OpenGLContext context, int textureTarget, int width, int height, int depth)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.height = height;
			this.depth = depth;
		}
		
		@Override
		public OpenGLTexture3D createTexture() 
		{
			return new OpenGLTexture3D(
					this.context,
					this.textureTarget, 
					this.getMultisamples(),
					this.context.getOpenGLInternalDepthFormat(this.getInternalPrecision()), 
					this.width,
					this.height,
					this.depth,
					GL_DEPTH_COMPONENT,
					this.areMultisampleLocationsFixed(),
					this.isLinearFilteringEnabled(),
					this.areMipmapsEnabled(),
					this.getMaxAnisotropy());
		}
	}
	
	public static class OpenGLTexture3DStencilBuilder extends StencilTextureBuilderBase<OpenGLContext, OpenGLTexture3D>
	{
		private int textureTarget;
		private int width;
		private int height;
		private int depth;
		
		OpenGLTexture3DStencilBuilder(OpenGLContext context, int textureTarget, int width, int height, int depth)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.height = height;
			this.depth = depth;
		}
		
		@Override
		public OpenGLTexture3D createTexture() 
		{
			return new OpenGLTexture3D(
					this.context,
					this.textureTarget, 
					this.getMultisamples(),
					this.context.getOpenGLInternalStencilFormat(this.getInternalPrecision()), 
					this.width,
					this.height,
					this.depth,
					GL_STENCIL_INDEX,
					this.areMultisampleLocationsFixed(),
					this.isLinearFilteringEnabled(),
					this.areMipmapsEnabled(),
					this.getMaxAnisotropy());
		}
	}
	
	public static class OpenGLTexture3DDepthStencilBuilder extends DepthStencilTextureBuilderBase<OpenGLContext, OpenGLTexture3D>
	{
		private int textureTarget;
		private int width;
		private int height;
		private int depth;
		
		OpenGLTexture3DDepthStencilBuilder(OpenGLContext context, int textureTarget, int width, int height, int depth)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.height = height;
			this.depth = depth;
		}
		
		@Override
		public OpenGLTexture3D createTexture() 
		{
			return new OpenGLTexture3D(
					this.context,
					this.textureTarget, 
					this.getMultisamples(),
					this.isFloatingPointEnabled() ? GL_DEPTH32F_STENCIL8 : GL_DEPTH24_STENCIL8, 
					this.width,
					this.height,
					this.depth,
					GL_DEPTH_STENCIL,
					this.areMultisampleLocationsFixed(),
					this.isLinearFilteringEnabled(),
					this.areMipmapsEnabled(),
					this.getMaxAnisotropy());
		}
	}
	
	private OpenGLTexture3D(OpenGLContext context, int textureTarget, int multisamples, int internalFormat, int width, int height, int layerCount, int format, 
			boolean fixedSampleLocations, boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy) 
	{
		// Create and allocate a 3D texture or 2D texture array
		super(context);
		
		this.textureTarget = textureTarget;
		this.width = width;
		this.height = height;
		this.depth = layerCount;
		this.useMipmaps = useMipmaps;
		
		this.bind();
		
		if (textureTarget == GL_TEXTURE_2D && multisamples > 1)
		{
			glTexImage3DMultisample(this.getOpenGLTextureTarget(), multisamples, internalFormat, width, height, layerCount, fixedSampleLocations);
			this.context.openGLErrorCheck();
		}
		else
		{
			// Last four parameters are meaningless, but are subject to certain validation conditions
			glTexImage3D(this.getOpenGLTextureTarget(), 0, internalFormat, width, height, layerCount, 0, format, GL_UNSIGNED_BYTE, 0);
			this.context.openGLErrorCheck();
		}
		
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
			// No mipmaps
			this.mipmapCount = 1;
			
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
		
		glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        this.context.openGLErrorCheck();
        
		glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        this.context.openGLErrorCheck();
        
		glTexParameteri(textureTarget, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        this.context.openGLErrorCheck();
		
		if (maxAnisotropy > 1.0f)
		{
			glTexParameterf(textureTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
	        this.context.openGLErrorCheck();
		}
	}
	
	@Override
	public void loadLayer(int layerIndex, InputStream fileStream, boolean flipVertical) throws IOException
	{
		this.bind();
			
		BufferedImage img = ImageIO.read(fileStream);
		if(img == null) { throw new IOException("Error: Unsupported image format."); }
		
		if (layerIndex < 0 || layerIndex >= this.depth)
		{
			throw new IllegalArgumentException("The layer index specified (" + layerIndex + ") is out of bounds (layer count: " + this.depth + ").");
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

		glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
		this.context.openGLErrorCheck();
		
		glTexSubImage3D(this.getOpenGLTextureTarget(), 0, 0, 0, layerIndex, img.getWidth(), img.getHeight(), 1, GL_BGRA, GL_UNSIGNED_BYTE, buffer);
		this.context.openGLErrorCheck();
		
		if (this.useMipmaps)
		{
			this.staleMipmaps = true;
		}
	}

	@Override
	public void loadLayer(int layerIndex, ZipWrapper zipFile, boolean flipVertical) throws IOException
	{
		this.loadLayer(layerIndex, zipFile.getInputStream(), flipVertical);
	}

	@Override
	public void loadLayer(int layerIndex, File file, boolean flipVertical) throws IOException
	{
		this.loadLayer(layerIndex, new FileInputStream(file), flipVertical);
	}

	@Override
	public void loadLayer(int layerIndex, InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
	{
		this.bind();
			
		BufferedImage colorImg = ImageIO.read(imageStream);
		BufferedImage maskImg = ImageIO.read(maskStream);

		if(colorImg == null) { throw new IOException("Error: Unsupported image format for color image."); }
		if(maskImg == null) { throw new IOException("Error: Unsupported image format for mask image."); }
		
		if (layerIndex < 0 || layerIndex >= this.depth)
		{
			throw new IllegalArgumentException("The layer index specified (" + layerIndex + ") is out of bounds (layer count: " + this.depth + ").");
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

		glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
		this.context.openGLErrorCheck();
		
		glTexSubImage3D(this.getOpenGLTextureTarget(), 0, 0, 0, layerIndex, colorImg.getWidth(), colorImg.getHeight(), 1, GL_BGRA, GL_UNSIGNED_BYTE, buffer);
		this.context.openGLErrorCheck();
		
		if (this.useMipmaps)
		{
			this.staleMipmaps = true;
		}
	}

	@Override
	public void loadLayer(int layerIndex, ZipWrapper imageZip, ZipWrapper maskZip, boolean flipVertical) throws IOException
	{
		this.loadLayer(layerIndex, imageZip.getInputStream(), maskZip.getInputStream(), flipVertical);
	}

	@Override
	public void loadLayer(int layerIndex, File imageFile, File maskFile, boolean flipVertical) throws IOException
	{
		this.loadLayer(layerIndex, new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical);
	}

	@Override
	public void generateMipmaps()
	{
		// Create mipmaps
		glGenerateMipmap(this.getOpenGLTextureTarget());
        this.context.openGLErrorCheck();
        
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
	        this.context.openGLErrorCheck();
	        
	        this.staleMipmaps = false;
		}
	}

	@Override
	protected int getOpenGLTextureTarget() 
	{
		return this.textureTarget;
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

	@Override
	public int getDepth()
	{
		return this.depth;
	}
	
	@Override
	protected int getLevelCount() 
	{
		return this.mipmapCount;
	}

	@Override
	public OpenGLFramebufferAttachment getLayerAsFramebufferAttachment(int layerIndex)
	{
		final OpenGLContext context = this.context;
		final int textureId = this.getTextureId();
		return new OpenGLFramebufferAttachment()
		{
			@Override
			public OpenGLContext getContext()
			{
				return context;
			}
			
			@Override
			public void attachToDrawFramebuffer(int attachment, int level) 
			{
				glFramebufferTextureLayer(GL_DRAW_FRAMEBUFFER, attachment, textureId, level, layerIndex);
				context.openGLErrorCheck();
			}

			@Override
			public void attachToReadFramebuffer(int attachment, int level) 
			{
				glFramebufferTextureLayer(GL_READ_FRAMEBUFFER, attachment, textureId, level, layerIndex);
				context.openGLErrorCheck();
			}
			
		};
	}

	@Override
	public void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT, TextureWrapMode wrapR)
	{
		this.bind();
		switch(wrapS)
		{
		case None: 
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			this.context.openGLErrorCheck();
			break;
		case MirrorOnce:
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_MIRROR_CLAMP_TO_EDGE);
			this.context.openGLErrorCheck();
			break;
		case Repeat:
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_REPEAT);
			this.context.openGLErrorCheck();
			break;
		case MirroredRepeat:
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_MIRRORED_REPEAT);
			this.context.openGLErrorCheck();
			break;
		}
		switch(wrapT)
		{
		case None: 
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			this.context.openGLErrorCheck();
			break;
		case MirrorOnce:
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, GL_MIRROR_CLAMP_TO_EDGE);
			this.context.openGLErrorCheck();
			break;
		case Repeat:
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, GL_REPEAT);
			this.context.openGLErrorCheck();
			break;
		case MirroredRepeat:
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, GL_MIRRORED_REPEAT);
			this.context.openGLErrorCheck();
			break;
		}
		switch(wrapR)
		{
		case None: 
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
			this.context.openGLErrorCheck();
			break;
		case MirrorOnce:
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_R, GL_MIRROR_CLAMP_TO_EDGE);
			this.context.openGLErrorCheck();
			break;
		case Repeat:
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_R, GL_REPEAT);
			this.context.openGLErrorCheck();
			break;
		case MirroredRepeat:
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_R, GL_MIRRORED_REPEAT);
			this.context.openGLErrorCheck();
			break;
		}
	}
}
