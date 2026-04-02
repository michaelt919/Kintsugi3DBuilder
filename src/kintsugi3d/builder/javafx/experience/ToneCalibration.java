/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

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
