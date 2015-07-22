package tetzlaff.gl;

public interface FramebufferObject<ContextType extends Context> extends Framebuffer<ContextType>, Resource
{
	Texture2D<ContextType> getColorAttachmentTexture(int index);
	Texture2D<ContextType> getDepthAttachmentTexture();
	
	void setColorAttachment(int index, FramebufferAttachment<ContextType> attachment);
	void setDepthAttachment(FramebufferAttachment<ContextType> attachment);
}
