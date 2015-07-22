package tetzlaff.gl.builders;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.FramebufferObject;

public interface FramebufferObjectBuilder<ContextType extends Context> 
{
	FramebufferObjectBuilder<ContextType> addColorAttachment();
	FramebufferObjectBuilder<ContextType> addColorAttachment(ColorFormat format);
	FramebufferObjectBuilder<ContextType> addColorAttachments(int count);
	FramebufferObjectBuilder<ContextType> addColorAttachments(ColorFormat format, int count);
	FramebufferObjectBuilder<ContextType> addDepthAttachment();
	FramebufferObjectBuilder<ContextType> addDepthAttachment(int precision);
	FramebufferObjectBuilder<ContextType> addFloatingPointDepthAttachment();
	FramebufferObjectBuilder<ContextType> addStencilAttachment();
	FramebufferObjectBuilder<ContextType> addStencilAttachment(int precision);
	FramebufferObjectBuilder<ContextType> addCombinedDepthStencilAttachment();
	FramebufferObjectBuilder<ContextType> addCombinedFPDepthStencilAttachment();
	
	FramebufferObject<ContextType> createFramebufferObject();
}
