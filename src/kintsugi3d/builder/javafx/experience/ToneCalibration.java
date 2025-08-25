package kintsugi3d.builder.javafx.experience;

import kintsugi3d.builder.javafx.controllers.modals.EyedropperController;
import kintsugi3d.builder.javafx.controllers.modals.LightCalibrationViewSelectController;
import kintsugi3d.builder.javafx.controllers.modals.SelectToneCalibrationImageController;
import kintsugi3d.builder.javafx.controllers.modals.viewselect.CurrentProjectViewSelectable;
import kintsugi3d.builder.javafx.controllers.modals.viewselect.ViewSelectable;
import kintsugi3d.builder.javafx.controllers.paged.SimpleDataReceiverPage;

import java.io.IOException;

public class ToneCalibration extends ExperienceBase
{

    public static final String PRIMARY_VIEW_SELECT = "/fxml/modals/createnewproject/PrimaryViewSelect.fxml";
    public static final String SELECT_TONE_CALIBRATION_IMAGE = "/fxml/modals/SelectToneCalibrationImage.fxml";
    public static final String EYEDROPPER = "/fxml/modals/EyedropperColorChecker.fxml";

    @Override
    public String getName()
    {
        return "Tone Calibration";
    }

    @Override
    protected void open() throws IOException
    {
        this.buildPagedModal(new CurrentProjectViewSelectable())
            .then(PRIMARY_VIEW_SELECT,
                SimpleDataReceiverPage<ViewSelectable, LightCalibrationViewSelectController>::new,
                LightCalibrationViewSelectController::new)
            .<SelectToneCalibrationImageController>thenNonData(SELECT_TONE_CALIBRATION_IMAGE)
            .<EyedropperController>then(EYEDROPPER)
            .finish();
    }
}
