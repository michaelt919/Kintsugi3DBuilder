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
