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
package tetzlaff.gl.builders;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthStencilAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.StencilAttachmentSpec;

/**
 * Implements the builder design pattern for creating framebuffer objects.
 * @author Michael Tetzlaff
 *
 * @param <ContextType>
 */
public interface FramebufferObjectBuilder<ContextType extends Context<ContextType>> 
{
	/**
	 * Adds a color attachment slot without specifying storage for it.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addEmptyColorAttachment();
	
	/**
	 * Adds multiple color attachment slots without specifying storage for them.
	 * @param count The number of attachment slots to add.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addEmptyColorAttachments(int count);
	
	/**
	 * Adds a color attachment to be created with a default RGBA storage format.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addColorAttachment();
	
	/**
	 * Adds a color attachment to be created with a specific color format.
	 * @param format The color format to be used internally.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addColorAttachment(ColorFormat format);
	
	/**
	 * Adds a color attachment to be created as specified by an attachment builder.
	 * @param builder A builder for the color attachment.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addColorAttachment(ColorAttachmentSpec builder);
	
	/**
	 * Adds multiple color attachments to be created with a default RGBA storage format.
	 * @param count The number of color attachments to be created.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addColorAttachments(int count);
	
	/**
	 * Adds multiple color attachments to be created with a specific color format.
	 * @param format The color format to be used internally.
	 * @param count The number of color attachments to be created.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addColorAttachments(ColorFormat format, int count);
	
	/**
	 * Adds multiple color attachments to be created as specified by an attachment builder.
	 * @param builder A builder for the color attachment.
	 * @param count The number of color attachments to be created.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addColorAttachments(ColorAttachmentSpec builder, int count);
	
	/**
	 * Adds a depth attachment to be created with a default storage format.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addDepthAttachment();
	
	/**
	 * Adds a depth attachment to be created with a specified precision level.
	 * @param precision The number of bits to use to store each depth value.
	 * @param floatingPoint Whether or not the depth values should be stored as floating-point values.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addDepthAttachment(int precision, boolean floatingPoint);
	
	/**
	 * Adds a depth attachment to be created as specified by an attachment builder.
	 * @param builder A builder for the depth attachment.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addDepthAttachment(DepthAttachmentSpec builder);
	
	/**
	 * Adds a stencil attachment to be created with a default storage format.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addStencilAttachment();
	
	/**
	 * Adds a stencil attachment to be created with a specified precision level.
	 * @param precision The number of bits to use to store each stencil value.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addStencilAttachment(int precision);
	
	/**
	 * Adds a stencil attachment to be created as specified by an attachment builder.
	 * @param builder A builder for the stencil attachment.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addStencilAttachment(StencilAttachmentSpec builder);

	/**
	 * Adds a depth+stencil attachment to be created with a default storage format.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addCombinedDepthStencilAttachment();
	
	/**
	 * Adds a depth+stencil attachment with an option to use floating-point storage for depth values.
	 * @param floatingPointDepth Whether or not the depth values should be stored as floating-point values.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addCombinedDepthStencilAttachment(boolean floatingPointDepth);
	
	/**
	 * Adds a depth+stencil attachment to be created as specified by an attachment builder.
	 * @param builder A builder for the depth attachment.
	 * @return The calling builder object.
	 */
	FramebufferObjectBuilder<ContextType> addCombinedDepthStencilAttachment(DepthStencilAttachmentSpec builder);
	
	/**
	 * Creates the framebuffer object.
	 * @return The newly created framebuffer object.
	 */
	FramebufferObject<ContextType> createFramebufferObject();
}
