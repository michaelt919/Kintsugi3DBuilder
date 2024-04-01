package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.gl.vecmath.Vector2;

import java.net.URL;
import java.util.ResourceBundle;

public class LightCalibrationController implements Initializable
{
    @FXML
    private AnchorPane root;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
    }

    public void apply()
    {
        // Close window and the onCloseRequest will take care of the rest.
        Window window = root.getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }
}
