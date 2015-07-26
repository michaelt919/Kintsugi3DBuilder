package tetzlaff.gl.builders.framebuffer;

public class DepthStencilAttachmentSpec extends AttachmentSpec
{
	public final boolean floatingPointDepth;
	
	public DepthStencilAttachmentSpec(boolean floatingPointDepth)
	{
		this.floatingPointDepth = floatingPointDepth;
	}
}
