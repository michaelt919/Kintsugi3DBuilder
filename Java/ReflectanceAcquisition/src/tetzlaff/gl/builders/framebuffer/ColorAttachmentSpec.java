package tetzlaff.gl.builders.framebuffer;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

public class ColorAttachmentSpec extends AttachmentSpec
{
	public final ColorFormat internalFormat;
	
	public ColorAttachmentSpec(ColorFormat internalFormat)
	{
		this.internalFormat = internalFormat;
	}
}
