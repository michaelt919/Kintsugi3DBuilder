package tetzlaff.ulf;

import java.io.IOException;

import javax.swing.AbstractListModel;

import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.MultiDrawable;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.opengl.OpenGLContext;

public class ULFToTexturesList extends ULFDrawableListModel<ULFToTextures>
{
	public ULFToTexturesList(OpenGLContext context, Trackball trackball) 
	{
		super(context, trackball);
	}
	
	@Override
	protected ULFToTextures createFromDirectory(String directoryPath) throws IOException
	{
		return new ULFToTextures(context, directoryPath, trackball);
	}
}
