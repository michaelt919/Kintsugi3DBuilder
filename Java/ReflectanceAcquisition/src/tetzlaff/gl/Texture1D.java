package tetzlaff.gl;

/**
 * An interface for a one-dimensional texture.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the GL context that the texture is associated with.
 */
public interface Texture1D<ContextType extends Context<ContextType>> extends Texture<ContextType>, FramebufferAttachment<ContextType>
{
	/**
	 * Gets the width of the texture.
	 * @return The width of the texture.
	 */
	int getWidth();
}
