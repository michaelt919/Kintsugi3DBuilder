package tetzlaff.ulf;

import java.io.File;
import java.io.IOException;

import javax.swing.ComboBoxModel;

public interface ULFListModel extends ComboBoxModel<ULFDrawable>
{
	ULFDrawable addFromVSETFile(File vsetFile) throws IOException;
	ULFDrawable addFromAgisoftXMLFile(File xmlFile, File meshFile, File imageDirectory) throws IOException;
	ULFDrawable addMorphFromLFMFile(File lfmFile) throws IOException;
	@Override
	ULFDrawable getSelectedItem();
	void setLoadingMonitor(ULFLoadingMonitor loadingMonitor);
}
