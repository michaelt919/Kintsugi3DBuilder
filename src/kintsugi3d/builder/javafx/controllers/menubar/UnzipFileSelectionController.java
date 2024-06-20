/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.function.Consumer;

public class UnzipFileSelectionController {
    private static final Logger log = LoggerFactory.getLogger(UnzipFileSelectionController.class);

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

    public Consumer<MetashapeObjectChunk> loaderControllerCallback;

    MetashapeObject metashapeObject;


    public UnzipFileSelectionController() {
        metashapeObject = new MetashapeObject();
    }

    public void init(){
        this.directoryChooser = new DirectoryChooser();
        chunkSelectionChoiceBox.setDisable(true);
        selectChunkButton.setDisable(true);
    }

    public void init(Consumer<MetashapeObjectChunk> loaderCallback ){
        this.loaderControllerCallback = loaderCallback;
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
                metashapeObject.loadChunkNamesFromPSX(psxPathTxtField.getText());

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

        try {
            //load chunk viewer window
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/menubar/ChunkViewer.fxml"));
            root = fxmlLoader.load();
            ChunkViewerController chunkViewerController = fxmlLoader.getController();

            metashapeObject.setPsxFilePath(psxPathTxtField.getText());

            //TODO: actually find model id instead of defaulting to 0
            MetashapeObjectChunk metashapeObjectChunk = new MetashapeObjectChunk(metashapeObject, selectedChunkName, 0);

            chunkViewerController.initializeChunkSelectionAndTreeView(metashapeObjectChunk);

            // Pass a reference of the LoaderController to callback in the chunk viewer controller.
            // This is how the chosen chunk will be passed to the LoaderController
            if(loaderControllerCallback != null){
                chunkViewerController.loaderControllerCallback = loaderControllerCallback;
            }

        }
        catch (Exception e){
            unzipPSXButton.fire();//selected .psx file and list of chunks may be referring to different objects
                                    //if chunk selection fails, try unzipping the file again
                                    //this action will also update the chunk selection choice box
                                    //see unzipPSXAndParse()
            log.error("An error occurred:", e);
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
