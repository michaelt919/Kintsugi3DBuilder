/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.scene;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;


import java.io.IOException;

public class SettingsReviewController {
    private AnchorPane frame;

    public void skipOnboarding(ActionEvent actionEvent) {
        //TODO: imp., probably just close onboarding windows and open other Kintsugi windows
    }

    public void continueProjectCreation(){
        Parent newContent = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/scene/ImportData.fxml"));
            newContent = loader.load();

            //initialize controller
            ImportDataController controller = loader.getController();
            controller.initHost(frame);

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (newContent != null) {
            frame.getChildren().setAll(newContent);
        }
    }


    public void initHost(AnchorPane frame) {
        this.frame = frame;
    }
}
