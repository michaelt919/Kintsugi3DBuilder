package tetzlaff.ibrelight.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class UnzipFileSelectionController {
    @FXML
    public Button unzipPSXButton;

    private File selectedFile;
    private Stage stage;

    private DirectoryChooser directoryChooser;

    @FXML
    public TextField psxPathTxtField;
    @FXML
    public ChoiceBox<String> chunkSelectionChoiceBox;

    @FXML
    public Button selectChunkButton;

    @FXML
    public TextField outputDirectoryPathTxtField;

    private Scene scene;
    private Parent root;

    MetashapeObject metashapeObject;


    public UnzipFileSelectionController() {
        metashapeObject = new MetashapeObject();
    }

    public void init(){
        this.directoryChooser = new DirectoryChooser();
        chunkSelectionChoiceBox.setDisable(true);
        selectChunkButton.setDisable(true);
    }

    public void selectPSX() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose .psx file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Metashape files", "*.psx"));

        stage = (Stage) unzipPSXButton.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if(selectedFile != null){
            psxPathTxtField.setText(selectedFile.getAbsolutePath());
        }
    }

    public void unzipPSXAndParse() {
        //get chunk names from Metashape object
        //add them to chunkSelectionChoiceBox

        //do not unzip if psx file path is invalid
        if(!isValidPSXFilePath()){
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        ArrayList<String> chunkNames = (ArrayList<String>)
                metashapeObject.getChunkNamesFromPSX(psxPathTxtField.getText());

        chunkSelectionChoiceBox.getItems().clear();
        chunkSelectionChoiceBox.getItems().addAll(chunkNames);

        chunkSelectionChoiceBox.setDisable(false);
        selectChunkButton.setDisable(false);

        //initialize choice box to first option instead of null option
        if (chunkSelectionChoiceBox.getItems() != null &&
            chunkSelectionChoiceBox.getItems().get(0) != null){
            chunkSelectionChoiceBox.setValue(chunkSelectionChoiceBox.getItems().get(0));
        }
    }

    private boolean isValidPSXFilePath() {
        String path = psxPathTxtField.getText();
        File file = new File(path);
        return file.exists() && file.getAbsolutePath().endsWith(".psx");
    }

    public void selectOutputDirectory() {//TODO: REMOVE THIS?
        //if this is removed, also remove the necessary items from UnzipFileSelection.fxml
        this.directoryChooser.setTitle("Choose an output directory");

        stage = (Stage) outputDirectoryPathTxtField.getScene().getWindow();
        File file = this.directoryChooser.showDialog(stage.getOwner());

        if (file != null && file.exists()){
            directoryChooser.setInitialDirectory(file);
            outputDirectoryPathTxtField.setText(file.getAbsolutePath());
        }
        else{
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void selectChunk(ActionEvent actionEvent) {
        String selectedChunkName = chunkSelectionChoiceBox.getValue();
        String selectedChunkZip = metashapeObject.getChunkZipPathPairs().get(selectedChunkName);

        try {
            //load chunk viewer window
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/menubar/ChunkViewer.fxml"));
            root = fxmlLoader.load();
            ChunkViewerController chunkViewerController = fxmlLoader.getController();


            metashapeObject.setPsxFilePath(psxPathTxtField.getText());
            MetashapeObjectChunk metashapeObjectChunk = new MetashapeObjectChunk(metashapeObject, selectedChunkZip);

            chunkViewerController.initializeChunkSelectionAndTreeView(metashapeObjectChunk);
        }
        catch (Exception e){
            unzipPSXButton.fire();//selected .psx file and list of chunks may be referring to different objects
                                    //if chunk selection fails, try unzipping the file again
                                    //this action will also update the chunk selection choice box
                                    //see unzipPSXAndParse()
            e.printStackTrace();
            return;//do not load new window for chunk viewer
        }

        stage = (Stage) ((javafx.scene.Node) actionEvent.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void enterToRun(KeyEvent keyEvent) {//press the enter button while in the text field to unzip
        if (keyEvent.getCode() == KeyCode.ENTER) {
            unzipPSXButton.fire();
        }
    }
}
