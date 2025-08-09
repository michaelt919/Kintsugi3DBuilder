package kintsugi3d.builder.javafx.experience;

import java.io.IOException;

public class SystemSettings extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "System Settings";
    }

    @Override
    public void open() throws IOException
    {
        openModal("fxml/modals/systemsettings/SystemSettings.fxml");
    }
}
