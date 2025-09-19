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

package kintsugi3d.builder.javafx.controllers.scene;

import javafx.fxml.FXML;
import kintsugi3d.builder.javafx.controllers.scene.camera.RootCameraSceneController;
import kintsugi3d.builder.javafx.controllers.scene.environment.RootEnvironmentSceneController;
import kintsugi3d.builder.javafx.controllers.scene.lights.RootLightSceneController;
import kintsugi3d.builder.javafx.controllers.scene.object.RootObjectSceneController;
import kintsugi3d.builder.javafx.internal.*;
import kintsugi3d.builder.state.SceneViewportModel;

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

    public void init(ObservableCameraModel cameraModel, ObservableLightingEnvironmentModel lightingModel, ObservableEnvironmentModel environmentMapModel,
                     ObservableObjectPoseModel objectModel, ObservableProjectModel projectModel, SceneViewportModel sceneViewportModel)
    {
        cameraController.init(cameraModel, projectModel);
        lightsController.init(lightingModel, projectModel, sceneViewportModel);
        environmentMapController.init(environmentMapModel, projectModel);
        objectPosesController.init(objectModel, projectModel);
    }
}
