package tetzlaff.gl.builders.framebuffer;

/**
 * A superclass for builder-like specification objects that can be used to create attachments for framebuffers.
 * @author Michael Tetzlaff
 *
 */
public abstract class AttachmentSpec
{
	private int multisamples = 1;
	private boolean fixedMultisampleLocations = true;
	private boolean mipmapsEnabled = false;
	private boolean linearFilteringEnabled = false;
	
	/**
	 * Gets the number of samples to be used for multisampling.
	 * @return The number of samples to be used for multisampling.
	 */
	public int getMultisamples()
	{
		return multisamples;
	}
	
	/**
	 * Gets whether or not the sample locations for multisampling are required to be fixed across all texels.
	 * @return true if the sample locations are required to be fixed; false otherwise.
	 */
	public boolean areMultisampleLocationsFixed()
	{
		return fixedMultisampleLocations;
	}
	
	/**
	 * Gets whether or not mipmaps are to be enabled.
	 * @return true if mipmaps are to be enabled, false otherwise.
	 */
	public boolean areMipmapsEnabled()
	{
		return mipmapsEnabled;
	}
	
	/**
	 * Gets whether or not linear filtering is to be enabled.
	 * @return true if linear filtering is to be enabled, false otherwise.
	 */
	public boolean isLinearFilteringEnabled()
	{
		return linearFilteringEnabled;
	}
	
	/**
	 * Sets the number of samples to use for multisampling.
	 * If the number of samples is 1, multisampling will be disabled for this texture.
	 * @param samples The number of samples to use.
	 * @param fixedSampleLocations Whether or not the sample locations are required to be fixed across all texels.
	 * @return The calling builder object.
	 */
	public AttachmentSpec setMultisamples(int samples, boolean fixedSampleLocations)
	{
		// TODO should this be a property of the framebuffer object builder?
		// Having different number of samples per attachment is not allowed.
		multisamples = samples;
		fixedMultisampleLocations = fixedSampleLocations;
		return this;
	}
	
	/**
	 * Sets whether or not mipmaps should be enabled.
	 * If mipmaps are enabled, the texture will come with them pre-generated.
	 * If the texture is modified as the result of being used as a framebuffer attachment, the mipmaps will become stale and will need to be re-generated.
	 * @param enabled Whether or not mipmaps should be enabled.
	 * @return The calling builder object.
	 */
	public AttachmentSpec setMipmapsEnabled(boolean enabled)
	{
		mipmapsEnabled = enabled;
		return this;
	}
	
	/**
	 * Sets whether linear filtering should be enabled.
	 * @param enabled true to enable linear filtering, false otherwise.
	 * @return The calling builder object.
	 */
	public AttachmentSpec setLinearFilteringEnabled(boolean enabled)
	{
		linearFilteringEnabled = enabled;
		return this;
	}
}
