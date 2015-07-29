package tetzlaff.gl;

/**
 * An interface for a framebuffer object (FBO) that has been created by a GL context.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the FBO is associated with.
 */
public interface FramebufferObject<ContextType extends Context<ContextType>> extends Framebuffer<ContextType>, Resource, Contextual<ContextType>
{
	Texture2D<ContextType> getColorAttachmentTexture(int index);
	Texture2D<ContextType> getDepthAttachmentTexture();
	
	void setColorAttachment(int index, FramebufferAttachment<ContextType> attachment);
	void setDepthAttachment(FramebufferAttachment<ContextType> attachment);
}
