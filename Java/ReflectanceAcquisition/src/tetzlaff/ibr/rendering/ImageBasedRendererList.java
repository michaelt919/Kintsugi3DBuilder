package tetzlaff.ibr.rendering;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.Program;
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.LightController;
import tetzlaff.ibr.IBRDrawableListModel;
import tetzlaff.ibr.IBRLoadOptions;

public class ImageBasedRendererList<ContextType extends Context<ContextType>> extends IBRDrawableListModel<ContextType>
{
	private static final long serialVersionUID = -8199166231586786343L;
	
	private LightController lightController;

	public ImageBasedRendererList(ContextType context, Program<ContextType> program, CameraController cameraController, LightController lightController) 
	{
		super(context, program, cameraController);
		this.lightController = lightController;
	}
	
	@Override
	protected ImageBasedRenderer<ContextType> createFromVSETFile(File vsetFile, IBRLoadOptions loadOptions) throws IOException
	{
		return new ImageBasedRenderer<ContextType>(context, this.getProgram(), vsetFile, null, loadOptions, this.getCameraController(), lightController);
	}
	
	@Override
	protected ImageBasedRenderer<ContextType> createFromAgisoftXMLFile(File xmlFile, File meshFile, IBRLoadOptions loadOptions) throws IOException
	{
		return new ImageBasedRenderer<ContextType>(context, this.getProgram(), xmlFile, meshFile, loadOptions, this.getCameraController(), lightController);
	}
}
