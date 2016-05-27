/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package tetzlaff.gl.builders.base;

import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.builders.FramebufferObjectBuilder;
import tetzlaff.gl.builders.TextureBuilder;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthStencilAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.StencilAttachmentSpec;

public abstract class FramebufferObjectBuilderBase<ContextType extends Context<ContextType>> implements FramebufferObjectBuilder<ContextType>
{
	protected final ContextType context;
	protected final int width;
	protected final int height;
	
	private final List<TextureBuilder<ContextType, ? extends Texture2D<ContextType>>> colorAttachmentFormats = 
			new ArrayList<TextureBuilder<ContextType, ? extends Texture2D<ContextType>>>();
	
	private TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> depthAttachmentBuilder = null;
	private TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> stencilAttachmentBuilder = null;
	private TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> depthStencilAttachmentBuilder = null;
	
	protected int getColorAttachmentCount()
	{
		return colorAttachmentFormats.size();
	}
	
	protected TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> getColorAttachmentBuilder(int index)
	{
		return colorAttachmentFormats.get(index);
	}
	
	protected boolean hasDepthAttachment()
	{
		return this.depthAttachmentBuilder != null;
	}
	
	protected boolean hasStencilAttachment()
	{
		return this.stencilAttachmentBuilder != null;
	}
	
	protected boolean hasCombinedDepthStencilAttachment()
	{
		return this.depthStencilAttachmentBuilder != null;
	}
	
	protected TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> getDepthAttachmentBuilder()
	{
		return this.depthAttachmentBuilder;
	}
	
	protected TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> getStencilAttachmentBuilder()
	{
		return this.stencilAttachmentBuilder;
	}
	
	protected TextureBuilder<? super ContextType, ? extends Texture2D<? super ContextType>> getDepthStencilAttachmentBuilder()
	{
		return this.depthStencilAttachmentBuilder;
	}
	
	protected FramebufferObjectBuilderBase(ContextType context, int width, int height)
	{
		this.context = context;
		this.width = width;
		this.height = height;
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addEmptyColorAttachment()
	{
		this.colorAttachmentFormats.add(null);
		return this;
	}

	@Override
	public FramebufferObjectBuilder<ContextType> addColorAttachment()
	{
		return this.addColorAttachment(ColorFormat.RGBA8);
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addColorAttachment(ColorFormat format)
	{
		if (format == null)
		{
			this.colorAttachmentFormats.add(null);
		}
		else
		{
			this.colorAttachmentFormats.add(context.get2DColorTextureBuilder(this.width, this.height).setInternalFormat(format));
		}
		return this;
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addColorAttachment(ColorAttachmentSpec builder)
	{
		this.colorAttachmentFormats.add(context.get2DColorTextureBuilder(width, height)
				.setInternalFormat(builder.internalFormat)
				.setMultisamples(builder.getMultisamples(), builder.areMultisampleLocationsFixed())
				.setMipmapsEnabled(builder.areMipmapsEnabled())
				.setLinearFilteringEnabled(builder.isLinearFilteringEnabled()));
		return this;
	}

	@Override
	public FramebufferObjectBuilder<ContextType> addEmptyColorAttachments(int count)
	{
		for (int i = 0; i < count; i++)
		{
			this.addEmptyColorAttachment();
		}
		return this;
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addColorAttachments(int count)
	{
		return this.addColorAttachments(ColorFormat.RGBA8, count);
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
	public FramebufferObjectBuilder<ContextType> addColorAttachments(ColorAttachmentSpec builder, int count)
	{
		for (int i = 0; i < count; i++)
		{
			this.addColorAttachment(builder);
		}
		return this;
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addDepthAttachment()
	{
		return this.addDepthAttachment(16, false);
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addDepthAttachment(int precision, boolean floatingPoint)
	{
		if (!this.hasDepthAttachment())
		{
			this.depthAttachmentBuilder = context.get2DDepthTextureBuilder(this.width, this.height)
											.setInternalPrecision(precision)
											.setFloatingPointEnabled(floatingPoint);
			return this;
		}
		else
		{
			throw new IllegalStateException("A depth attachment already exists.");
		}
	}

	@Override
	public FramebufferObjectBuilder<ContextType> addDepthAttachment(DepthAttachmentSpec builder)
	{
		if (!this.hasDepthAttachment() && !this.hasCombinedDepthStencilAttachment())
		{
			this.depthAttachmentBuilder = context.get2DDepthTextureBuilder(this.width, this.height)
											.setInternalPrecision(builder.precision)
											.setFloatingPointEnabled(builder.floatingPoint)
											.setMultisamples(builder.getMultisamples(), builder.areMultisampleLocationsFixed())
											.setMipmapsEnabled(builder.areMipmapsEnabled())
											.setLinearFilteringEnabled(builder.isLinearFilteringEnabled());
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
		if (!this.hasStencilAttachment() && !this.hasCombinedDepthStencilAttachment())
		{
			this.stencilAttachmentBuilder = context.get2DStencilTextureBuilder(this.width, this.height).setInternalPrecision(precision);
			return this;
		}
		else
		{
			throw new IllegalStateException("A stencil attachment already exists.");
		}
	}

	@Override
	public FramebufferObjectBuilder<ContextType> addStencilAttachment(StencilAttachmentSpec builder)
	{
		if (!this.hasStencilAttachment() && !this.hasCombinedDepthStencilAttachment())
		{
			this.stencilAttachmentBuilder = context.get2DStencilTextureBuilder(this.width, this.height)
												.setInternalPrecision(builder.precision)
												.setMultisamples(builder.getMultisamples(), builder.areMultisampleLocationsFixed())
												.setMipmapsEnabled(builder.areMipmapsEnabled())
												.setLinearFilteringEnabled(builder.isLinearFilteringEnabled());
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
		if (!this.hasDepthAttachment() && !this.hasStencilAttachment() && !this.hasCombinedDepthStencilAttachment())
		{
			this.depthStencilAttachmentBuilder = context.get2DDepthStencilTextureBuilder(this.width, this.height);
			return this;
		}
		else
		{
			throw new IllegalStateException("A depth or stencil attachment already exists.");
		}
	}
	
	@Override
	public FramebufferObjectBuilder<ContextType> addCombinedDepthStencilAttachment(boolean floatingPoint)
	{
		if (!this.hasDepthAttachment() && !this.hasStencilAttachment() && !this.hasCombinedDepthStencilAttachment())
		{
			this.depthStencilAttachmentBuilder = context.get2DDepthStencilTextureBuilder(this.width, this.height).setFloatingPointDepthEnabled(floatingPoint);
			return this;
		}
		else
		{
			throw new IllegalStateException("A depth or stencil attachment already exists.");
		}
	}

	@Override
	public FramebufferObjectBuilder<ContextType> addCombinedDepthStencilAttachment(DepthStencilAttachmentSpec builder)
	{
		if (!this.hasDepthAttachment() && !this.hasStencilAttachment() && !this.hasCombinedDepthStencilAttachment())
		{
			this.depthStencilAttachmentBuilder = context.get2DDepthStencilTextureBuilder(this.width, this.height)
													.setFloatingPointDepthEnabled(builder.floatingPointDepth)
													.setMultisamples(builder.getMultisamples(), builder.areMultisampleLocationsFixed())
													.setMipmapsEnabled(builder.areMipmapsEnabled())
													.setLinearFilteringEnabled(builder.isLinearFilteringEnabled());
			return this;
		}
		else
		{
			throw new IllegalStateException("A depth or stencil attachment already exists.");
		}
	}
}
