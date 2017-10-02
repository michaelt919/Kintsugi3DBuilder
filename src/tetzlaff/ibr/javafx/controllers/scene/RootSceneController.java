package tetzlaff.ibr.javafx.controllers.scene;

import javafx.fxml.FXML;
import tetzlaff.ibr.javafx.backend.JavaFXCameraModel;
import tetzlaff.ibr.javafx.backend.JavaFXEnvironmentModel;
import tetzlaff.ibr.javafx.backend.JavaFXLightingModel;
import tetzlaff.ibr.javafx.backend.JavaFXObjectModel;
import tetzlaff.ibr.javafx.controllers.scene.camera.RootCameraSceneController;
import tetzlaff.ibr.javafx.controllers.scene.environment.RootEnvironmentSceneController;
import tetzlaff.ibr.javafx.controllers.scene.lights.RootLightSceneController;
import tetzlaff.ibr.javafx.controllers.scene.object.RootObjectSceneController;

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

    public void init(JavaFXCameraModel cameraModel, JavaFXLightingModel lightingModel, JavaFXEnvironmentModel environmentMapModel,
        JavaFXObjectModel objectModel, SceneModel sceneModel)
    {
        cameraController.init(cameraModel, sceneModel);
        lightsController.init(lightingModel, sceneModel);
        environmentMapController.init(environmentMapModel, sceneModel);
        objectPosesController.init(objectModel, sceneModel);
    }
}
