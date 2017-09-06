package tetzlaff.ibr.javafx.controllers.scene;

import javafx.fxml.FXML;
import tetzlaff.ibr.javafx.controllers.scene.camera.RootCameraSceneController;
import tetzlaff.ibr.javafx.controllers.scene.environment_map.RootEnvironmentSceneController;
import tetzlaff.ibr.javafx.controllers.scene.lights.RootLightSceneController;
import tetzlaff.ibr.javafx.controllers.scene.object.RootObjectSceneController;
import tetzlaff.ibr.javafx.models.JavaFXCameraModel;
import tetzlaff.ibr.javafx.models.JavaFXEnvironmentMapModel;
import tetzlaff.ibr.javafx.models.JavaFXLightingModel;
import tetzlaff.ibr.javafx.models.JavaFXObjectModel;

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

    public RootSceneController()
    {
    }


    public void init(JavaFXCameraModel cameraModel, JavaFXLightingModel lightingModel, JavaFXEnvironmentMapModel environmentMapModel, JavaFXObjectModel objectModel)
    {
        cameraController.init(cameraModel);
        lightsController.init(lightingModel);
        environmentMapController.init(environmentMapModel);
        objectPosesController.init(objectModel);
    }
}
