package tetzlaff.gl.builders.framebuffer;

import tetzlaff.gl.ColorFormat;

/**
 * A builder-like object for specifying a framebuffer color attachment.
 * @author Michael Tetzlaff
 *
 */
public class ColorAttachmentSpec extends AttachmentSpec
{
	/**
	 * The color format to be used internally.
	 */
	public final ColorFormat internalFormat;
	
	/**
	 * Creates a new color attachment specification.
	 * @param internalFormat The color format to be used internally.
	 */
	public ColorAttachmentSpec(ColorFormat internalFormat)
	{
		this.internalFormat = internalFormat;
	}
	
	@Override
	public ColorAttachmentSpec setMultisamples(int samples, boolean fixedSampleLocations)
	{
		super.setMultisamples(samples, fixedSampleLocations);
		return this;
	}

	@Override
	public ColorAttachmentSpec setMipmapsEnabled(boolean enabled)
	{
		super.setMipmapsEnabled(enabled);
		return this;
	}
	
	@Override
	public ColorAttachmentSpec setLinearFilteringEnabled(boolean enabled)
	{
		super.setLinearFilteringEnabled(enabled);
		return this;
	}
}
