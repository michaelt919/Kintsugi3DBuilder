package openGL.wrappers.interfaces;

public interface FramebufferObject extends Framebuffer, GLResource
{
	Texture getColorAttachmentTexture(int index);
}
