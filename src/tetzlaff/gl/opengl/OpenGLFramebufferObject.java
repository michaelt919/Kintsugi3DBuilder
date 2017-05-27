package tetzlaff.gl.opengl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.IntBuffer;
import java.util.AbstractCollection;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;

import tetzlaff.gl.FramebufferAttachment;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.Resource;
import tetzlaff.gl.builders.base.FramebufferObjectBuilderBase;

class OpenGLFramebufferObject extends OpenGLFramebuffer implements FramebufferObject<OpenGLContext>, Resource
{
	private int width;
	private int height;
	private int fboId;
	private AbstractCollection<Resource> ownedAttachments;
	private OpenGLFramebufferAttachment[] colorAttachments;
	private OpenGLFramebufferAttachment depthAttachment;
	private OpenGLFramebufferAttachment stencilAttachment;
	private OpenGLFramebufferAttachment depthStencilAttachment;

	public static class OpenGLFramebufferObjectBuilder extends FramebufferObjectBuilderBase<OpenGLContext>
	{
		OpenGLFramebufferObjectBuilder(OpenGLContext context, int width, int height) 
		{
			super(context, width, height);
		}

		@Override
		public OpenGLFramebufferObject createFramebufferObject()
		{
			if (this.getColorAttachmentCount() > GL_MAX_COLOR_ATTACHMENTS)
			{
				throw new IllegalArgumentException("Too many color attachments specified - maximum is " + GL_MAX_COLOR_ATTACHMENTS + ".");
			}
			
			OpenGLTexture2D[] colorAttachments = new OpenGLTexture2D[this.getColorAttachmentCount()];
			for (int i = 0; i < this.getColorAttachmentCount(); i++)
			{
				if (this.getColorAttachmentBuilder(i) != null)
				{
					colorAttachments[i] = (OpenGLTexture2D)this.getColorAttachmentBuilder(i).createTexture();
				}
			}

			OpenGLTexture2D depthAttachment = null;
			OpenGLTexture2D stencilAttachment = null;
			OpenGLTexture2D depthStencilAttachment = null;
			if (this.hasCombinedDepthStencilAttachment())
			{
				depthStencilAttachment = (OpenGLTexture2D)this.getDepthStencilAttachmentBuilder().createTexture();
			}
			else
			{
				if (this.hasDepthAttachment())
				{
					depthAttachment = (OpenGLTexture2D)this.getDepthAttachmentBuilder().createTexture();
				}
				
				if (this.hasStencilAttachment())
				{
					stencilAttachment = (OpenGLTexture2D)this.getStencilAttachmentBuilder().createTexture();
				}
			}
			
			return new OpenGLFramebufferObject(context, width, height, colorAttachments, depthAttachment, stencilAttachment, depthStencilAttachment);
		}
	}
	
	private OpenGLFramebufferObject(
			OpenGLContext context,
			int width, int height, 
			OpenGLTexture2D[] colorAttachments, 
			OpenGLTexture2D depthAttachment, 
			OpenGLTexture2D stencilAttachment, 
			OpenGLTexture2D depthStencilAttachment)
	{
		super(context);
		
		this.fboId = glGenFramebuffers();
		this.context.openGLErrorCheck();
		
		this.width = width;
		this.height = height;
		this.ownedAttachments = new ArrayList<Resource>();
		this.colorAttachments = new OpenGLFramebufferAttachment[colorAttachments.length];
		this.depthAttachment = depthAttachment;
		this.stencilAttachment = stencilAttachment;
		this.depthStencilAttachment = depthStencilAttachment;
		
		IntBuffer drawBufferList = BufferUtils.createIntBuffer(colorAttachments.length);
		
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.fboId);
		this.context.openGLErrorCheck();
		
		for (int i = 0; i < colorAttachments.length; i++)
		{
			drawBufferList.put(i, GL_COLOR_ATTACHMENT0 + i);
			
			if (colorAttachments[i] != null)
			{
				this.colorAttachments[i] = colorAttachments[i];
				
				colorAttachments[i].attachToDrawFramebuffer(GL_COLOR_ATTACHMENT0 + i, 0);
				ownedAttachments.add(colorAttachments[i]);
			}
		}
		
		if (colorAttachments.length > 0)
		{
			glDrawBuffers(drawBufferList);
			this.context.openGLErrorCheck();
		}
		
		if (depthAttachment != null)
		{
			depthAttachment.attachToDrawFramebuffer(GL_DEPTH_ATTACHMENT, 0);
			ownedAttachments.add(depthAttachment);
		}
		
		if (stencilAttachment != null)
		{
			stencilAttachment.attachToDrawFramebuffer(GL_STENCIL_ATTACHMENT, 0);
			ownedAttachments.add(stencilAttachment);
		}
		
		if (depthStencilAttachment != null)
		{
			depthStencilAttachment.attachToDrawFramebuffer(GL_DEPTH_STENCIL_ATTACHMENT, 0);
			ownedAttachments.add(depthStencilAttachment);
		}
	}
	
	@Override
	int getId()
	{
		return fboId;
	}
	
	@Override
	public FramebufferSize getSize()
	{
		return new FramebufferSize(this.width, this.height);
	}
	
	@Override
	void selectColorSourceForRead(int index) 
	{
		glReadBuffer(GL_COLOR_ATTACHMENT0 + index);
		this.context.openGLErrorCheck();
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
	public void setColorAttachment(int index, FramebufferAttachment<OpenGLContext> attachment)
	{
		if (attachment instanceof OpenGLFramebufferAttachment)
		{
			OpenGLFramebufferAttachment attachmentCast = (OpenGLFramebufferAttachment)attachment;
			
			if (index < this.colorAttachments.length && this.colorAttachments[index] != null)
			{
				// Remove the attachment from the list of owned attachments if it existed
				if (this.ownedAttachments.remove(this.colorAttachments[index]))
				{
					// Delete it
					((Resource)this.colorAttachments[index]).close();
				}
			}
			
			glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.fboId);
			this.context.openGLErrorCheck();
			attachmentCast.attachToDrawFramebuffer(GL_COLOR_ATTACHMENT0 + index, 0);
			this.colorAttachments[index] = attachmentCast;
		}
		else
		{
			throw new IllegalArgumentException("Attachment must be of type OpenGLFramebufferAttachment.");
		}
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
	public void setDepthAttachment(FramebufferAttachment<OpenGLContext> attachment)
	{
		if (attachment instanceof OpenGLFramebufferAttachment)
		{
			OpenGLFramebufferAttachment attachmentCast = (OpenGLFramebufferAttachment)attachment;
			
			if (this.depthAttachment != null)
			{
				// Remove the attachment from the list of owned attachments if it existed
				if (this.ownedAttachments.remove(this.depthAttachment))
				{
					// Delete it
					((Resource)this.depthAttachment).close();
				}
			}
			
			glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.fboId);
			this.context.openGLErrorCheck();
			attachmentCast.attachToDrawFramebuffer(GL_DEPTH_ATTACHMENT, 0);
			
			this.depthAttachment = attachmentCast;
		}
		else
		{
			throw new IllegalArgumentException("Attachment must be of type OpenGLFramebufferAttachment.");
		}
	}

	@Override
	public OpenGLTexture2D getStencilAttachmentTexture()
	{
		if (this.stencilAttachment == null || !(this.stencilAttachment instanceof OpenGLTexture2D))
		{
			throw new UnsupportedOperationException("The stencil attachment for the framebuffer is not a 2D texture.");
		}
		else return (OpenGLTexture2D)this.stencilAttachment;
	}

	
	@Override
	public void setStencilAttachment(FramebufferAttachment<OpenGLContext> attachment)
	{
		if (attachment instanceof OpenGLFramebufferAttachment)
		{
			OpenGLFramebufferAttachment attachmentCast = (OpenGLFramebufferAttachment)attachment;
			
			if (this.stencilAttachment != null)
			{
				// Remove the attachment from the list of owned attachments if it existed
				if (this.ownedAttachments.remove(this.stencilAttachment))
				{
					// Delete it
					((Resource)this.stencilAttachment).close();
				}
			}
			
			glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.fboId);
			this.context.openGLErrorCheck();
			attachmentCast.attachToDrawFramebuffer(GL_STENCIL_ATTACHMENT, 0);
			
			this.stencilAttachment = attachmentCast;
		}
		else
		{
			throw new IllegalArgumentException("Attachment must be of type OpenGLFramebufferAttachment.");
		}
	}
	
	@Override
	public OpenGLTexture2D getDepthStencilAttachmentTexture()
	{
		if (this.depthStencilAttachment == null || !(this.depthStencilAttachment instanceof OpenGLTexture2D))
		{
			throw new UnsupportedOperationException("The depth/stencil attachment for the framebuffer is not a 2D texture.");
		}
		else return (OpenGLTexture2D)this.depthStencilAttachment;
	}

	
	@Override
	public void setDepthStencilAttachment(FramebufferAttachment<OpenGLContext> attachment)
	{
		if (attachment instanceof OpenGLFramebufferAttachment)
		{
			OpenGLFramebufferAttachment attachmentCast = (OpenGLFramebufferAttachment)attachment;
			
			if (this.depthStencilAttachment != null)
			{
				// Remove the attachment from the list of owned attachments if it existed
				if (this.ownedAttachments.remove(this.depthStencilAttachment))
				{
					// Delete it
					((Resource)this.depthStencilAttachment).close();
				}
			}
			
			glBindFramebuffer(GL_DRAW_FRAMEBUFFER, this.fboId);
			this.context.openGLErrorCheck();
			attachmentCast.attachToDrawFramebuffer(GL_DEPTH_STENCIL_ATTACHMENT, 0);
			
			this.depthStencilAttachment = attachmentCast;
		}
		else
		{
			throw new IllegalArgumentException("Attachment must be of type OpenGLFramebufferAttachment.");
		}
	}
	
	@Override
	public void close()
	{
		glDeleteFramebuffers(this.fboId);
		this.context.openGLErrorCheck();
		for (Resource attachment : ownedAttachments)
		{
			attachment.close();
		}
	}
}
