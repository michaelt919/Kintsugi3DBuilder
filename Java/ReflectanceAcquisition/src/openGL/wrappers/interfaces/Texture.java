package openGL.wrappers.interfaces;

public interface Texture extends GLResource, FramebufferAttachment
{
	void bindToTextureUnit(int textureUnitIndex);
}
