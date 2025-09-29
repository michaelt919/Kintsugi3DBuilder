package kintsugi3d.builder.javafx.experience;

import kintsugi3d.builder.javafx.controllers.modals.viewselect.CurrentProjectViewSelectable;
import kintsugi3d.builder.javafx.controllers.modals.viewselect.ViewSelectable;
import kintsugi3d.builder.javafx.controllers.modals.workflow.tonecalibration.EyedropperController;
import kintsugi3d.builder.javafx.controllers.modals.workflow.tonecalibration.SelectToneCalibrationImageController;
import kintsugi3d.builder.javafx.controllers.modals.workflow.tonecalibration.ToneCalibrationViewSelectController;
import kintsugi3d.builder.javafx.controllers.paged.SimpleDataReceiverPage;

import java.io.IOException;

public class ToneCalibration extends ExperienceBase
{

    public static final String PRIMARY_VIEW_SELECT = "/fxml/modals/createnewproject/ViewSelect.fxml";
    public static final String SELECT_TONE_CALIBRATION_IMAGE = "/fxml/modals/workflow/SelectToneCalibrationImage.fxml";
    public static final String EYEDROPPER = "/fxml/modals/workflow/EyedropperColorChecker.fxml";

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
                SimpleDataReceiverPage<ViewSelectable, ToneCalibrationViewSelectController>::new,
                ToneCalibrationViewSelectController::new)
            .<SelectToneCalibrationImageController>thenNonData(SELECT_TONE_CALIBRATION_IMAGE)
            .<EyedropperController>then(EYEDROPPER)
            .finish()
            .setMinContentWidth(840)
            .setMinContentHeight(640);
    }
}
