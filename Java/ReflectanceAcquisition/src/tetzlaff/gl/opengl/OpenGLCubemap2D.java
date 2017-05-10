package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Cubemap2D;
import tetzlaff.gl.CubemapFace;
import tetzlaff.gl.FramebufferAttachment;
import tetzlaff.gl.TextureType;
import tetzlaff.gl.TextureWrapMode;
import tetzlaff.gl.ColorFormat.DataType;
import tetzlaff.gl.builders.base.ColorTextureBuilderBase;
import tetzlaff.gl.builders.base.DepthStencilTextureBuilderBase;
import tetzlaff.gl.builders.base.DepthTextureBuilderBase;
import tetzlaff.gl.builders.base.StencilTextureBuilderBase;
import tetzlaff.helpers.RadianceImageLoader;

public class OpenGLCubemap2D extends OpenGLTexture implements Cubemap2D<OpenGLContext>
{
	private int textureTarget;
	private int width;
	private int height;
	private int multisamples;
	private int levelCount;
	
//	static class OpenGLCubemap2DFromFileBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLCubemap2D>
//	{
//		private int textureTarget;
//		private BufferedImage colorImg;
//		private BufferedImage maskImg = null;
//		private boolean flipVertical;
//		
//		OpenGLCubemap2DFromFileBuilder(OpenGLContext context, int textureTarget, InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
//		{
//			super(context);
//			this.textureTarget = textureTarget;
//			this.colorImg = ImageIO.read(imageStream);
//			if (maskStream != null)
//			{
//				this.maskImg = ImageIO.read(maskStream);
//				if (maskImg.getWidth() != colorImg.getWidth() || maskImg.getHeight() != colorImg.getHeight())
//				{
//					throw new IllegalArgumentException("Color image and mask image must have the same dimensions.");
//				}
//			}
//			this.flipVertical = flipVertical;
//		}
//		
//		@Override
//		public OpenGLCubemap2D createTexture() 
//		{
//			int width = colorImg.getWidth();
//			int height = colorImg.getHeight();
//			ByteBuffer buffer = OpenGLTexture.bufferedImageToNativeBuffer(colorImg, maskImg, flipVertical);
//			
//			OptionalParameters opt = new OptionalParameters();
//			opt.useLinearFiltering = this.isLinearFilteringEnabled();
//			opt.useMipmaps = this.areMipmapsEnabled();
//			opt.maxAnisotropy = this.getMaxAnisotropy();
//			
//			if (this.isInternalFormatCompressed())
//			{
//				return new OpenGLCubemap2D(
//						this.context,
//						this.textureTarget, 
//						this.getInternalCompressionFormat(), 
//						width,
//						height,
//						GL_BGRA,
//						GL_UNSIGNED_BYTE,
//						buffer,
//						opt);
//			}
//			else
//			{
//				return new OpenGLCubemap2D(
//						this.context,
//						this.textureTarget, 
//						this.getInternalColorFormat(), 
//						width,
//						height,
//						GL_BGRA,
//						GL_UNSIGNED_BYTE,
//						buffer,
//						opt);
//			}
//		}
//	}
//	
//	static class OpenGLCubemap2DFromHDRFileBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLCubemap2D>
//	{
//		private int textureTarget;
//		private RadianceImageLoader.Image colorImg;
//		private BufferedImage maskImg = null;
//		
//		OpenGLCubemap2DFromHDRFileBuilder(OpenGLContext context, int textureTarget, BufferedInputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException
//		{
//			super(context);
//			this.textureTarget = textureTarget;
//			this.colorImg = new RadianceImageLoader().read(imageStream, flipVertical, true);
//			if (maskStream != null)
//			{
//				this.maskImg = ImageIO.read(maskStream);
//				if (maskImg.getWidth() != colorImg.width || maskImg.getHeight() != colorImg.height)
//				{
//					throw new IllegalArgumentException("Color image and mask image must have the same dimensions.");
//				}
//			}
//		}
//		
//		@Override
//		public OpenGLCubemap2D createTexture() 
//		{
//			int width = colorImg.width;
//			int height = colorImg.height;
//			
//			ByteBuffer buffer = OpenGLTexture.hdrImageToNativeBuffer(colorImg, maskImg);
//			
//			OptionalParameters opt = new OptionalParameters();
//			opt.useLinearFiltering = this.isLinearFilteringEnabled();
//			opt.useMipmaps = this.areMipmapsEnabled();
//			opt.maxAnisotropy = this.getMaxAnisotropy();
//			
//			if (this.isInternalFormatCompressed())
//			{
//				return new OpenGLCubemap2D(
//						this.context,
//						this.textureTarget, 
//						this.getInternalCompressionFormat(), 
//						width,
//						height,
//						maskImg == null ? GL_RGB : GL_RGBA,
//						GL_FLOAT,
//						buffer,
//						opt);
//			}
//			else
//			{
//				return new OpenGLCubemap2D(
//						this.context,
//						this.textureTarget, 
//						this.getInternalColorFormat(), 
//						width,
//						height,
//						maskImg == null ? GL_RGB : GL_RGBA,
//						GL_FLOAT,
//						buffer,
//						opt);
//			}
//		}
//	}
//	
//	static class OpenGLCubemap2DFromBufferBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLCubemap2D>
//	{
//		private int textureTarget;
//		private int width;
//		private int height;
//		private int format;
//		private int type;
//		private ByteBuffer buffer;
//		
//		OpenGLCubemap2DFromBufferBuilder(OpenGLContext context, int textureTarget, int width, int height, int format, int type, ByteBuffer buffer)
//		{
//			super(context);
//			this.textureTarget = textureTarget;
//			this.width = width;
//			this.height = height;
//			this.format = format;
//			this.type = type;
//			this.buffer = buffer;
//		}
//
//		@Override
//		public OpenGLCubemap2D createTexture() 
//		{
//			OptionalParameters opt = new OptionalParameters();
//			opt.useLinearFiltering = this.isLinearFilteringEnabled();
//			opt.useMipmaps = this.areMipmapsEnabled();
//			opt.maxAnisotropy = this.getMaxAnisotropy();
//			
//			if (this.isInternalFormatCompressed())
//			{
//				return new OpenGLCubemap2D(
//						this.context,
//						this.textureTarget, 
//						this.getInternalCompressionFormat(), 
//						this.width,
//						this.height,
//						this.format,
//						this.type,
//						this.buffer,
//						opt);
//			}
//			else
//			{
//				return new OpenGLCubemap2D(
//						this.context,
//						this.textureTarget, 
//						this.getInternalColorFormat(), 
//						this.width,
//						this.height,
//						this.format,
//						this.type,
//						this.buffer,
//						opt);
//			}
//		}
//	}
	
	static class OpenGLCubemap2DColorBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLCubemap2D>
	{
		private int textureTarget;
		private int width;
		private int height;
		
		OpenGLCubemap2DColorBuilder(OpenGLContext context, int textureTarget, int width, int height)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.height = height;
		}
		
		@Override
		public OpenGLCubemap2D createTexture()
		{
			OptionalParameters opt = new OptionalParameters();
			opt.useLinearFiltering = this.isLinearFilteringEnabled();
			opt.useMipmaps = this.areMipmapsEnabled();
			opt.maxAnisotropy = this.getMaxAnisotropy();
			
			if (this.isInternalFormatCompressed())
			{
				return new OpenGLCubemap2D(
						this.context,
						this.textureTarget, 
						this.getInternalCompressionFormat(), 
						this.width,
						this.height,
						(!this.isInternalFormatCompressed() && 
							(this.getInternalColorFormat().dataType == DataType.SIGNED_INTEGER || 
								this.getInternalColorFormat().dataType == DataType.UNSIGNED_INTEGER)) ? GL_RGBA_INTEGER : GL_RGBA,
						opt);
			}
			else
			{
				return new OpenGLCubemap2D(
						this.context,
						this.textureTarget, 
						this.getInternalColorFormat(), 
						this.width,
						this.height,
						(!this.isInternalFormatCompressed() && 
							(this.getInternalColorFormat().dataType == DataType.SIGNED_INTEGER || 
								this.getInternalColorFormat().dataType == DataType.UNSIGNED_INTEGER)) ? GL_RGBA_INTEGER : GL_RGBA,
						opt);
			}
		}
	}
	
	static class OpenGLCubemap2DDepthBuilder extends DepthTextureBuilderBase<OpenGLContext, OpenGLCubemap2D>
	{
		private int textureTarget;
		private int width;
		private int height;
		
		OpenGLCubemap2DDepthBuilder(OpenGLContext context, int textureTarget, int width, int height)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.height = height;
		}
		
		@Override
		public OpenGLCubemap2D createTexture() 
		{
			OptionalParameters opt = new OptionalParameters();
			opt.useLinearFiltering = this.isLinearFilteringEnabled();
			opt.useMipmaps = this.areMipmapsEnabled();
			opt.maxAnisotropy = this.getMaxAnisotropy();
			
			return new OpenGLCubemap2D(
					this.context,
					this.textureTarget, 
					TextureType.DEPTH,
					this.getInternalPrecision(), 
					this.width,
					this.height,
					GL_DEPTH_COMPONENT,
					opt);
		}
	}
	
	static class OpenGLCubemap2DStencilBuilder extends StencilTextureBuilderBase<OpenGLContext, OpenGLCubemap2D>
	{
		private int textureTarget;
		private int width;
		private int height;
		
		OpenGLCubemap2DStencilBuilder(OpenGLContext context, int textureTarget, int width, int height)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.height = height;
		}
		
		@Override
		public OpenGLCubemap2D createTexture() 
		{
			OptionalParameters opt = new OptionalParameters();
			opt.useLinearFiltering = this.isLinearFilteringEnabled();
			opt.useMipmaps = this.areMipmapsEnabled();
			opt.maxAnisotropy = this.getMaxAnisotropy();
			
			return new OpenGLCubemap2D(
					this.context,
					this.textureTarget, 
					TextureType.STENCIL,
					this.getInternalPrecision(), 
					this.width,
					this.height,
					GL_STENCIL_INDEX,
					opt);
		}
	}
	
	static class OpenGLCubemap2DDepthStencilBuilder extends DepthStencilTextureBuilderBase<OpenGLContext, OpenGLCubemap2D>
	{
		private int textureTarget;
		private int width;
		private int height;
		
		OpenGLCubemap2DDepthStencilBuilder(OpenGLContext context, int textureTarget, int width, int height)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.height = height;
		}
		
		@Override
		public OpenGLCubemap2D createTexture() 
		{
			OptionalParameters opt = new OptionalParameters();
			opt.useLinearFiltering = this.isLinearFilteringEnabled();
			opt.useMipmaps = this.areMipmapsEnabled();
			opt.maxAnisotropy = this.getMaxAnisotropy();
			
			return new OpenGLCubemap2D(
					this.context,
					this.textureTarget, 
					this.isFloatingPointEnabled() ? TextureType.FLOATING_POINT_DEPTH_STENCIL : TextureType.DEPTH_STENCIL,
					this.isFloatingPointEnabled() ? 40 : 32,
					this.width,
					this.height,
					GL_DEPTH_STENCIL,
					opt);
		}
	}
	
	private static class OptionalParameters
	{
		int type = GL_UNSIGNED_BYTE;
		
		ByteBuffer positiveX;
		ByteBuffer negativeX;
		ByteBuffer positiveY;
		ByteBuffer negativeY;
		ByteBuffer positiveZ;
		ByteBuffer negativeZ;
		
		boolean useLinearFiltering = false;
		boolean useMipmaps = false;
		float maxAnisotropy = 1.0f;
	}

	private OpenGLCubemap2D(OpenGLContext context, int textureTarget, ColorFormat colorFormat, int width, int height, int format, OptionalParameters opt) 
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		super(context, colorFormat);
		init(textureTarget, context.getOpenGLInternalColorFormat(colorFormat), width, height, format, opt);
	}
	
	private OpenGLCubemap2D(OpenGLContext context, int textureTarget, CompressionFormat compressionFormat, int width, int height, int format, OptionalParameters opt) 
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		super(context, compressionFormat);
		init(textureTarget, context.getOpenGLCompressionFormat(compressionFormat), width, height, format, opt);
	}
	
	private OpenGLCubemap2D(OpenGLContext context, int textureTarget, TextureType textureType, int precision, int width, int height, int format, OptionalParameters opt)
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		super(context, textureType);
		init(textureTarget, getSpecialInternalFormat(context, textureType, precision), width, height, format, opt);
	}
	
	private void init(int textureTarget, int internalFormat, int width, int height, int format, OptionalParameters opt)
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		this.textureTarget = textureTarget;
		this.bind();
		this.width = width;
		this.height = height;
		
		glPixelStorei(GL_UNPACK_ALIGNMENT, getUnpackAlignment(format, opt.type));
		this.context.openGLErrorCheck();
		
		if (opt.positiveX == null)
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, internalFormat, width, height, 0, format, opt.type, 0);
			this.context.openGLErrorCheck();
		}
		else
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, internalFormat, width, height, 0, format, opt.type, opt.positiveX);
			this.context.openGLErrorCheck();
		}
		
		if (opt.negativeX == null)
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, internalFormat, width, height, 0, format, opt.type, 0);
			this.context.openGLErrorCheck();
		}
		else
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, internalFormat, width, height, 0, format, opt.type, opt.negativeX);
			this.context.openGLErrorCheck();
		}
		
		if (opt.positiveY == null)
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, internalFormat, width, height, 0, format, opt.type, 0);
			this.context.openGLErrorCheck();
		}
		else
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, internalFormat, width, height, 0, format, opt.type, opt.positiveY);
			this.context.openGLErrorCheck();
		}
		
		if (opt.negativeY == null)
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, internalFormat, width, height, 0, format, opt.type, 0);
			this.context.openGLErrorCheck();
		}
		else
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, internalFormat, width, height, 0, format, opt.type, opt.negativeY);
			this.context.openGLErrorCheck();
		}

		if (opt.positiveZ == null)
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, internalFormat, width, height, 0, format, opt.type, 0);
			this.context.openGLErrorCheck();
		}
		else
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, internalFormat, width, height, 0, format, opt.type, opt.positiveZ);
			this.context.openGLErrorCheck();
		}
		
		if (opt.negativeZ == null)
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, internalFormat, width, height, 0, format, opt.type, opt.negativeZ);
			this.context.openGLErrorCheck();
		}
		
		this.initFilteringAndMipmaps(opt.useLinearFiltering, opt.useMipmaps);
		
		if (opt.maxAnisotropy > 1.0f)
		{
			glTexParameterf(textureTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, opt.maxAnisotropy);
	        this.context.openGLErrorCheck();
		}
	}
	
	void initFilteringAndMipmaps(boolean useLinearFiltering, boolean useMipmaps)
	{
		super.initFilteringAndMipmaps(useLinearFiltering, useMipmaps);
		
		if (useMipmaps)
		{
	        // Calculate the number of mipmap levels
			this.levelCount = 0;
			int dim = Math.max(width, height);
			while (dim > 1)
			{
				this.levelCount++;
				dim /= 2;
			}
		}
		else
		{
			// No mipmaps
			this.levelCount = 1;
		}
		
		glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        this.context.openGLErrorCheck();
        
		glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        this.context.openGLErrorCheck();
        
        glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
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
	public int getMipmapLevelCount() 
	{
		return this.levelCount;
	}

	@Override
	public void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT)
	{
		this.bind();
		int numericWrapS = translateWrapMode(wrapS);
		int numericWrapT = translateWrapMode(wrapT);
		
		if (numericWrapS != 0)
		{
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, numericWrapS);
			this.context.openGLErrorCheck();
		}
		
		if (numericWrapT != 0)
		{
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, numericWrapT);
			this.context.openGLErrorCheck();
		}
	}

	@Override
	public FramebufferAttachment<OpenGLContext> getFaceAsFramebufferAttachment(CubemapFace face) 
	{
		final OpenGLContext context = this.context;
		final int textureId = this.getTextureId();
		
		final int layerIndex;
		
		switch(face)
		{
		case PositiveX: layerIndex = 0; break;
		case NegativeX: layerIndex = 1; break;
		case PositiveY: layerIndex = 2; break;
		case NegativeY: layerIndex = 3; break;
		case PositiveZ: layerIndex = 4; break;
		case NegativeZ: layerIndex = 5; break;
		default: layerIndex = -1; break; // Should never happen
		}
		
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
}
