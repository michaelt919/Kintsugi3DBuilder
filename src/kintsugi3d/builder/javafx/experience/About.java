package kintsugi3d.builder.javafx.experience;

import java.io.IOException;

public class About extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "About Kintsugi 3D Builder";
    }

    @Override
    protected void open() throws IOException
    {
        openModal("fxml/modals/About.fxml");
    }
}
