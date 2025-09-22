package kintsugi3d.builder.javafx.experience;

import java.io.IOException;

public class SpecularTextureFit extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "Reoptimize Textures";
    }

    @Override
    protected void open() throws IOException
    {
        openPagedModel("/fxml/modals/workflow/SpecularTexturesFit.fxml");
    }
}
