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

public class RootSceneController implements Initializable{


    @FXML
    RootCameraSceneController cameraController;
    @FXML
    RootLightSceneController lightsController;
    @FXML
    RootEVSceneController environmentMapController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        cameraController.init2(cameraModel3);
        environmentMapController.init2(environmentMapModel3);
        lightsController.init2(lightModel3);

    }
}
