package tetzlaff.gl;

public interface Cubemap2D <ContextType extends Context<ContextType>> extends Texture<ContextType>
{
	/**
	 * Gets the width of the texture.
	 * @return The width of the texture.
	 */
	int getWidth();
	
	/**
	 * Gets the height of the texture.
	 * @return The height of the texture.
	 */
	int getHeight();

	void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT);
	
	FramebufferAttachment<ContextType> getFaceAsFramebufferAttachment(CubemapFace face);
}
