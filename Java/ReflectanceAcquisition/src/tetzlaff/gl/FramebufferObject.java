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
