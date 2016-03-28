package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.helpers.Trackball;

/**
 * An implementation for rendering a list of unstructured light fields that supports adding new light fields to the list as ULFRenderer objects.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the context that will be used for rendering.
 */
public class ULFRendererList<ContextType extends Context<ContextType>> extends ULFDrawableListModel<ContextType>
{
	private static final long serialVersionUID = -8199166231586786343L;

	/**
	 * Creates a new unstructured light field renderer list.
	 * @param context The GL context in which to perform the rendering.
	 * @param trackball The trackball controlling the movement of the virtual camera.
	 */
	public ULFRendererList(ContextType context, Trackball trackball) 
	{
		super(context, trackball);
	}
	
	@Override
	protected ULFRenderer<ContextType> createFromVSETFile(File vsetFile, ULFLoadOptions loadOptions) throws IOException
	{
		return new ULFRenderer<ContextType>(context, this.getProgram(), vsetFile, loadOptions, trackball);
	}
	
	@Override
	protected ULFRenderer<ContextType> createFromAgisoftXMLFile(File xmlFile, File meshFile, ULFLoadOptions loadOptions) throws IOException
	{
		return new ULFRenderer<ContextType>(context, this.getProgram(), xmlFile, meshFile, loadOptions, trackball);
	}
	
	@Override
	protected ULFMorphRenderer<ContextType> createMorphFromLFMFile(File lfmFile, ULFLoadOptions loadOptions) throws IOException
	{
		return new ULFMorphRenderer<ContextType>(context, this.getProgram(), lfmFile, loadOptions, trackball);
	}
}
