package kintsugi3d.builder.javafx.experience;

import kintsugi3d.builder.javafx.controllers.modals.workflow.ExportModelController;

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
        this.<ExportModelController>openPagedModel("/fxml/modals/ExportModel.fxml");
    }
}
