package tetzlaff.gl.builders.framebuffer;

public class StencilAttachmentSpec extends AttachmentSpec
{
	public final int precision;
	
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