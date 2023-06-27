package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

    final static String[] validExtensions = {"*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"};


    private LoadingModel loadingModel;

    public void openEyedropper(ActionEvent actionEvent) throws IOException {
        if(isImagePathInTxtField()) {//only move on if image is selected.
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

    private boolean isImagePathInTxtField() {//TODO: TELL USER THAT FILE IS NOT FOUND?
        String path = imgPathTxtField.getText();
        selectedFile = new File(path);
        if (selectedFile.exists() && isValidImageType(path)){
            SharedDataModel.getInstance().setSelectedImage(selectedFile);
            return true;
        }
        else{
            return false;
        }
    }

    private boolean isValidImageType(String path) {
        for (String extension : validExtensions) {
            if (path.matches("." + extension)) {
                return true;
            }
        }
        return false;
    }

    public void selectImage(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Image File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", validExtensions));

        stage = (Stage) runButton.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            imgPathTxtField.setText(selectedFile.getAbsolutePath());
        }
    }

    public void init(LoadingModel loadingModel) {
        this.loadingModel = loadingModel;
    }

    public void enterToRun(KeyEvent keyEvent) {//press the enter button while in the text field to hit the run button
        if (keyEvent.getCode() == KeyCode.ENTER) {
            runButton.fire();
        }
    }
}
