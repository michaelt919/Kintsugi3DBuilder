package kintsugi3d.builder.javafx.experience;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.controllers.main.MainWindowController;
import kintsugi3d.builder.javafx.controllers.modals.LightCalibrationController;
import kintsugi3d.builder.javafx.internal.SettingsModelImpl;

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
        LightCalibrationController lightCalibrationController = openModal("fxml/modals/LightCalibration.fxml");

        if (lightCalibrationController != null)
        {
            Stage stage = getModal().getStage();
            SettingsModelImpl settingsModel = getState().getSettingsModel();

            getModal().getStage().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e ->
            {
                Global.state().getIOModel().applyLightCalibration();
                Global.state().getSettingsModel().set("lightCalibrationMode", false);
                MainWindowController.getInstance().setShaderNameVisibility(Global.state().getIOModel().hasValidHandler());
            });

            // Must wait until the controllers is created to add this additional window close event handler.
            stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST,
                e -> lightCalibrationController.unbind(settingsModel));

            // Bind controller to settings model to synchronize with "currentLightCalibration".
            lightCalibrationController.bind(settingsModel);

            if (Global.state().getIOModel().hasValidHandler())
            {
                // Set the "currentLightCalibration" to the existing calibration values in the view set.
                ViewSet loadedViewSet = Global.state().getIOModel().getLoadedViewSet();

                settingsModel.set("currentLightCalibration",
                    loadedViewSet.getLightPosition(loadedViewSet.getLightIndex(0)).getXY());
            }

            MainWindowController.getInstance().getCameraViewListController().rebindSearchableListView();

            // Enables light calibration mode when the window is opened.
            settingsModel.set("lightCalibrationMode", true);

            //shader name doesn't change like it should when opening light calibration, so hide it for now
            //TODO: figure out how to show the correct name instead of hiding the text?
            MainWindowController.getInstance().setShaderNameVisibility(false);
        }
    }
}
