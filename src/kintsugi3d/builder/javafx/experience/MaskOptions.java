package kintsugi3d.builder.javafx.experience;

import kintsugi3d.builder.javafx.controllers.modals.workflow.MaskOptionsController;

import java.io.IOException;

public class MaskOptions extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "Mask / Feather Options";
    }

    @Override
    protected void open() throws IOException
    {
        this.<MaskOptionsController>openPagedModel("/fxml/modals/MaskOptions.fxml");
    }
}
