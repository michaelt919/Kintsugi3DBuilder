package tetzlaff.ibr.javafx.controllers.scene;

import javafx.fxml.FXML;
import tetzlaff.ibr.javafx.controllers.scene.camera.RootCameraSceneController;
import tetzlaff.ibr.javafx.controllers.scene.environment_map.RootEVSceneController;
import tetzlaff.ibr.javafx.controllers.scene.lights.RootLightSceneController;
import tetzlaff.ibr.javafx.models.JavaFXCameraModel;
import tetzlaff.ibr.javafx.models.JavaFXEnvironmentMapModel;
import tetzlaff.ibr.javafx.models.JavaFXLightingModel;
import tetzlaff.ibr.rendering2.ToolModelImp;

public class RootSceneController {


    @FXML
    private RootCameraSceneController cameraController;
    @FXML
    private RootLightSceneController lightsController;
    @FXML
    private RootEVSceneController environmentMapController;

    public void init2(JavaFXCameraModel cameraModel, JavaFXLightingModel lightModel, JavaFXEnvironmentMapModel environmentMapModel, ToolModelImp toolModel){
        cameraController.init2(cameraModel, toolModel);
        lightsController.init2(lightModel);
        environmentMapController.init2(environmentMapModel);
    }
}
