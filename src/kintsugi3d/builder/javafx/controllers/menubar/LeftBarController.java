package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class LeftBarController {

    @FXML
    public RadioButton TexturesButton, CamerasButton;

    @FXML
    public VBox textureTab, cameraTab;

    @FXML
    private void chooseTabAction(ActionEvent event) throws IOException {
        textureTab.setVisible(TexturesButton.isSelected());
        textureTab.setManaged(TexturesButton.isSelected());
        cameraTab.setVisible(CamerasButton.isSelected());
        cameraTab.setManaged(CamerasButton.isSelected());
    }

}