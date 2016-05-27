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
package tetzlaff.gl;

/**
 * An interface for a framebuffer object (FBO) that has been created by a GL context.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the FBO is associated with.
 */
public interface FramebufferObject<ContextType extends Context<ContextType>> extends Framebuffer<ContextType>, Resource, Contextual<ContextType>
{
	/**
	 * Gets a texture bound as a color attachment.
	 * An exception will be thrown if the requested color attachment does not exist.
	 * @param index The index of the color attachment to be retrieved.
	 * @return The texture being used as a color attachment.
	 */
	Texture2D<ContextType> getColorAttachmentTexture(int index);
	
	/**
	 * Gets the texture bound as the depth attachment.
	 * An exception will be thrown if a depth attachment does not exist.
	 * @return The texture being used as the depth attachment.
	 */
	Texture2D<ContextType> getDepthAttachmentTexture();
	
	/**
	 * Sets the storage for one of the framebuffer's color attachments.
	 * @param index The index of the attachment point at which to bind the storage.
	 * @param attachment The storage to use for the color attachment.
	 */
	void setColorAttachment(int index, FramebufferAttachment<ContextType> attachment);
	
	/**
	 * Sets the storage for the framebuffer's depth attachment.
	 * @param attachment The storage to use for the depth attachment.
	 */
	void setDepthAttachment(FramebufferAttachment<ContextType> attachment);
}
