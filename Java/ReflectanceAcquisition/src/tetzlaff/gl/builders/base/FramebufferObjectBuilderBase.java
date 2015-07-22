package tetzlaff.gl.builders.base;

import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.builders.FramebufferObjectBuilder;

public abstract class FramebufferObjectBuilderBase<ContextType extends Context> implements FramebufferObjectBuilder<ContextType>
{
	public final int width;
	public final int height;
	
	private final List<ColorFormat> colorAttachmentFormats = new ArrayList<ColorFormat>();
	private int depthAttachmentPrecision = 0;
	private int stencilAttachmentPrecision = 0;
	private boolean floatingPointDepthAttachment = false;
	private boolean combinedDepthStencilAttachment = false;
	
	public int getColorAttachmentCount()
	{
		return colorAttachmentFormats.size();
	}
	
	public ColorFormat getColorAttachmentFormat(int index)
	{
		return colorAttachmentFormats.get(index);
	}
	
	public boolean hasDepthAttachment()
	{
		return this.depthAttachmentPrecision > 0;
	}
	
	public boolean hasStencilAttachment()
	{
		return this.depthAttachmentPrecision > 0;
	}
	
	public int getDepthAttachmentPrecision()
	{
		return this.depthAttachmentPrecision;
	}
	
	public int getStencilAttachmentPrecision()
	{
		return this.stencilAttachmentPrecision;
	}
	
	public boolean hasFloatingPointDepthAttachment()
	{
		return this.hasDepthAttachment() && this.floatingPointDepthAttachment;
	}
	
	public boolean hasCombinedDepthStencilAttachment()
	{
		return this.combinedDepthStencilAttachment;
	}
	
	protected FramebufferObjectBuilderBase(int width, int height)
	{
		this.width = width;
		this.height = height;
	}
	
	public FramebufferObjectBuilder<ContextType> addColorAttachment()
	{
		return this.addColorAttachment(ColorFormat.RGB8);
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addColorAttachment(ColorFormat format)
	{
		this.colorAttachmentFormats.add(format);
		return this;
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addColorAttachments(int count)
	{
		return this.addColorAttachments(ColorFormat.RGB8, count);
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addColorAttachments(ColorFormat format, int count)
	{
		for (int i = 0; i < count; i++)
		{
			this.addColorAttachment(format);
		}
		return this;
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addDepthAttachment()
	{
		return this.addDepthAttachment(16);
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addDepthAttachment(int precision)
	{
		if (this.depthAttachmentPrecision == 0)
		{
			this.depthAttachmentPrecision = precision;
			return this;
		}
		else
		{
			throw new IllegalStateException("A depth attachment already exists.");
		}
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addFloatingPointDepthAttachment()
	{
		if (this.depthAttachmentPrecision == 0)
		{
			this.depthAttachmentPrecision = 32;
			this.floatingPointDepthAttachment = true;
			return this;
		}
		else
		{
			throw new IllegalStateException("A depth attachment already exists.");
		}
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addStencilAttachment()
	{
		return this.addStencilAttachment(8);
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addStencilAttachment(int precision)
	{
		if (this.stencilAttachmentPrecision == 0)
		{
			this.stencilAttachmentPrecision = precision;
			return this;
		}
		else
		{
			throw new IllegalStateException("A stencil attachment already exists.");
		}
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addCombinedDepthStencilAttachment()
	{
		if (this.depthAttachmentPrecision == 0 && this.stencilAttachmentPrecision == 0)
		{
			this.depthAttachmentPrecision = 24;
			this.stencilAttachmentPrecision = 8;
			this.floatingPointDepthAttachment = false;
			this.combinedDepthStencilAttachment = true;
			return this;
		}
		else
		{
			throw new IllegalStateException("A depth or stencil attachment already exists.");
		}
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addCombinedFPDepthStencilAttachment()
	{
		if (this.depthAttachmentPrecision == 0 && this.stencilAttachmentPrecision == 0)
		{
			this.depthAttachmentPrecision = 32;
			this.stencilAttachmentPrecision = 8;
			this.floatingPointDepthAttachment = true;
			this.combinedDepthStencilAttachment = true;
			return this;
		}
		else
		{
			throw new IllegalStateException("A depth or stencil attachment already exists.");
		}
	}
}
