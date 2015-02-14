package tetzlaff.ulf;

import java.io.IOException;

import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.opengl.OpenGLContext;

public class ULFRendererList extends ULFDrawableListModel
{
	public ULFRendererList(OpenGLContext context, Trackball trackball) 
	{
		super(context, trackball);
	}
	
	@Override
	protected ULFRenderer createFromVSETFile(String vsetFile) throws IOException
	{
		return new ULFRenderer(context, vsetFile, trackball);
	}
	
	@Override
	protected ULFMorphRenderer createMorphFromLFMFile(String lfmFile) throws IOException
	{
		return new ULFMorphRenderer(context, lfmFile, trackball);
	}
}
