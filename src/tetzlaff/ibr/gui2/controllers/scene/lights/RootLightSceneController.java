package tetzlaff.ibr.gui2.controllers.scene.lights;//Created by alexk on 7/16/2017.

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class RootLightSceneController implements Initializable {
    @FXML private VBox settings;
    @FXML private SettingsLightSceneController settingsController;
    @FXML private TableView<LightGroupSetting> tableView;
    @FXML private VBox groupControls;
    @FXML private VBox lightControls;
    @FXML private Button theRenameButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("GLSC started!");
    }
}
