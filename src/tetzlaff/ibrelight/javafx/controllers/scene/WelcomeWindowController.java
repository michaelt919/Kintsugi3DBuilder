package tetzlaff.ibrelight.javafx.controllers.scene;

import javafx.fxml.Initializable;
import tetzlaff.ibrelight.javafx.controllers.menubar.MenubarController;

import java.net.URL;
import java.util.ResourceBundle;

public class WelcomeWindowController extends MenubarController implements Initializable {
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("successfully initialized!");
    }


}
