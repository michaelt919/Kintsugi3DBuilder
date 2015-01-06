package tetzlaff.gl;

public interface FramebufferObject<AttachmentType, TextureType extends AttachmentType> extends Framebuffer
{
	TextureType getColorAttachmentTexture(int index);
	TextureType getDepthAttachmentTexture();
	
	void setColorAttachment(int index, AttachmentType attachment);
	void setDepthAttachment(AttachmentType attachment);
}
