package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.MultithreadModels;

import java.util.Optional;

public class LightCalibrationViewSelectController extends PrimaryViewSelectController
{
    @Override
    public boolean canConfirm()
    {
        return false;
    }

    @Override
    public boolean nextButtonPressed()
    {
        ViewSet viewSet = MultithreadModels.getInstance().getIOModel().getLoadedViewSet();

        int viewIndex = viewSet.findIndexOfView(getSelectedViewName());
        if (viewIndex == viewSet.getPrimaryViewIndex())
        {
            // No change was made, continue to next page
            return true;
        }

        if (true/*tone calibration values are not cleared*/) //TODO
        {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Change light calibration view? This will clear any previous tone calibration values!");
            Optional<ButtonType> confirmResult = alert.showAndWait();
            if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK)
            {
                // Stay on this page
                return false;
            }
        }

        //TODO: Clear tone calibration values
        viewSet.setPrimaryViewIndex(viewIndex);
        return true;
    }

    @Override
    protected String getHintText()
    {
        return "Select light calibration view";
    }

    @Override
    protected boolean showFixOrientation()
    {
        return false;
    }
}
