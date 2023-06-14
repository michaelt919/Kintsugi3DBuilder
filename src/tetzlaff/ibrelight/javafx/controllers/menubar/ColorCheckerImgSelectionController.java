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
    public TextField imgPathTxtField;

    private File selectedFile;

    private Stage stage;
    private Scene scene;
    private Parent root;

    private LoadingModel loadingModel;

    public void openEyedropper(ActionEvent actionEvent) throws IOException {
        if(SharedDataModel.getInstance().getSelectedImage()!= null || loadImgFromTxtField()) {//only move on if image is selected.
                                                                                            // Also, if no image selected, check to see if
                                                                                            //path was put into text field manually
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

    private boolean loadImgFromTxtField() {//TODO: TELL USER THAT FILE IS NOT FOUND?
        String path = imgPathTxtField.getText();
        selectedFile = new File(path);
        if (selectedFile.exists()){
            SharedDataModel.getInstance().setSelectedImage(selectedFile);
            return true;
        }
        else{
            return false;
        }
    }

    public void selectImage(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Image File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"));

        stage = (Stage) runButton.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            imgPathTxtField.setText(selectedFile.getAbsolutePath());
            SharedDataModel.getInstance().setSelectedImage(selectedFile);
        }
    }

    public void init(LoadingModel loadingModel) {
        this.loadingModel = loadingModel;
    }
}
