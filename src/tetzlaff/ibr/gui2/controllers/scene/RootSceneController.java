package tetzlaff.ibr.gui2.controllers.scene;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.omg.PortableServer.THREAD_POLICY_ID;
import tetzlaff.ibr.app2.TheApp;
import tetzlaff.ibr.gui2.controllers.scene.camera.RootCameraSceneController;
import tetzlaff.ibr.gui2.controllers.scene.environment_map.RootEVSceneController;
import tetzlaff.ibr.gui2.controllers.scene.lights.RootLightSceneController;
import tetzlaff.ibr.rendering2.CameraModel3;
import tetzlaff.ibr.rendering2.EnvironmentMapModel3;
import tetzlaff.ibr.rendering2.LightModel3;
import tetzlaff.ibr.rendering2.ToolModel3;
import tetzlaff.mvc.controllers.CameraController;
import tetzlaff.mvc.models.LightModel;

public class RootSceneController {


    @FXML
    private RootCameraSceneController cameraController;
    @FXML
    private RootLightSceneController lightsController;
    @FXML
    private RootEVSceneController environmentMapController;

    public void init2(CameraModel3 cameraModel3, LightModel3 lightModel3, EnvironmentMapModel3 environmentMapModel3, ToolModel3 toolModel3){
        cameraController.init2(cameraModel3, toolModel3);
        lightsController.init2(lightModel3);
        environmentMapController.init2(environmentMapModel3);
    }
}
