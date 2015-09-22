package tetzlaff.gl.builders.framebuffer;

/**
 * A builder-like object for specifying a framebuffer depth attachment.
 * @author Michael Tetzlaff
 *
 */
public class DepthAttachmentSpec extends AttachmentSpec
{
	/**
	 * The number of bits to use to represent each depth value.
	 */
	public final int precision;
	
	/**
	 * Whether or not floating-point storage for this depth attachment.
	 */
	public final boolean floatingPoint;
	
	/**
	 * Creates a new depth attachment specification.
	 * @param precision The number of bits to use to represent each depth value.
	 * @param floatingPoint Whether or not floating-point storage for this depth attachment.
	 */
	public DepthAttachmentSpec(int precision, boolean floatingPoint)
	{
		this.precision = precision;
		this.floatingPoint = floatingPoint;
	}
	
	@Override
	public DepthAttachmentSpec setMultisamples(int samples, boolean fixedSampleLocations)
	{
		super.setMultisamples(samples, fixedSampleLocations);
		return this;
	}

	@Override
	public DepthAttachmentSpec setMipmapsEnabled(boolean enabled)
	{
		super.setMipmapsEnabled(enabled);
		return this;
	}
	
	@Override
	public DepthAttachmentSpec setLinearFilteringEnabled(boolean enabled)
	{
		super.setLinearFilteringEnabled(enabled);
		return this;
	}
}
