package tetzlaff.ibr.gui2.controllers.scene;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import tetzlaff.ibr.app2.TheApp;
import tetzlaff.ibr.gui2.controllers.scene.camera.RootCameraSceneController;
import tetzlaff.ibr.gui2.controllers.scene.environment_map.RootEVSceneController;
import tetzlaff.ibr.gui2.controllers.scene.lights.RootLightSceneController;
import tetzlaff.ibr.rendering2.CameraModel3;
import tetzlaff.ibr.rendering2.LightModel3;

import java.net.URL;
import java.util.ResourceBundle;

public class RootSceneController implements Initializable{

    private final CameraModel3 cameraModel3 = TheApp.getRootModel().getCameraModel3();
    private final LightModel3 lightModel3 = TheApp.getRootModel().getLightModel3();

    @FXML
    RootCameraSceneController cameraController;
    @FXML
    RootLightSceneController lightsController;
    @FXML
    RootEVSceneController environmentMapController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        cameraController.init2(cameraModel3);
        lightsController.init2(lightModel3);

    }
}
