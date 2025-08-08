package kintsugi3d.builder.javafx.experience;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.javafx.controllers.modals.MaskOptionsController;

import java.io.IOException;

public class MaskOptions extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "Mask Options";
    }

    @Override
    public void open() throws IOException
    {
        MaskOptionsController maskOptionsController = openModal("fxml/modals/MaskOptions.fxml");

        if (maskOptionsController != null && Global.state().getIOModel().hasValidHandler())
        {
            maskOptionsController.setProjectSettingsModel(Global.state().getIOModel().getLoadedViewSet().getProjectSettings());
        }
    }
}
