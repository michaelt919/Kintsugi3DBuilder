package tetzlaff.ibr.gui2.controllers.scene;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import tetzlaff.ibr.app2.TheApp;
import tetzlaff.ibr.gui2.controllers.scene.camera.RootCameraSceneController;
import tetzlaff.ibr.gui2.controllers.scene.environment_map.RootEVSceneController;
import tetzlaff.ibr.gui2.controllers.scene.lights.RootLightSceneController;
import tetzlaff.ibr.rendering2.CameraModel2;
import tetzlaff.ibr.rendering2.LightModel2;
import tetzlaff.mvc.models.LightModel;

import java.net.URL;
import java.util.ResourceBundle;

public class RootSceneController implements Initializable{

    private final CameraModel2 cameraModel2 = TheApp.getRootModel().getCameraModel2();
    private final LightModel2 lightModel2 = TheApp.getRootModel().getLightModel2();

    @FXML
    RootCameraSceneController cameraController;
    @FXML
    RootLightSceneController lightsController;
    @FXML
    RootEVSceneController environmentMapController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        cameraController.init2(cameraModel2);
        lightsController.init2(lightModel2);

    }
}
