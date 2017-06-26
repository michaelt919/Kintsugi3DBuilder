package tetzlaff.ibr.alexkautz_workspace.user_interface;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Base {

    @FXML private void launchLoadWindow(){
        System.out.println("Start Load");



        try {

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("Loader.fxml"));
            Parent root = fxmlLoader.load();
            Stage stage = new Stage();
            stage.setTitle("Load Window");
            stage.setScene(new Scene(root, 750, 330));

            Loader loader = fxmlLoader.getController();


            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println("End Load");
    }
}
