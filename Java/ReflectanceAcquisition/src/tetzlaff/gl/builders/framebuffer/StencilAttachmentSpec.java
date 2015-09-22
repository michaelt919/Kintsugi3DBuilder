package tetzlaff.gl.builders.framebuffer;

/**
 * A builder-like object for specifying a framebuffer stencil attachment.
 * @author Michael Tetzlaff
 *
 */
public class StencilAttachmentSpec extends AttachmentSpec
{
	/**
	 * The number of bits to use to represent each stencil value.
	 */
	public final int precision;
	
	/**
	 * Creates a new stencil attachment specification.
	 * @param precision The number of bits to use to represent each depth value.
	 */
	public StencilAttachmentSpec(int precision)
	{
		this.precision = precision;
	}
	
	@Override
	public StencilAttachmentSpec setMultisamples(int samples, boolean fixedSampleLocations)
	{
		super.setMultisamples(samples, fixedSampleLocations);
		return this;
	}

	@Override
	public StencilAttachmentSpec setMipmapsEnabled(boolean enabled)
	{
		super.setMipmapsEnabled(enabled);
		return this;
	}
	
	@Override
	public StencilAttachmentSpec setLinearFilteringEnabled(boolean enabled)
	{
		super.setLinearFilteringEnabled(enabled);
		return this;
	}
}
