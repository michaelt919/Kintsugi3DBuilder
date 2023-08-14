/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.scene;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.*;
import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.io.ViewSetReaderFromAgisoftXML;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ResourceBundle;

public class ImportDataController implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(ImportDataController.class);

    @FXML private ChoiceBox<String> chunkSelectionChoiceBox;
    @FXML private Separator separatorBar;

    @FXML private Button okButton;
    @FXML private Button metashapeFileSelectButton;

    @FXML private ChoiceBox<String> primaryViewChoiceBox;
    @FXML private Text loadCheckCameras;
    @FXML private Text loadCheckObj;
    @FXML private Text loadCheckImages;
    @FXML private Text loadMetashapeObject;

    @FXML private GridPane root;

    private Stage thisStage;

    private final FileChooser camFileChooser = new FileChooser();
    private final FileChooser objFileChooser = new FileChooser();
    private final DirectoryChooser photoDirectoryChooser = new DirectoryChooser();

    private File cameraFile;
    private File objFile;
    private File photoDir;

    private Runnable callback;
    private File metashapePsxFile;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {

        setHomeDir(new File(System.getProperty("user.home")));
        camFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Agisoft Metashape XML file", "*.xml"));
        objFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wavefront OBJ file", "*.obj"));

        camFileChooser.setTitle("Select camera positions file");
        objFileChooser.setTitle("Select object file");
        photoDirectoryChooser.setTitle("Select photo directory");
    }

    public void setCallback(Runnable callback)
    {
        this.callback = callback;
    }

    @FXML
    private void camFileSelect()
    {

        File temp = camFileChooser.showOpenDialog(getStage());

        if (temp != null)
        {
            cameraFile = temp;
            setHomeDir(temp);

            try
            {
                ReadonlyViewSet newViewSet = ViewSetReaderFromAgisoftXML.getInstance().readFromFile(cameraFile);

                loadCheckCameras.setText("Loaded");
                loadCheckCameras.setFill(Paint.valueOf("Green"));

                primaryViewChoiceBox.getItems().clear();
                for (int i = 0; i < newViewSet.getCameraPoseCount(); i++)
                {
                    primaryViewChoiceBox.getItems().add(newViewSet.getImageFileName(i));
                }
                primaryViewChoiceBox.getItems().sort(Comparator.naturalOrder());
                primaryViewChoiceBox.getSelectionModel().select(0);
            }
            catch (Exception e)
            {
                log.error("An error occurred reading camera file:", e);
                new Alert(Alert.AlertType.ERROR, e.toString()).show();
            }
        }

        updateOkButtonAndSeparatorBar();
    }

    @FXML
    private void objFileSelect()
    {

        File temp = objFileChooser.showOpenDialog(getStage());

        if (temp != null)
        {
            objFile = temp;
            setHomeDir(temp);
            loadCheckObj.setText("Loaded");
            loadCheckObj.setFill(Paint.valueOf("Green"));
        }

        updateOkButtonAndSeparatorBar();
    }

    @FXML
    private void psxFileSelect(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose .psx file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Metashape files", "*.psx"));

        Stage stage = (Stage) ((Node)actionEvent.getSource()).getScene().getWindow();
        metashapePsxFile = fileChooser.showOpenDialog(stage);

        if(isMetashapeObjectLoaded()){
            loadMetashapeObject.setText("Loaded");
            loadMetashapeObject.setFill(Paint.valueOf("Green"));

            //load chunks into chunk selection module
            MetashapeObject metashapeObject = new MetashapeObject();
            ArrayList<String> chunkNames = (ArrayList<String>)
                    metashapeObject.loadChunkNamesFromPSX(metashapePsxFile.getAbsolutePath());

            chunkSelectionChoiceBox.getItems().clear();
            chunkSelectionChoiceBox.getItems().addAll(chunkNames);

            chunkSelectionChoiceBox.setDisable(false);

            //initialize choice box to first option instead of null option
            if (chunkSelectionChoiceBox.getItems() != null &&
                    chunkSelectionChoiceBox.getItems().get(0) != null){
                chunkSelectionChoiceBox.setValue(chunkSelectionChoiceBox.getItems().get(0));
            }
        }

        updateOkButtonAndSeparatorBar();
    }

    @FXML
    private void photoDirectorySelect()
    {

        File temp = photoDirectoryChooser.showDialog(getStage());

        if (temp != null)
        {
            photoDir = temp;
            setHomeDir(temp);
            loadCheckImages.setText("Loaded");
            loadCheckImages.setFill(Paint.valueOf("Green"));
        }

        updateOkButtonAndSeparatorBar();
    }

    @FXML
    private void okButtonPress()
    {
        if (areComponentsLoaded())
        {
            callback.run();

            new Thread(() ->
                    MultithreadModels.getInstance().getLoadingModel().loadFromAgisoftFiles(
                            cameraFile.getPath(), cameraFile, objFile, photoDir,
                            primaryViewChoiceBox.getSelectionModel().getSelectedItem()))
                    .start();

            close();
        }
    }

    private boolean areComponentsLoaded() {
        //true if either...
        //  metashape psx is loaded
        //  or
        //  all three other components are loaded
        return isMetashapeObjectLoaded() || (cameraFile != null && (objFile != null) && (photoDir != null));
    }

    private boolean isMetashapeObjectLoaded() {
        return metashapePsxFile != null;
    }

    private void updateOkButtonAndSeparatorBar(){
        //if project can be created, enable ok button and turn separator bar green
        //if not, disable ok button and turn separator bar red

        if(areComponentsLoaded()){
            okButton.setDisable(false);
            separatorBar.setStyle("-fx-background-color: green");
        }
        else{
            okButton.setDisable(true);
            separatorBar.setStyle("-fx-background-color: red");
        }
    }

    @FXML
    private void cancelButtonPress()
    {
        close();
    }

    private void close()
    {
        Window window = root.getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void setHomeDir(File home)
    {
        File parentDir;
        parentDir = home.getParentFile();
        camFileChooser.setInitialDirectory(parentDir);
        objFileChooser.setInitialDirectory(parentDir);
        photoDirectoryChooser.setInitialDirectory(parentDir);
    }

    private Stage getStage()
    {
        if (thisStage == null)
        {
            thisStage = (Stage) root.getScene().getWindow();
        }
        return thisStage;
    }


}
