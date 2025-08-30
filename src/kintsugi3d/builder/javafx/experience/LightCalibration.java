package kintsugi3d.builder.javafx.experience;

import kintsugi3d.builder.javafx.controllers.modals.LightCalibrationController;

import java.io.IOException;

public class LightCalibration extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "Light Calibration";
    }

    @Override
    protected void open() throws IOException
    {
        this.<LightCalibrationController>openPagedModel("/fxml/modals/LightCalibration.fxml");
    }
}
