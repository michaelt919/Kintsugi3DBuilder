package tetzlaff.ibr;

import java.io.File;
import java.io.IOException;

import javax.swing.ComboBoxModel;

import tetzlaff.gl.Context;

public interface IBRRenderableListModel<ContextType extends Context<ContextType>> extends ComboBoxModel<IBRRenderable<ContextType>>
{
	IBRRenderable<ContextType> addFromVSETFile(String id, File vsetFile, IBRLoadOptions loadOptions) throws IOException;
	IBRRenderable<ContextType> addFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, IBRLoadOptions loadOptions) throws IOException;
	@Override
	IBRRenderable<ContextType> getSelectedItem();
	void setLoadingMonitor(LoadingMonitor loadingMonitor);
}
