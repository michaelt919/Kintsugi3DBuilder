package tetzlaff.gl.builders.framebuffer;


public class DepthStencilAttachmentSpec extends AttachmentSpec
{
	/**
	 * Whether or not floating-point storage for the depth component.
	 */
	public final boolean floatingPointDepth;
	
	/**
	 * Creates a new depth+stencil attachment specification.
	 * @param floatingPoint Whether or not floating-point storage for the depth attachment.
	 */
	public DepthStencilAttachmentSpec(boolean floatingPointDepth)
	{
		this.floatingPointDepth = floatingPointDepth;
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
