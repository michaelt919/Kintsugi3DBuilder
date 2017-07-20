package tetzlaff.ibr.gui2.controllers.menu_bar;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.ibr.app2.TheApp;
import tetzlaff.ibr.rendering.ImageBasedRendererList;
import tetzlaff.ibr.rendering2.tools.ToolModel2;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MenubarController implements Initializable {

    private final ToolModel2 toolModel = TheApp.getRootModel().getToolModel2();
    private ImageBasedRendererList<OpenGLContext> model;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Menubar Controller Initialized.");
    }

    @FXML private void loadMenu(){
        if(model == null){
            model = toolModel.getModel();
            if(model == null) return;
        }

        try {
            URL url = getClass().getClassLoader().getResource("fxml/menu_bar/Loader.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(url);
            Parent root =fxmlLoader.load();
            Stage stage = new Stage();
            stage.setTitle("Load Window");
            stage.setScene(new Scene(root, 750, 330));

            LoaderController loaderController = fxmlLoader.getController();

            loaderController.setModel(model);

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void exit(){
        System.exit(0);
    }

}
