package tetzlaff.gl.builders.framebuffer;

import tetzlaff.gl.ColorFormat;

public class ColorAttachmentSpec extends AttachmentSpec
{
	public final ColorFormat internalFormat;
	
	public ColorAttachmentSpec(ColorFormat internalFormat)
	{
		this.internalFormat = internalFormat;
	}
}
