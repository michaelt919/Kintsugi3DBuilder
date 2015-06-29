package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.opengl.OpenGLContext;

public class ULFRendererList extends ULFDrawableListModel
{
	private static final long serialVersionUID = -8199166231586786343L;

	public ULFRendererList(OpenGLContext context, Trackball trackball) 
	{
		super(context, trackball);
	}
	
	@Override
	protected ULFRenderer createFromVSETFile(File vsetFile) throws IOException
	{
		return new ULFRenderer(context, vsetFile, trackball);
	}
	
	@Override
	protected ULFRenderer createFromAgisoftXMLFile(File xmlFile, File meshFile, File imageDirectory) throws IOException
	{
		return new ULFRenderer(context, xmlFile, meshFile, imageDirectory, trackball);
	}
	
	@Override
	protected ULFMorphRenderer createMorphFromLFMFile(File lfmFile) throws IOException
	{
		return new ULFMorphRenderer(context, lfmFile, trackball);
	}
}
