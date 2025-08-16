package kintsugi3d.builder.javafx.experience;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.javafx.controllers.modals.ExportModelController;

import java.io.File;
import java.io.IOException;

public class ExportModel extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "Export glTF";
    }

    @Override
    protected void open() throws IOException
    {
        ExportModelController exportRequest = openModal("fxml/modals/ExportModel.fxml");

        File loadedProjectFile = Global.state().getIOModel().getLoadedProjectFile();
        if (loadedProjectFile != null)
        {
            exportRequest.setCurrentDirectoryFile(loadedProjectFile.getParentFile());
        }
    }
}
