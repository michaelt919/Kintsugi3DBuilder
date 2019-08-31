/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.gl.builders.framebuffer;

import tetzlaff.gl.core.ColorFormat;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferObject;

public interface FramebufferObjectBuilder<ContextType extends Context<ContextType>> 
{
    FramebufferObjectBuilder<ContextType> addEmptyColorAttachment();
    FramebufferObjectBuilder<ContextType> addEmptyColorAttachments(int count);

    FramebufferObjectBuilder<ContextType> addColorAttachment();
    FramebufferObjectBuilder<ContextType> addColorAttachment(ColorFormat format);
    FramebufferObjectBuilder<ContextType> addColorAttachment(ColorAttachmentSpec builder);
    FramebufferObjectBuilder<ContextType> addColorAttachments(int count);
    FramebufferObjectBuilder<ContextType> addColorAttachments(ColorFormat format, int count);
    FramebufferObjectBuilder<ContextType> addColorAttachments(ColorAttachmentSpec builder, int count);

    FramebufferObjectBuilder<ContextType> addDepthAttachment();
    FramebufferObjectBuilder<ContextType> addDepthAttachment(int precision, boolean floatingPoint);
    FramebufferObjectBuilder<ContextType> addDepthAttachment(DepthAttachmentSpec builder);

    FramebufferObjectBuilder<ContextType> addStencilAttachment();
    FramebufferObjectBuilder<ContextType> addStencilAttachment(int precision);
    FramebufferObjectBuilder<ContextType> addStencilAttachment(StencilAttachmentSpec builder);

    FramebufferObjectBuilder<ContextType> addCombinedDepthStencilAttachment();
    FramebufferObjectBuilder<ContextType> addCombinedDepthStencilAttachment(boolean floatingPointDepth);
    FramebufferObjectBuilder<ContextType> addCombinedDepthStencilAttachment(DepthStencilAttachmentSpec builder);

    FramebufferObject<ContextType> createFramebufferObject();
}
