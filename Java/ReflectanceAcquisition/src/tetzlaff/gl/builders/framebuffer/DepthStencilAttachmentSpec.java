package tetzlaff.gl.builders.framebuffer;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture;

public class DepthStencilAttachmentSpec extends AttachmentSpec
{
	public final boolean floatingPointDepth;
	
	public DepthStencilAttachmentSpec(boolean floatingPointDepth)
	{
		this.floatingPointDepth = floatingPointDepth;
	}
}
