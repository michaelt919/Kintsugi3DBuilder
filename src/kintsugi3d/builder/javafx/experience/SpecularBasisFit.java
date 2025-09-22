package kintsugi3d.builder.javafx.experience;

import java.io.IOException;

public class SpecularBasisFit extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "Process Textures";
    }

    @Override
    protected void open() throws IOException
    {
        openPagedModel("/fxml/modals/workflow/SpecularBasisFit.fxml");
    }
}
