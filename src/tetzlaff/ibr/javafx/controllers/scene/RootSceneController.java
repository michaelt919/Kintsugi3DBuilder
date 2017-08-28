package tetzlaff.ibr.javafx.controllers.scene;

import javafx.fxml.FXML;
import tetzlaff.ibr.javafx.controllers.scene.camera.RootCameraSceneController;
import tetzlaff.ibr.javafx.controllers.scene.environment_map.RootEnvironmentSceneController;
import tetzlaff.ibr.javafx.controllers.scene.lights.RootLightSceneController;
import tetzlaff.ibr.javafx.models.JavaFXCameraModel;
import tetzlaff.ibr.javafx.models.JavaFXEnvironmentMapModel;
import tetzlaff.ibr.javafx.models.JavaFXLightingModel;
import tetzlaff.ibr.javafx.models.JavaFXToolBindingModel;

public class RootSceneController
{

    @FXML
    private RootCameraSceneController cameraController;
    @FXML
    private RootLightSceneController lightsController;
    @FXML
    private RootEnvironmentSceneController environmentMapController;

    public void init(JavaFXCameraModel cameraModel, JavaFXLightingModel lightingModel, JavaFXEnvironmentMapModel environmentMapModel, JavaFXToolBindingModel toolModel)
    {
        cameraController.init(cameraModel, toolModel);
        lightsController.init(lightingModel);
        environmentMapController.init(environmentMapModel);
    }
}
