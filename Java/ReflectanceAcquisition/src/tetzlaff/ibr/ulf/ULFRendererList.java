package tetzlaff.ibr.ulf;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.ibr.IBRDrawableListModel;
import tetzlaff.ibr.IBRLoadOptions;

public class ULFRendererList<ContextType extends Context<ContextType>> extends IBRDrawableListModel<ContextType>
{
	private static final long serialVersionUID = -8199166231586786343L;

	public ULFRendererList(ContextType context, Program<ContextType> program, Program<ContextType> indexProgram, CameraController cameraController) 
	{
		super(context, program, indexProgram, cameraController);
	}
	
	@Override
	protected ULFRenderer<ContextType> createFromVSETFile(File vsetFile, IBRLoadOptions loadOptions) throws IOException
	{
		return new ULFRenderer<ContextType>(context, this.getProgram(), null, vsetFile, null, loadOptions, cameraController);
	}
	
	@Override
	protected ULFRenderer<ContextType> createFromAgisoftXMLFile(File xmlFile, File meshFile, IBRLoadOptions loadOptions) throws IOException
	{
		return new ULFRenderer<ContextType>(context, this.getProgram(), null, xmlFile, meshFile, loadOptions, cameraController);
	}
	
	@Override
	protected ULFMorphRenderer<ContextType> createMorphFromLFMFile(File lfmFile, IBRLoadOptions loadOptions) throws IOException
	{
		return new ULFMorphRenderer<ContextType>(context, this.getProgram(), lfmFile, loadOptions, cameraController);
	}
}
