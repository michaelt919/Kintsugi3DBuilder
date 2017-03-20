package tetzlaff.ibr;

import java.io.File;
import java.io.IOException;

import javax.swing.ComboBoxModel;

import tetzlaff.gl.Context;

public interface IBRListModel<ContextType extends Context<ContextType>> extends ComboBoxModel<IBRDrawable<ContextType>>
{
	IBRDrawable<ContextType> addFromVSETFile(File vsetFile, IBRLoadOptions loadOptions) throws IOException;
	IBRDrawable<ContextType> addFromAgisoftXMLFile(File xmlFile, File meshFile, IBRLoadOptions loadOptions) throws IOException;
	IBRDrawable<ContextType> addMorphFromLFMFile(File lfmFile, IBRLoadOptions loadOptions) throws IOException;
	@Override
	IBRDrawable<ContextType> getSelectedItem();
	void setLoadingMonitor(IBRLoadingMonitor loadingMonitor);
}
