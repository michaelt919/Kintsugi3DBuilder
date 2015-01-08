package tetzlaff.ulf;

import java.io.IOException;

import javax.swing.ComboBoxModel;

import tetzlaff.helpers.Selectable;

public interface ULFListModel extends ComboBoxModel<UnstructuredLightField>
{
	UnstructuredLightField addFromDirectory(String directoryPath) throws IOException;
	UnstructuredLightField getSelectedItem();
}
