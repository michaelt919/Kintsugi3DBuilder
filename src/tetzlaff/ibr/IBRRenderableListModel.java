package tetzlaff.ibr;

import java.io.File;
import java.io.IOException;

import javax.swing.ComboBoxModel;

import tetzlaff.gl.Context;
import tetzlaff.ibr.rendering2.to_sort.IBRLoadOptions2;

public interface IBRRenderableListModel<ContextType extends Context<ContextType>> extends ComboBoxModel<IBRRenderable<ContextType>>
{
	IBRRenderable<ContextType> addFromVSETFile(String id, File vsetFile, IBRLoadOptions loadOptions) throws IOException;
	IBRRenderable<ContextType> addFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, IBRLoadOptions2 loadOptions) throws IOException;
	@Override
	IBRRenderable<ContextType> getSelectedItem();
	void setLoadingMonitor(LoadingMonitor loadingMonitor);
}
