package kintsugi3d.builder.javafx.experience;

import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class Log extends ExperienceBase
{
    @Override
    public String getName()
    {
        return "Log";
    }

    @Override
    protected void open() throws IOException
    {
        createModal("fxml/modals/Logger.fxml");

        // Change a few settings before opening the modal.
        Stage stage = getModal().getStage();
        stage.setResizable(true);
        stage.initStyle(StageStyle.DECORATED);

        getModal().open();
    }
}
