package tetzlaff.ibr;

import java.io.File;
import java.io.IOException;

import javax.swing.ComboBoxModel;

import tetzlaff.gl.Context;

public interface IBRDrawableListModel<ContextType extends Context<ContextType>> extends ComboBoxModel<IBRDrawable<ContextType>>
{
	IBRDrawable<ContextType> addFromVSETFile(String id, File vsetFile, IBRLoadOptions loadOptions) throws IOException;
	IBRDrawable<ContextType> addFromAgisoftXMLFile(String id, File xmlFile, File meshFile, File undistortedImageDirectory, IBRLoadOptions loadOptions) throws IOException;
	@Override
	IBRDrawable<ContextType> getSelectedItem();
	void setLoadingMonitor(IBRLoadingMonitor loadingMonitor);
}
