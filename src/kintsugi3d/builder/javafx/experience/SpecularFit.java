package kintsugi3d.builder.javafx.experience;

import java.io.IOException;

public class SpecularFit extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "Process Textures";
    }

    @Override
    public void open() throws IOException
    {
        openModal("fxml/modals/SpecularFit.fxml");
    }
}
