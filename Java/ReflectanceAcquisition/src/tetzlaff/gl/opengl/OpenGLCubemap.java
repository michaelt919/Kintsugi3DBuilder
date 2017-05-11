package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Cubemap;
import tetzlaff.gl.CubemapFace;
import tetzlaff.gl.FramebufferAttachment;
import tetzlaff.gl.TextureType;
import tetzlaff.gl.TextureWrapMode;
import tetzlaff.gl.builders.base.ColorCubemapBuilderBase;
import tetzlaff.gl.builders.base.DepthStencilTextureBuilderBase;
import tetzlaff.gl.builders.base.DepthTextureBuilderBase;
import tetzlaff.gl.builders.base.StencilTextureBuilderBase;
import tetzlaff.gl.helpers.FloatVertexList;

public class OpenGLCubemap extends OpenGLTexture implements Cubemap<OpenGLContext>
{
	private int textureTarget;
	private int faceSize;
	private int multisamples;
	private int levelCount;
	
	// TODO expand this to support more formats than just RGB floats
	static class ColorBuilder extends ColorCubemapBuilderBase<OpenGLContext, OpenGLCubemap>
	{	
		private int textureTarget;
		private int faceSize;
		
		private final ByteBuffer[] faces = new ByteBuffer[6];
		
		private int cubemapFaceToIndex(CubemapFace face)
		{
			switch(face)
			{
			case PositiveX: return 0;
			case NegativeX: return 1;
			case PositiveY: return 2;
			case NegativeY: return 3;
			case PositiveZ: return 4;
			case NegativeZ: return 5;
			default: return -1; // Should never happen
			}
		}
		
		ColorBuilder(OpenGLContext context, int textureTarget, int faceSize)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.faceSize = faceSize;
		}

		@Override
		public ColorBuilder loadFace(CubemapFace face, FloatVertexList data)
				throws IOException 
		{
			if (data.dimensions != 3)
			{
				throw new UnsupportedOperationException("Only RGB data is currently supported for cubemaps.");
			}
			else
			{
				faces[cubemapFaceToIndex(face)] = BufferUtils.createByteBuffer(data.count * data.dimensions * 4);
				faces[cubemapFaceToIndex(face)].asFloatBuffer().put(data.getBuffer());
				return this;
			}
		}
		
		@Override
		public OpenGLCubemap createTexture()
		{
			OptionalParameters opt = new OptionalParameters();
			opt.useLinearFiltering = this.isLinearFilteringEnabled();
			opt.useMipmaps = this.areMipmapsEnabled();
			opt.maxAnisotropy = this.getMaxAnisotropy();
			opt.dataType = GL_FLOAT;
			opt.positiveX = faces[0];
			opt.negativeX = faces[1];
			opt.positiveY = faces[2];
			opt.negativeY = faces[3];
			opt.positiveZ = faces[4];
			opt.negativeZ = faces[5];
			
			if (this.isInternalFormatCompressed())
			{
				return new OpenGLCubemap(
						this.context,
						this.textureTarget, 
						this.getInternalCompressionFormat(), 
						this.faceSize,
						GL_RGB,
						opt);
			}
			else
			{
				return new OpenGLCubemap(
						this.context,
						this.textureTarget, 
						this.getInternalColorFormat(), 
						this.faceSize,
						GL_RGB,
						opt);
			}
		}
	}
	
	static class DepthBuilder extends DepthTextureBuilderBase<OpenGLContext, OpenGLCubemap>
	{
		private int textureTarget;
		private int faceSize;
		
		DepthBuilder(OpenGLContext context, int textureTarget, int faceSize)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.faceSize = faceSize;
		}
		
		@Override
		public OpenGLCubemap createTexture() 
		{
			OptionalParameters opt = new OptionalParameters();
			opt.useLinearFiltering = this.isLinearFilteringEnabled();
			opt.useMipmaps = this.areMipmapsEnabled();
			opt.maxAnisotropy = this.getMaxAnisotropy();
			
			return new OpenGLCubemap(
					this.context,
					this.textureTarget, 
					TextureType.DEPTH,
					this.getInternalPrecision(),
					this.faceSize,
					GL_DEPTH_COMPONENT,
					opt);
		}
	}
	
	static class StencilBuilder extends StencilTextureBuilderBase<OpenGLContext, OpenGLCubemap>
	{
		private int textureTarget;
		private int faceSize;
		
		StencilBuilder(OpenGLContext context, int textureTarget, int faceSize)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.faceSize = faceSize;
		}
		
		@Override
		public OpenGLCubemap createTexture() 
		{
			OptionalParameters opt = new OptionalParameters();
			opt.useLinearFiltering = this.isLinearFilteringEnabled();
			opt.useMipmaps = this.areMipmapsEnabled();
			opt.maxAnisotropy = this.getMaxAnisotropy();
			
			return new OpenGLCubemap(
					this.context,
					this.textureTarget, 
					TextureType.STENCIL,
					this.getInternalPrecision(), 
					this.faceSize,
					GL_STENCIL_INDEX,
					opt);
		}
	}
	
	static class DepthStencilBuilder extends DepthStencilTextureBuilderBase<OpenGLContext, OpenGLCubemap>
	{
		private int textureTarget;
		private int faceSize;
		
		DepthStencilBuilder(OpenGLContext context, int textureTarget, int faceSize)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.faceSize = faceSize;
		}
		
		@Override
		public OpenGLCubemap createTexture() 
		{
			OptionalParameters opt = new OptionalParameters();
			opt.useLinearFiltering = this.isLinearFilteringEnabled();
			opt.useMipmaps = this.areMipmapsEnabled();
			opt.maxAnisotropy = this.getMaxAnisotropy();
			
			return new OpenGLCubemap(
					this.context,
					this.textureTarget, 
					this.isFloatingPointEnabled() ? TextureType.FLOATING_POINT_DEPTH_STENCIL : TextureType.DEPTH_STENCIL,
					this.isFloatingPointEnabled() ? 40 : 32,
					this.faceSize,
					GL_DEPTH_STENCIL,
					opt);
		}
	}
	
	private static class OptionalParameters
	{
		int dataType = GL_UNSIGNED_BYTE;
		
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

	private OpenGLCubemap(OpenGLContext context, int textureTarget, ColorFormat colorFormat, int faceSize, int format, OptionalParameters opt) 
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		super(context, colorFormat);
		init(textureTarget, context.getOpenGLInternalColorFormat(colorFormat), faceSize, format, opt);
	}
	
	private OpenGLCubemap(OpenGLContext context, int textureTarget, CompressionFormat compressionFormat, int faceSize, int format, OptionalParameters opt) 
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		super(context, compressionFormat);
		init(textureTarget, context.getOpenGLCompressionFormat(compressionFormat), faceSize, format, opt);
	}
	
	private OpenGLCubemap(OpenGLContext context, int textureTarget, TextureType textureType, int precision, int faceSize, int format, OptionalParameters opt)
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		super(context, textureType);
		init(textureTarget, getSpecialInternalFormat(context, textureType, precision), faceSize, format, opt);
	}
	
	private void init(int textureTarget, int internalFormat, int faceSize, int format, OptionalParameters opt)
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		this.textureTarget = textureTarget;
		this.bind();
		this.faceSize = faceSize;
		
		glPixelStorei(GL_UNPACK_ALIGNMENT, getUnpackAlignment(format, opt.dataType));
		this.context.openGLErrorCheck();
		
		if (opt.positiveX == null)
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, internalFormat, faceSize, faceSize, 0, format, opt.dataType, 0);
			this.context.openGLErrorCheck();
		}
		else
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, internalFormat, faceSize, faceSize, 0, format, opt.dataType, opt.positiveX);
			this.context.openGLErrorCheck();
		}
		
		if (opt.negativeX == null)
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, internalFormat, faceSize, faceSize, 0, format, opt.dataType, 0);
			this.context.openGLErrorCheck();
		}
		else
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, internalFormat, faceSize, faceSize, 0, format, opt.dataType, opt.negativeX);
			this.context.openGLErrorCheck();
		}
		
		if (opt.positiveY == null)
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, internalFormat, faceSize, faceSize, 0, format, opt.dataType, 0);
			this.context.openGLErrorCheck();
		}
		else
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, internalFormat, faceSize, faceSize, 0, format, opt.dataType, opt.positiveY);
			this.context.openGLErrorCheck();
		}
		
		if (opt.negativeY == null)
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, internalFormat, faceSize, faceSize, 0, format, opt.dataType, 0);
			this.context.openGLErrorCheck();
		}
		else
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, internalFormat, faceSize, faceSize, 0, format, opt.dataType, opt.negativeY);
			this.context.openGLErrorCheck();
		}

		if (opt.positiveZ == null)
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, internalFormat, faceSize, faceSize, 0, format, opt.dataType, 0);
			this.context.openGLErrorCheck();
		}
		else
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, internalFormat, faceSize, faceSize, 0, format, opt.dataType, opt.positiveZ);
			this.context.openGLErrorCheck();
		}
		
		if (opt.negativeZ == null)
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, internalFormat, faceSize, faceSize, 0, format, opt.dataType, 0);
			this.context.openGLErrorCheck();
		}
		else
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, internalFormat, faceSize, faceSize, 0, format, opt.dataType, opt.negativeZ);
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
			int dim = faceSize;
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
	public int getFaceSize()
	{
		return this.faceSize;
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
