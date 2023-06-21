package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class UnzipFileSelectionController {
    @FXML
    public Button runButton;

    private File selectedFile;
    private Stage stage;

    @FXML
    public TextField psxPathTxtField;

    public void init(){
    }

    public void selectPSX(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose .psx file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Metashape files", "*.psx"));

        stage = (Stage) runButton.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if(selectedFile != null){
            psxPathTxtField.setText(selectedFile.getAbsolutePath());
        }
    }

    public void unzip(ActionEvent actionEvent) {
        //TODO: IMPLEMENT THIS
    }

    private boolean loadPSXFromTxtField() {
        String path = psxPathTxtField.getText();

        File tempFile = new File(path);

        if (tempFile.exists()) {
            selectedFile = tempFile;
            return true;//file exists
        }
        else{
            return false;//file does not exist
        }
    }
}
