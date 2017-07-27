package tetzlaff.ibr.gui2.controllers.menu_bar;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import tetzlaff.ibr.app2.TheApp;
import tetzlaff.ibr.rendering2.ToolModel3;
import tetzlaff.ibr.rendering2.tools2.ToolBox;

public class MenubarController implements Initializable {

    @FXML private ToggleGroup toolGroup;

    private final ToolModel3 toolModel = TheApp.getRootModel().getToolModel3();

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        toolGroup.selectedToggleProperty().addListener((ob,o,n)->{
            if(n != null){
                int indexInToolList = toolGroup.getToggles().indexOf(n);
                switch (indexInToolList){
                    case 0: toolModel.setTool(ToolBox.TOOL.ORBIT); return;
                    case 1: toolModel.setTool(ToolBox.TOOL.PAN); return;
                    case 2: toolModel.setTool(ToolBox.TOOL.DOLLY); return;
                    default: toolModel.setTool(ToolBox.TOOL.ORBIT);
                }
            }
        });

    }

    @FXML private void loadMenu(){
        try {
            URL url = getClass().getClassLoader().getResource("fxml/menu_bar/Loader.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(url);
            Parent root =fxmlLoader.load();
            Stage stage = new Stage();
            stage.setTitle("Load Window");
            stage.setScene(new Scene(root, 750, 330));

            LoaderController loaderController = fxmlLoader.getController();
            loaderController.setToolModel3(toolModel);

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void exit(){
        System.exit(0);
    }

}
