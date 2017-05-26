package tetzlaff.gl.builders.framebuffer;

public class DepthStencilAttachmentSpec extends AttachmentSpec
{
	public final boolean floatingPointDepth;
	
	private DepthStencilAttachmentSpec(boolean floatingPointDepth)
	{
		this.floatingPointDepth = floatingPointDepth;
	}
	
	public static DepthStencilAttachmentSpec createWithFixedPointDepth()
	{
		return new DepthStencilAttachmentSpec(true);
	}
	
	public static DepthStencilAttachmentSpec createWithFloatingPointDepth()
	{
		return new DepthStencilAttachmentSpec(false);
	}
	
	@Override
	public DepthStencilAttachmentSpec setMultisamples(int samples, boolean fixedSampleLocations)
	{
		super.setMultisamples(samples, fixedSampleLocations);
		return this;
	}

	@Override
	public DepthStencilAttachmentSpec setMipmapsEnabled(boolean enabled)
	{
		super.setMipmapsEnabled(enabled);
		return this;
	}
	
	@Override
	public DepthStencilAttachmentSpec setLinearFilteringEnabled(boolean enabled)
	{
		super.setLinearFilteringEnabled(enabled);
		return this;
	}
}
