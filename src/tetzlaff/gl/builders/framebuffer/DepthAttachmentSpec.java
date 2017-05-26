package tetzlaff.gl.builders.framebuffer;

public class DepthAttachmentSpec extends AttachmentSpec
{
	public final int precision;
	public final boolean floatingPoint;
	
	private DepthAttachmentSpec(int precision, boolean floatingPoint)
	{
		this.precision = precision;
		this.floatingPoint = floatingPoint;
	}
	
	public static DepthAttachmentSpec createFixedPointWithPrecision(int precision)
	{
		return new DepthAttachmentSpec(precision, false);
	}
	
	public static DepthAttachmentSpec createFloatingPointWithPrecision(int precision)
	{
		return new DepthAttachmentSpec(precision, true);
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
