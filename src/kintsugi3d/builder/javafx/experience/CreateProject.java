package kintsugi3d.builder.javafx.experience;

import kintsugi3d.builder.javafx.controllers.modals.createnewproject.HotSwapController;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.SelectImportOptionsController;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.paged.PageFrameController;
import kintsugi3d.builder.javafx.controllers.paged.SimpleDataSourcePage;
import kintsugi3d.builder.javafx.controllers.paged.SimpleNonDataPage;

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
    protected void open() throws IOException
    {
        PageFrameController controller = openPagedModel(
            "/fxml/modals/createnewproject/SelectImportOptions.fxml",
            SimpleNonDataPage<SelectImportOptionsController>::new);

        controller.setConfirmCallback(confirmCallback);
    }

    private void openHotSwap() throws IOException
    {
        PageFrameController controller = openPagedModel(
            "/fxml/modals/createnewproject/ManualImport.fxml",
            SimpleDataSourcePage<InputSource, HotSwapController>::new,
            HotSwapController::new);

        controller.setConfirmCallback(confirmCallback);
    }

    public void tryOpenHotSwap()
    {
        if (!getModal().isOpen())
        {
            try
            {
                openHotSwap();
            }
            catch (Exception e)
            {
                handleError(e);
            }
        }
    }

    public void setConfirmCallback(Runnable confirmCallback)
    {
        this.confirmCallback = confirmCallback;
    }
}
