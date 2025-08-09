package kintsugi3d.builder.javafx.experience;

import kintsugi3d.builder.javafx.controllers.modals.createnewproject.HotSwapController;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.SelectImportOptionsController;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.paged.PageFrameController;
import kintsugi3d.builder.javafx.controllers.paged.SimpleDataSourcePage;
import kintsugi3d.builder.javafx.controllers.paged.SimpleDataTransformerPage;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;

import java.io.IOException;

public class CreateProject extends ExperienceBase
{
    private Runnable confirmCallback;

    @Override
    public String getName()
    {
        return "Create Project";
    }

    @Override
    public void open() throws IOException
    {
        PageFrameController controller = openPagedModel(
            "/fxml/modals/createnewproject/SelectImportOptions.fxml",
            SimpleDataSourcePage<InputSource, SelectImportOptionsController>::new);
        WelcomeWindowController.getInstance().hide();

        controller.setCancelCallback(WelcomeWindowController.getInstance()::showIfNoModelLoadedAndNotProcessing);
        controller.setPreConfirmCallback(WelcomeWindowController.getInstance()::hide);
        controller.setConfirmCallback(confirmCallback);
    }

    public void openHotSwap() throws IOException
    {
        PageFrameController controller = openPagedModel(
            "/fxml/modals/createnewproject/HotSwap.fxml",
            SimpleDataTransformerPage<InputSource, InputSource, HotSwapController>::new);
    }

    public void setConfirmCallback(Runnable confirmCallback)
    {
        this.confirmCallback = confirmCallback;
    }
}
