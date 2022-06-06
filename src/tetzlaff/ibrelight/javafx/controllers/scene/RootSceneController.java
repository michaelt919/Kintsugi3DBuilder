/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx.controllers.scene;

import javafx.fxml.FXML;
import tetzlaff.ibrelight.javafx.controllers.scene.camera.RootCameraSceneController;
import tetzlaff.ibrelight.javafx.controllers.scene.environment.RootEnvironmentSceneController;
import tetzlaff.ibrelight.javafx.controllers.scene.lights.RootLightSceneController;
import tetzlaff.ibrelight.javafx.controllers.scene.object.RootObjectSceneController;
import tetzlaff.ibrelight.javafx.internal.*;
import tetzlaff.models.SceneViewportModel;

public class RootSceneController
{
    @FXML
    private RootCameraSceneController cameraController;
    @FXML
    private RootLightSceneController lightsController;
    @FXML
    private RootEnvironmentSceneController environmentMapController;
    @FXML
    private RootObjectSceneController objectPosesController;

    public void init(CameraModelImpl cameraModel, LightingModelImpl lightingModel, EnvironmentModelImpl environmentMapModel,
        ObjectModelImpl objectModel, SettingsModelImpl settingsModel, SceneModel sceneModel, SceneViewportModel sceneViewportModel)
    {
        cameraController.init(cameraModel, sceneModel);
        lightsController.init(lightingModel, sceneModel, sceneViewportModel);
        environmentMapController.init(environmentMapModel, sceneModel);
        objectPosesController.init(objectModel, sceneModel);
    }
}
