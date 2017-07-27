package tetzlaff.ibr.alexkautz_workspace.user_interface;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tetzlaff.ibr.alexkautz_workspace.mount_olympus.PassedParameters;
import tetzlaff.ibr.gui2.controllers.menu_bar.LoaderController;
import tetzlaff.ibr.rendering2.tools.Tool;

import java.io.IOException;

public class Base {

    @FXML private void launchLoadWindow(){
        System.out.println("Start Load");

        try {

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("LoaderController.fxml"));
            Parent root = fxmlLoader.load();
            Stage stage = new Stage();
            stage.setTitle("Load Window");
            stage.setScene(new Scene(root, 750, 330));

            LoaderController loaderController = fxmlLoader.getController();

            stage.initModality(Modality.APPLICATION_MODAL);

            stage.show();


        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("End Load");
    }

    public ToggleGroup paintBrush;

    public void changeToggle(){
        int toolIndex = paintBrush.getToggles().indexOf(paintBrush.getSelectedToggle());

        Tool newTool;
        switch (toolIndex){
            case 0: newTool = Tool.LOOK; break;
            case 1: newTool = Tool.DRAG; break;
            default: newTool = Tool.LOOK; break;
        }
        PassedParameters.get().getRenderPerams().getToolModel2().setTool(newTool);
    }
}
