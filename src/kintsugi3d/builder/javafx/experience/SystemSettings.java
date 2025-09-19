package kintsugi3d.builder.javafx.experience;

import kintsugi3d.builder.javafx.controllers.modals.systemsettings.SystemSettingsController;

import java.io.IOException;

public class SystemSettings extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "System Settings";
    }

    @Override
    protected void open() throws IOException
    {
        SystemSettingsController controller = openModal("/fxml/modals/systemsettings/SystemSettings.fxml");
        controller.initializeSettingsPages(getParentWindow(), getState());
    }
}
