package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static tetzlaff.gl.opengl.helpers.StaticHelpers.openGLErrorCheck;

import java.nio.IntBuffer;
import java.util.AbstractCollection;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;

import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.opengl.exceptions.OpenGLInvalidFramebufferOperationException;

public class OpenGLFramebufferObject 
	extends OpenGLFramebuffer implements FramebufferObject<OpenGLFramebufferAttachment, OpenGLTexture2D>, OpenGLResource
{
	private int nativeWidth;
	private int nativeHeight;
	private int fboId;
	private AbstractCollection<OpenGLResource> ownedAttachments;
	private OpenGLFramebufferAttachment[] colorAttachments;
	private OpenGLFramebufferAttachment depthAttachment;
	private OpenGLFramebufferAttachment stencilAttachment;
	private OpenGLFramebufferAttachment depthStencilAttachment;
	
	public OpenGLFramebufferObject(int width, int height, int colorAttachmentCount,
			boolean generateDepthAttachment, boolean generateStencilAttachment, boolean combineDepthAndStencil)
	{
		this.fboId = glGenFramebuffers();
		openGLErrorCheck();
		
		this.nativeWidth = width;
		this.nativeHeight = height;
		this.ownedAttachments = new ArrayList<OpenGLResource>();
		
		if (colorAttachmentCount < 0)
		{
			throw new IllegalArgumentException("The number of color attachments cannot be negative.");
		}
		
		if (colorAttachmentCount > GL_MAX_COLOR_ATTACHMENTS)
		{
			throw new IllegalArgumentException("Too many color attachments specified - maximum is " + GL_MAX_COLOR_ATTACHMENTS + ".");
		}
		
		this.colorAttachments = new OpenGLFramebufferAttachment[colorAttachmentCount];
		IntBuffer drawBufferList = BufferUtils.createIntBuffer(colorAttachmentCount);
		
		for (int i = 0; i < colorAttachmentCount; i++)
		{
			this.colorAttachments[i] = createAttachment(GL_COLOR_ATTACHMENT0 + i, GL_RGBA, GL_RGBA);
			drawBufferList.put(i, GL_COLOR_ATTACHMENT0 + i);
		}
		
		if (generateDepthAttachment && generateStencilAttachment && combineDepthAndStencil)
		{
			this.depthStencilAttachment = createAttachment(GL_DEPTH_STENCIL_ATTACHMENT, GL_DEPTH_STENCIL, GL_DEPTH_STENCIL);
		}
		else
		{
			if (generateDepthAttachment)
			{
				this.depthAttachment = createAttachment(GL_DEPTH_ATTACHMENT, GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT);
			}
			
			if (generateStencilAttachment)
			{
				this.stencilAttachment = createAttachment(GL_STENCIL_ATTACHMENT, GL_STENCIL_INDEX, GL_STENCIL_INDEX);
			}
		}
		
		if (colorAttachmentCount > 0)
		{
			glDrawBuffers(drawBufferList);
			openGLErrorCheck();
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
		ownedAttachments.add(attachment);
		return attachment;
	}
	
	public OpenGLFramebufferObject(int width, int height, int colorAttachmentCount, boolean generateDepthAttachment)
	{
		this(width, height, colorAttachmentCount, generateDepthAttachment, false, false);
	}
	
	public OpenGLFramebufferObject(int width, int height, int colorAttachmentCount)
	{
		this(width, height, colorAttachmentCount, true);
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
	public FramebufferSize getSize()
	{
		return new FramebufferSize(this.nativeWidth, this.nativeHeight);
	}
	
	@Override
	protected void selectColorSourceForRead(int index) 
	{
		glReadBuffer(GL_COLOR_ATTACHMENT0 + index);
		openGLErrorCheck();
	}
	
	@Override
	public OpenGLTexture2D getColorAttachmentTexture(int index)
	{
		if(index < 0)
		{
			throw new IllegalArgumentException("Attachment index cannot be negative.");
		}
		else if (index > this.colorAttachments.length)
		{
			throw new IllegalArgumentException("Attachment index (" + index + 
					") exceeded the number of color attachments for the framebufer (" + this.colorAttachments.length + ").");
		}
		else if (this.colorAttachments[index] == null || !(this.colorAttachments[index] instanceof OpenGLTexture2D))
		{
			throw new UnsupportedOperationException("The color attachment for the framebuffer at index " + index + " is not a 2D texture.");
		}
		else return (OpenGLTexture2D)this.colorAttachments[index];
	}
	
	@Override
	public void setColorAttachment(int index, OpenGLFramebufferAttachment attachment)
	{
		if (index < this.colorAttachments.length && this.colorAttachments[index] != null)
		{
			// Remove the attachment from the list of owned attachments if it existed
			if (this.ownedAttachments.remove(this.colorAttachments[index]))
			{
				// Delete it
				((OpenGLResource)this.colorAttachments[index]).delete();
			}
		}
		
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.fboId);
		openGLErrorCheck();
		attachment.attachToDrawFramebuffer(GL_COLOR_ATTACHMENT0 + index, 0);
		this.colorAttachments[index] = attachment;
	}
	
	@Override
	public OpenGLTexture2D getDepthAttachmentTexture()
	{
		if (this.depthAttachment == null || !(this.depthAttachment instanceof OpenGLTexture2D))
		{
			throw new UnsupportedOperationException("The depth attachment for the framebuffer is not a 2D texture.");
		}
		else return (OpenGLTexture2D)this.depthAttachment;
	}
	
	@Override
	public void setDepthAttachment(OpenGLFramebufferAttachment attachment)
	{
		if (this.depthAttachment != null)
		{
			// Remove the attachment from the list of owned attachments if it existed
			if (this.ownedAttachments.remove(this.depthAttachment))
			{
				// Delete it
				((OpenGLResource)this.depthAttachment).delete();
			}
		}
		
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.fboId);
		openGLErrorCheck();
		attachment.attachToDrawFramebuffer(GL_DEPTH_ATTACHMENT, 0);
		
		this.depthAttachment = attachment;
	}
	
	@Override
	public void delete()
	{
		glDeleteFramebuffers(this.fboId);
		openGLErrorCheck();
		for (OpenGLResource attachment : ownedAttachments)
		{
			attachment.delete();
		}
	}
}
