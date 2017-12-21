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
