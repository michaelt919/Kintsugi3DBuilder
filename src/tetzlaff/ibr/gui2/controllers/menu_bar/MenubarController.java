package tetzlaff.ibr.gui2.controllers.menu_bar;

import javafx.fxml.Initializable;
import tetzlaff.ibr.app2.TheApp;
import tetzlaff.ibr.rendering2.tools.ToolModel2;

import java.net.URL;
import java.util.ResourceBundle;

public class MenubarController implements Initializable {

    private final ToolModel2 toolModel = TheApp.getRootModel().getToolModel2();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Menubar Controller Initialized.");
    }

    public void exit(){
        System.exit(0);
    }

}
