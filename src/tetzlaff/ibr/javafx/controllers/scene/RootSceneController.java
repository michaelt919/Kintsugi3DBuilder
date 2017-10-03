package tetzlaff.ibr.javafx.controllers.scene;

import javafx.fxml.FXML;
import tetzlaff.ibr.javafx.controllers.scene.camera.RootCameraSceneController;
import tetzlaff.ibr.javafx.controllers.scene.environment.RootEnvironmentSceneController;
import tetzlaff.ibr.javafx.controllers.scene.lights.RootLightSceneController;
import tetzlaff.ibr.javafx.controllers.scene.object.RootObjectSceneController;
import tetzlaff.ibr.javafx.internal.CameraModelImpl;
import tetzlaff.ibr.javafx.internal.EnvironmentModelImpl;
import tetzlaff.ibr.javafx.internal.LightingModelImpl;
import tetzlaff.ibr.javafx.internal.ObjectModelImpl;

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
        ObjectModelImpl objectModel, SceneModel sceneModel)
    {
        cameraController.init(cameraModel, sceneModel);
        lightsController.init(lightingModel, sceneModel);
        environmentMapController.init(environmentMapModel, sceneModel);
        objectPosesController.init(objectModel, sceneModel);
    }
}
