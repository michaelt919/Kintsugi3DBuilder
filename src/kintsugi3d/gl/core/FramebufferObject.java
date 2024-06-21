/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.core;

/**
 * An interface for a framebuffer object (FBO) that has been created by a GL context.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the FBO is associated with.
 */
public interface FramebufferObject<ContextType extends Context<ContextType>> extends Framebuffer<ContextType>, Resource
{
    Texture2D<ContextType> getColorAttachmentTexture(int index);
    Texture2D<ContextType> getDepthAttachmentTexture();
    Texture2D<ContextType> getStencilAttachmentTexture();
    Texture2D<ContextType> getDepthStencilAttachmentTexture();

    void setColorAttachment(int index, FramebufferAttachment<ContextType> attachment);
    void setDepthAttachment(FramebufferAttachment<ContextType> attachment);
    void setStencilAttachment(FramebufferAttachment<ContextType> attachment);
    void setDepthStencilAttachment(FramebufferAttachment<ContextType> attachment);
}
