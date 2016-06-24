package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;

import tetzlaff.gl.Texture1D;
import tetzlaff.gl.builders.base.ColorTextureBuilderBase;
// mipmaps

class OpenGLTexture1D extends OpenGLTexture implements Texture1D<OpenGLContext>
{
	private int textureTarget;
	private int width;
	private int levelCount;
	
	static class OpenGLTexture1DFromBufferBuilder extends ColorTextureBuilderBase<OpenGLContext, OpenGLTexture1D>
	{
		private int textureTarget;
		private int width;
		private int format;
		private int type;
		private ByteBuffer buffer;
		
		OpenGLTexture1DFromBufferBuilder(OpenGLContext context, int textureTarget, int width, int format, int type, ByteBuffer buffer)
		{
			super(context);
			this.textureTarget = textureTarget;
			this.width = width;
			this.format = format;
			this.type = type;
			this.buffer = buffer;
		}

		@Override
		public OpenGLTexture1D createTexture() 
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
			
			return new OpenGLTexture1D(
					this.context,
					this.textureTarget, 
					colorFormat, 
					this.width,
					this.format,
					this.type,
					this.buffer,
					this.isLinearFilteringEnabled(),
					this.areMipmapsEnabled(),
					this.getMaxAnisotropy());
		}
	}
	
	private OpenGLTexture1D(OpenGLContext context, int textureTarget, int internalFormat, int width, int format, int type, ByteBuffer buffer, boolean useLinearFiltering, boolean useMipmaps, float maxAnisotropy) 
	{
		// Create an empty texture to be used as a render target for a framebuffer.
		super(context);
		this.textureTarget = textureTarget;
		this.bind();
		this.width = width;
		glTexImage1D(textureTarget, 0, internalFormat, width, 0, format, type, buffer);
		this.context.openGLErrorCheck();

		if (useMipmaps)
		{
			// Create mipmaps
			glGenerateMipmap(textureTarget);
	        this.context.openGLErrorCheck();
	        
	        // Calculate the number of mipmap levels
			this.levelCount = 0;
			int dim = width;
			while (dim > 0)
			{
				this.levelCount++;
				dim /= 2;
			}
			
			if (useLinearFiltering)
			{
				glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		        this.context.openGLErrorCheck();
				glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		        this.context.openGLErrorCheck();
			}
			else
			{
				glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
		        this.context.openGLErrorCheck();
				glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		        this.context.openGLErrorCheck();
			}
		}
		else
		{
			// No mipmaps
			this.levelCount = 1;
			
			if (useLinearFiltering)
			{
				glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		        this.context.openGLErrorCheck();
				glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		        this.context.openGLErrorCheck();
			}
			else
			{
				glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		        this.context.openGLErrorCheck();
				glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		        this.context.openGLErrorCheck();
			}
		}
		
		glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        this.context.openGLErrorCheck();
		
		if (maxAnisotropy > 1.0f)
		{
			glTexParameterf(textureTarget, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
	        this.context.openGLErrorCheck();
		}
	}
	
	@Override
	public int getWidth()
	{
		return this.width;
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
