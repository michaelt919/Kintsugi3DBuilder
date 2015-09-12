package tetzlaff.imagebasedmicrofacet;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.ulf.ULFDrawableListModel;
import tetzlaff.ulf.ULFLoadOptions;

public class ImageBasedMicrofacetRendererList<ContextType extends Context<ContextType>> extends ULFDrawableListModel<ContextType>
{
	private static final long serialVersionUID = -8199166231586786343L;
	private Trackball lightTrackball;

	public ImageBasedMicrofacetRendererList(ContextType context, Program<ContextType> program, Program<ContextType> indexProgram, Trackball viewTrackball, Trackball lightTrackball) 
	{
		super(context, program, viewTrackball);
		this.lightTrackball = lightTrackball;
	}
	
	@Override
	protected ImageBasedMicrofacetRenderer<ContextType> createFromVSETFile(File vsetFile, ULFLoadOptions loadOptions) throws IOException
	{
		return new ImageBasedMicrofacetRenderer<ContextType>(context, this.getProgram(), this.getIndexProgram(), vsetFile, null, loadOptions, trackball, lightTrackball);
	}
	
	@Override
	protected ImageBasedMicrofacetRenderer<ContextType> createFromAgisoftXMLFile(File xmlFile, File meshFile, ULFLoadOptions loadOptions) throws IOException
	{
		return new ImageBasedMicrofacetRenderer<ContextType>(context, this.getProgram(), this.getIndexProgram(), xmlFile, meshFile, loadOptions, trackball, lightTrackball);
	}
	
	@Override
	protected ImageBasedMicrofacetRenderer<ContextType> createMorphFromLFMFile(File lfmFile, ULFLoadOptions loadOptions) throws IOException
	{
		throw new IllegalStateException("Morphs not supported for halfway field rendering.");
		//return new ULFMorphRenderer<ContextType>(context, this.getProgram(), lfmFile, loadOptions, trackball, lightTrackball);
	}
}
