package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import javax.swing.ComboBoxModel;

import tetzlaff.gl.Context;

public interface ULFListModel<ContextType extends Context<ContextType>> extends ComboBoxModel<ULFDrawable<ContextType>>
{
	ULFDrawable<ContextType> addFromVSETFile(File vsetFile, ULFLoadOptions loadOptions) throws IOException;
	ULFDrawable<ContextType> addFromAgisoftXMLFile(File xmlFile, File meshFile, ULFLoadOptions loadOptions) throws IOException;
	ULFDrawable<ContextType> addMorphFromLFMFile(File lfmFile, ULFLoadOptions loadOptions) throws IOException;
	@Override
	ULFDrawable<ContextType> getSelectedItem();
	void setLoadingMonitor(ULFLoadingMonitor loadingMonitor);
}
