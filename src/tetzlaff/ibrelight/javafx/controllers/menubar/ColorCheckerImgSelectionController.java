package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tetzlaff.ibrelight.core.LoadingModel;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class ColorCheckerImgSelectionController {
    public Button runButton;
    public TextField colorPickerSetField;

    private File selectedFile;

    private Stage stage;
    private Scene scene;
    private Parent root;

    private LoadingModel loadingModel;

    public void openEyedropper(ActionEvent actionEvent) throws IOException {
        if(SharedDataModel.getInstance().getSelectedImage()!= null) {//only move on if image is selected
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/menubar/EyedropperColorChecker.fxml"));
            root = fxmlLoader.load();
            EyedropperController eyedropperController = fxmlLoader.getController();
            eyedropperController.setLoadingModel(loadingModel);
            stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        }
        else{
            Toolkit.getDefaultToolkit().beep();//default error noise
        }
    }

    public void selectImage(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Image File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"));

        stage = (Stage) runButton.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            colorPickerSetField.setText(selectedFile.getAbsolutePath());
            SharedDataModel.getInstance().setSelectedImage(selectedFile);

        }
    }

    public void init(LoadingModel loadingModel) {
        this.loadingModel = loadingModel;
    }
}
