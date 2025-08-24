package kintsugi3d.builder.javafx.experience;

import kintsugi3d.builder.javafx.controllers.modals.createnewproject.*;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.ManualInputSource;
import kintsugi3d.builder.javafx.controllers.paged.SimpleDataSourcePage;

import java.io.IOException;

public class CreateProject extends ExperienceBase
{
    private static final String METASHAPE_IMPORT = "/fxml/modals/createnewproject/MetashapeImport.fxml";
    public static final String MANUAL_IMPORT = "/fxml/modals/createnewproject/ManualImport.fxml";
    private static final String MASKS_IMPORT = "/fxml/modals/createnewproject/MasksImport.fxml";
    public static final String PRIMARY_VIEW_SELECT = "/fxml/modals/createnewproject/PrimaryViewSelect.fxml";

    private Runnable confirmCallback;

    @Override
    public String getName()
    {
        return "Create Project";
    }

    @Override
    protected void open() throws IOException
    {
        var masks = buildPagedModal()
            .thenSelect("Select Import Option")
            .choice("Metashape", METASHAPE_IMPORT, SimpleDataSourcePage<InputSource, MetashapeImportController>::new)
                .<MasksImportController>then(MASKS_IMPORT);

            masks.<OrientationViewSelectController>then(PRIMARY_VIEW_SELECT)
                .finish()
            .choice("Reality Capture", MANUAL_IMPORT, SimpleDataSourcePage<ManualInputSource, RealityCaptureImportController>::new,
                    RealityCaptureImportController::new)
                .join(masks.getPage())
            .choice("Manual", MANUAL_IMPORT, SimpleDataSourcePage<ManualInputSource, ManualImportController>::new)
                .join(masks.getPage())
            .finish()
            .setConfirmCallback(confirmCallback);
    }

    private void openHotSwap() throws IOException
    {
        buildPagedModal()
            .then(MANUAL_IMPORT, SimpleDataSourcePage<ManualInputSource, HotSwapController>::new, HotSwapController::new)
            .<MasksImportController>then(MASKS_IMPORT)
            .<OrientationViewSelectController>then(PRIMARY_VIEW_SELECT)
            .finish()
            .setConfirmCallback(confirmCallback);
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
