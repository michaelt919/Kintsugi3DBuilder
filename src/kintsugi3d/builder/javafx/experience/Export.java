package kintsugi3d.builder.javafx.experience;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.javafx.controllers.modals.ExportRequestController;

import java.io.File;
import java.io.IOException;

public class Export extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "Export glTF";
    }

    @Override
    public void open() throws IOException
    {
        ExportRequestController exportRequest = openModal("fxml/modals/export/ExportRequestUI.fxml");

        File loadedProjectFile = Global.state().getIOModel().getLoadedProjectFile();
        if (loadedProjectFile != null)
        {
            exportRequest.setCurrentDirectoryFile(loadedProjectFile.getParentFile());
        }
    }
}
