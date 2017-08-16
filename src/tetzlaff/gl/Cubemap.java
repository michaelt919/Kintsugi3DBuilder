package tetzlaff.gl;

public interface Cubemap <ContextType extends Context<ContextType>> extends Texture<ContextType>
{
    /**
     * Gets the length in pixels along a side of one of the cubemap's faces.
     * @return The size of a face.
     */
    int getFaceSize();

    void setTextureWrap(TextureWrapMode wrapS, TextureWrapMode wrapT);

    FramebufferAttachment<ContextType> getFaceAsFramebufferAttachment(CubemapFace face);
}
