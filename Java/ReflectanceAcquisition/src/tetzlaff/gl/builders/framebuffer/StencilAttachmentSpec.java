package tetzlaff.gl.builders.framebuffer;

public class StencilAttachmentSpec extends AttachmentSpec
{
	public final int precision;
	
	public StencilAttachmentSpec(int precision)
	{
		this.precision = precision;
	}
}
