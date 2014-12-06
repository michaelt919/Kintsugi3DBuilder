package tetzlaff.gl;

public interface FramebufferObject<TextureType> extends Framebuffer
{
	TextureType getColorAttachmentTexture(int index);
}
