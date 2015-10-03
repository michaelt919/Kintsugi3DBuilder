package tetzlaff.imagebasedmicrofacet;

import java.io.File;
import java.io.IOException;
import java.util.List;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.ulf.ULFDrawableListModel;
import tetzlaff.ulf.ULFLoadOptions;

public class ImageBasedMicrofacetRendererList<ContextType extends Context<ContextType>> extends ULFDrawableListModel<ContextType> implements TrackballLightModel
{
	private static final long serialVersionUID = -8199166231586786343L;
	private List<Trackball> trackballs;

	public ImageBasedMicrofacetRendererList(ContextType context, Program<ContextType> program, Program<ContextType> indexProgram, List<Trackball> trackballs) 
	{
		super(context, program, trackballs.get(0));
		this.trackballs = trackballs;
	}
	
	@Override
	protected ImageBasedMicrofacetRenderer<ContextType> createFromVSETFile(File vsetFile, ULFLoadOptions loadOptions) throws IOException
	{
		return new ImageBasedMicrofacetRenderer<ContextType>(context, this.getProgram(), this.getIndexProgram(), vsetFile, null, loadOptions, trackballs);
	}
	
	@Override
	protected ImageBasedMicrofacetRenderer<ContextType> createFromAgisoftXMLFile(File xmlFile, File meshFile, ULFLoadOptions loadOptions) throws IOException
	{
		return new ImageBasedMicrofacetRenderer<ContextType>(context, this.getProgram(), this.getIndexProgram(), xmlFile, meshFile, loadOptions, trackballs);
	}
	
	@Override
	protected ImageBasedMicrofacetRenderer<ContextType> createMorphFromLFMFile(File lfmFile, ULFLoadOptions loadOptions) throws IOException
	{
		throw new IllegalStateException("Morphs not supported for halfway field rendering.");
		//return new ULFMorphRenderer<ContextType>(context, this.getProgram(), lfmFile, loadOptions, trackballs);
	}

	@Override
	public int getActiveTrackball() 
	{
		if (this.getSelectedItem() != null && this.getSelectedItem() instanceof TrackballLightModel)
		{
			return ((TrackballLightModel)this.getSelectedItem()).getActiveTrackball();
		}
		else
		{
			return -1;
		}
	}

	@Override
	public void setActiveTrackball(int index) 
	{
		if (this.getSelectedItem() != null && this.getSelectedItem() instanceof TrackballLightModel)
		{
			((TrackballLightModel)this.getSelectedItem()).setActiveTrackball(index);
		}
	}

	@Override
	public Vector3 getActiveLightColor() 
	{
		if (this.getSelectedItem() != null && this.getSelectedItem() instanceof TrackballLightModel)
		{
			return ((TrackballLightModel)this.getSelectedItem()).getActiveLightColor();
		}
		else
		{
			return new Vector3(0.0f, 0.0f, 0.0f);
		}
	}

	@Override
	public void setActiveLightColor(Vector3 color) 
	{
		if (this.getSelectedItem() != null && this.getSelectedItem() instanceof TrackballLightModel)
		{
			((TrackballLightModel)this.getSelectedItem()).setActiveLightColor(color);
		}
	}
}
