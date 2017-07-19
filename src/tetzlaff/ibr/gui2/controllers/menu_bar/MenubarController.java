package tetzlaff.ibr.gui2.controllers.menu_bar;

import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class MenubarController implements Initializable {

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Menubar Controller Initialized.");
    }

    public void exit(){
        System.exit(0);
    }

}
