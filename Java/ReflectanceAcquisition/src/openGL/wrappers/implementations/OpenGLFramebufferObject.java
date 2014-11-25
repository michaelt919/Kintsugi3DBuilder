package openGL.wrappers.implementations;

import static openGL.OpenGLHelper.openGLErrorCheck;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.AbstractCollection;
import java.util.ArrayList;

import openGL.exceptions.OpenGLInvalidFramebufferOperationException;
import openGL.wrappers.interfaces.FramebufferAttachment;
import openGL.wrappers.interfaces.FramebufferObject;
import openGL.wrappers.interfaces.GLResource;
import openGL.wrappers.interfaces.Texture;

public class OpenGLFramebufferObject extends OpenGLFramebuffer implements FramebufferObject
{
	private int nativeWidth;
	private int nativeHeight;
	private int fboId;
	private AbstractCollection<GLResource> attachments;
	private Texture[] colorTextures;
	private Texture depthTexture;
	private Texture stencilTexture;
	private Texture depthStencilTexture;
	
	public OpenGLFramebufferObject(int width, int height, int colorAttachments, boolean depthAttachment, boolean stencilAttachment, boolean combineDepthAndStencil)
	{
		this.fboId = glGenFramebuffers();
		openGLErrorCheck();
		
		this.nativeWidth = width;
		this.nativeHeight = height;
		this.attachments = new ArrayList<GLResource>();
		
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
		
		this.colorTextures = new Texture[colorAttachments];
		
		for (int i = 0; i < colorAttachments; i++)
		{
			this.colorTextures[i] = createAttachment(GL_COLOR_ATTACHMENT0 + i, GL_RGBA);
		}
		
		if (depthAttachment && stencilAttachment && combineDepthAndStencil)
		{
			this.depthStencilTexture = createAttachment(GL_DEPTH_STENCIL_ATTACHMENT, GL_DEPTH_STENCIL);
		}
		else
		{
			if (depthAttachment)
			{
				this.depthTexture = createAttachment(GL_DEPTH_ATTACHMENT, GL_DEPTH_COMPONENT);
			}
			
			if (stencilAttachment)
			{
				this.stencilTexture = createAttachment(GL_STENCIL_ATTACHMENT, GL_STENCIL_INDEX);
			}
		}
		
		if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
		{
			throw new OpenGLInvalidFramebufferOperationException();
		}
		openGLErrorCheck();
	}
	
	private Texture createAttachment(int attachmentType, int internalFormat)
	{
		//FramebufferAttachment attachment = new OpenGLRenderbuffer(0, internalFormat, this.nativeWidth, this.nativeHeight);
		Texture attachment = new OpenGLTexture2D(internalFormat, this.nativeWidth, this.nativeHeight);
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.fboId);
		openGLErrorCheck();
		attachment.attachToDrawFramebuffer(attachmentType, 0);
		attachments.add(attachment);
		return attachment;
	}
	
	public OpenGLFramebufferObject(int width, int height, int colorAttachments)
	{
		this(width, height, colorAttachments, false, false, false);
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
	public Texture getColorAttachmentTexture(int index)
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
		for (GLResource attachment : attachments)
		{
			attachment.delete();
		}
	}
}
