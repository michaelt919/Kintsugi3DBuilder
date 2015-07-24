package tetzlaff.gl.builders.framebuffer;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

public class StencilAttachmentSpec extends AttachmentSpec
{
	public final int precision;
	
	public StencilAttachmentSpec(int precision)
	{
		this.precision = precision;
	}
}
