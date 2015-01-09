package tetzlaff.ulf;

import java.io.IOException;

import javax.swing.AbstractListModel;

import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.MultiDrawable;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.opengl.OpenGLContext;

public class ULFRendererList extends ULFDrawableListModel<ULFRenderer>
{
	public ULFRendererList(OpenGLContext context, Trackball trackball) 
	{
		super(context, trackball);
	}
	
	@Override
	protected ULFRenderer createFromDirectory(String directoryPath) throws IOException
	{
		return new ULFRenderer(context, directoryPath, trackball);
	}
}
