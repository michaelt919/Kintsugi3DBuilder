package tetzlaff.ulf;

import java.io.IOException;

import javax.swing.ComboBoxModel;

public interface ULFListModel extends ComboBoxModel<ULFDrawable>
{
	ULFDrawable addFromVSETFile(String vsetFile) throws IOException;
	ULFDrawable addMorphFromLFMFile(String lfmFile) throws IOException;
	ULFDrawable getSelectedItem();
	void setLoadingMonitor(ULFLoadingMonitor loadingMonitor);
}
