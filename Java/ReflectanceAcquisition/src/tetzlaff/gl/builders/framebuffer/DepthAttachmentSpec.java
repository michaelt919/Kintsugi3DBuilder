package tetzlaff.gl.builders.framebuffer;

public class DepthAttachmentSpec extends AttachmentSpec
{
	public final int precision;
	public final boolean floatingPoint;
	
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
