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
}
