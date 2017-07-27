package tetzlaff.ibr;

import tetzlaff.gl.Context;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public interface IBRRenderableListModel<ContextType extends Context<ContextType>> extends ComboBoxModel<IBRRenderable<ContextType>>
{
	IBRRenderable<ContextType> addFromVSETFile(String id, File vsetFile, IBRLoadOptions loadOptions) throws IOException;
	IBRRenderable<ContextType> addFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, IBRLoadOptions loadOptions) throws IOException;
	@Override
	IBRRenderable<ContextType> getSelectedItem();
	void setLoadingMonitor(LoadingMonitor loadingMonitor);
}
