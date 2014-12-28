package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.openGLErrorCheck;

import java.util.AbstractCollection;
import java.util.ArrayList;

import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.opengl.exceptions.OpenGLInvalidFramebufferOperationException;

public class OpenGLFramebufferObject extends OpenGLFramebuffer implements FramebufferObject<OpenGLTexture>, OpenGLResource
{
	private int nativeWidth;
	private int nativeHeight;
	private int fboId;
	private AbstractCollection<OpenGLResource> attachments;
	private OpenGLTexture[] colorTextures;
	private OpenGLTexture depthTexture;
	private OpenGLTexture stencilTexture;
	private OpenGLTexture depthStencilTexture;
	
	public OpenGLFramebufferObject(int width, int height, int colorAttachments, boolean depthAttachment, boolean stencilAttachment, boolean combineDepthAndStencil)
	{
		this.fboId = glGenFramebuffers();
		openGLErrorCheck();
		
		this.nativeWidth = width;
		this.nativeHeight = height;
		this.attachments = new ArrayList<OpenGLResource>();
		
		if (colorAttachments < 0)
		{
			throw new IllegalArgumentException("The number of color attachments cannot be negative.");
		}
		
		if (colorAttachments > GL_MAX_COLOR_ATTACHMENTS)
		{
			throw new IllegalArgumentException("Too many color attachments specified - maximum is " + GL_MAX_COLOR_ATTACHMENTS + ".");
		}
		
		if (colorAttachments == 0 && !depthAttachment && !stencilAttachment)
		{
			throw new IllegalArgumentException("No attachments specified - every FBO must have at least one attachment.");
		}
		
		this.colorTextures = new OpenGLTexture[colorAttachments];
		
		for (int i = 0; i < colorAttachments; i++)
		{
			this.colorTextures[i] = createAttachment(GL_COLOR_ATTACHMENT0 + i, GL_RGBA, GL_RGBA);
		}
		
		if (depthAttachment && stencilAttachment && combineDepthAndStencil)
		{
			this.depthStencilTexture = createAttachment(GL_DEPTH_STENCIL_ATTACHMENT, GL_DEPTH_STENCIL, GL_DEPTH_STENCIL);
		}
		else
		{
			if (depthAttachment)
			{
				this.depthTexture = createAttachment(GL_DEPTH_ATTACHMENT, GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT);
			}
			
			if (stencilAttachment)
			{
				this.stencilTexture = createAttachment(GL_STENCIL_ATTACHMENT, GL_STENCIL_INDEX, GL_STENCIL_INDEX);
			}
		}
		
		if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
		{
			throw new OpenGLInvalidFramebufferOperationException();
		}
		openGLErrorCheck();
	}
	
	private OpenGLTexture createAttachment(int attachmentType, int internalFormat, int format)
	{
		//FramebufferAttachment attachment = new OpenGLRenderbuffer(0, internalFormat, this.nativeWidth, this.nativeHeight);
		OpenGLTexture attachment = new OpenGLTexture2D(internalFormat, this.nativeWidth, this.nativeHeight, format);
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.fboId);
		openGLErrorCheck();
		attachment.attachToDrawFramebuffer(attachmentType, 0);
		attachments.add(attachment);
		return attachment;
	}
	
	public OpenGLFramebufferObject(int width, int height, int colorAttachments)
	{
		this(width, height, colorAttachments, true, false, false);
	}
	
	public OpenGLFramebufferObject(int width, int height)
	{
		this(width, height, 1);
	}
	
	@Override
	protected int getId()
	{
		return fboId;
	}
	
	@Override
	public int getWidth()
	{
		return this.nativeWidth;
	}
	
	@Override
	public int getHeight()
	{
		return this.nativeHeight;
	}

	@Override
	protected void selectColorSourceForRead(int index) 
	{
		glReadBuffer(GL_COLOR_ATTACHMENT0 + index);
		openGLErrorCheck();
	}
	
	@Override
	public OpenGLTexture getColorAttachmentTexture(int index)
	{
		if(index < 0)
		{
			throw new IllegalArgumentException("Attachment index cannot be negative.");
		}
		else if (index > this.colorTextures.length)
		{
			throw new IllegalArgumentException("Attachment index (" + index + 
					") exceeded the number of color attachments for the framebufer (" + this.colorTextures.length + ").");
		}
		else if (this.colorTextures[index] == null)
		{
			throw new UnsupportedOperationException("The color attachment for the framebuffer at index " + index + " is not a texture.");
		}
		else return colorTextures[index];
	}
	
	@Override
	public void delete()
	{
		glDeleteFramebuffers(this.fboId);
		openGLErrorCheck();
		for (OpenGLResource attachment : attachments)
		{
			attachment.delete();
		}
	}
}
