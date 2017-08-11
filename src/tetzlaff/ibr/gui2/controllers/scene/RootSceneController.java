package tetzlaff.ibr.gui2.controllers.scene;

import javafx.fxml.FXML;
import tetzlaff.ibr.gui2.controllers.scene.camera.RootCameraSceneController;
import tetzlaff.ibr.gui2.controllers.scene.environment_map.RootEVSceneController;
import tetzlaff.ibr.gui2.controllers.scene.lights.RootLightSceneController;
import tetzlaff.ibr.rendering2.CameraModelImp;
import tetzlaff.ibr.rendering2.EnvironmentMapModelImp;
import tetzlaff.ibr.rendering2.LightModelImp;
import tetzlaff.ibr.rendering2.ToolModelImp;

public class RootSceneController {


    @FXML
    private RootCameraSceneController cameraController;
    @FXML
    private RootLightSceneController lightsController;
    @FXML
    private RootEVSceneController environmentMapController;

    public void init2(CameraModelImp cameraModel, LightModelImp lightModel, EnvironmentMapModelImp environmentMapModel, ToolModelImp toolModel){
        cameraController.init2(cameraModel, toolModel);
        lightsController.init2(lightModel);
        environmentMapController.init2(environmentMapModel);
    }
}
