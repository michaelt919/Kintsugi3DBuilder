package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.helpers.Trackball;

public class ULFRendererList<ContextType extends Context<ContextType>> extends ULFDrawableListModel<ContextType>
{
	private static final long serialVersionUID = -8199166231586786343L;

	public ULFRendererList(ContextType context, Trackball trackball) 
	{
		super(context, trackball);
	}
	
	@Override
	protected ULFRenderer<ContextType> createFromVSETFile(File vsetFile) throws IOException
	{
		return new ULFRenderer<ContextType>(context, program, vsetFile, trackball);
	}
	
	@Override
	protected ULFRenderer<ContextType> createFromAgisoftXMLFile(File xmlFile, File meshFile, File imageDirectory) throws IOException
	{
		return new ULFRenderer<ContextType>(context, program, xmlFile, meshFile, imageDirectory, trackball);
	}
	
	@Override
	protected ULFMorphRenderer<ContextType> createMorphFromLFMFile(File lfmFile) throws IOException
	{
		return new ULFMorphRenderer<ContextType>(context, program, lfmFile, trackball);
	}
}
