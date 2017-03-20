package tetzlaff.ibr.microfacet;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.LightController;
import tetzlaff.ibr.IBRDrawableListModel;
import tetzlaff.ibr.IBRLoadOptions;

public class ImageBasedMicrofacetRendererList<ContextType extends Context<ContextType>> extends IBRDrawableListModel<ContextType>
{
	private static final long serialVersionUID = -8199166231586786343L;
	
	private LightController lightController;
	private Program<ContextType> shadowProgram;

	public ImageBasedMicrofacetRendererList(ContextType context, Program<ContextType> program, Program<ContextType> indexProgram, Program<ContextType> shadowProgram, CameraController cameraController, LightController lightController) 
	{
		super(context, program, indexProgram, cameraController);
		this.lightController = lightController;
		this.shadowProgram = shadowProgram;
	}
	
	@Override
	protected ImageBasedMicrofacetRenderer<ContextType> createFromVSETFile(File vsetFile, IBRLoadOptions loadOptions) throws IOException
	{
		return new ImageBasedMicrofacetRenderer<ContextType>(context, this.getProgram(), this.getIndexProgram(), this.shadowProgram, vsetFile, null, loadOptions, this.getCameraController(), lightController);
	}
	
	@Override
	protected ImageBasedMicrofacetRenderer<ContextType> createFromAgisoftXMLFile(File xmlFile, File meshFile, IBRLoadOptions loadOptions) throws IOException
	{
		return new ImageBasedMicrofacetRenderer<ContextType>(context, this.getProgram(), this.getIndexProgram(), this.shadowProgram, xmlFile, meshFile, loadOptions, this.getCameraController(), lightController);
	}
	
	@Override
	protected ImageBasedMicrofacetRenderer<ContextType> createMorphFromLFMFile(File lfmFile, IBRLoadOptions loadOptions) throws IOException
	{
		throw new IllegalStateException("Morphs not supported for halfway field rendering.");
		//return new ULFMorphRenderer<ContextType>(context, this.getProgram(), lfmFile, loadOptions, trackballs);
	}
}
