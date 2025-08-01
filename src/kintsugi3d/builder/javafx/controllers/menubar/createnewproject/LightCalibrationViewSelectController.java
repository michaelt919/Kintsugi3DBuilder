/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ViewSet;

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
        ViewSet viewSet = Global.state().getIOModel().getLoadedViewSet();

        int viewIndex = viewSet.findIndexOfView(getSelectedViewName());
        if (viewIndex == viewSet.getPrimaryViewIndex())
        {
            // No change was made, continue to next page
            return true;
        }

        if (viewSet.hasCustomLuminanceEncoding())
        {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Change light calibration view? This will clear any previous tone calibration values!");
            Optional<ButtonType> confirmResult = alert.showAndWait();
            if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK)
            {
                // Stay on this page
                return false;
            }
        }

        viewSet.clearTonemapping();
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
