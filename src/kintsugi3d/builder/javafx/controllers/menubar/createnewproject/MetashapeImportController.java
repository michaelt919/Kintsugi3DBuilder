/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObject;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.MetashapeProjectInputSource;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;
import kintsugi3d.util.RecentProjects;
import kintsugi3d.util.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;

public class MetashapeImportController extends FXMLPageController implements ShareInfo {
    private static final Logger log = LoggerFactory.getLogger(MetashapeImportController.class);

    @FXML private Text fileNameTxtField;
    @FXML private AnchorPane anchorPane;
    @FXML private Text loadMetashapeObject;

    @FXML private ChoiceBox chunkSelectionChoiceBox;
    @FXML private ChoiceBox modelSelectionChoiceBox;

    private File metashapePsxFile;
    private MetashapeObjectChunk metashapeObjectChunk;

    private static final String NO_MODEL_ID_MSG = "No Model ID";
    private static final String NO_MODEL_NAME_MSG = "Unnamed Model";
    private volatile boolean alertShown = false;

    FileChooser fileChooser;
    @Override
    public Region getHostRegion() {
        return anchorPane;
    }

    @Override
    public void init() {
        fileChooser = new FileChooser();
        fileChooser.setTitle("Choose .psx file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Metashape files (*.psx)", "*.psx"));
        fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());

        hostPage.setNextPage(hostScrollerController.getPage("/fxml/menubar/createnewproject/PrimaryViewSelect.fxml"));
    }

    @Override
    public void refresh() {
        updateLoadedIndicators();
        //need to do Platform.runLater so updateModelSelectionChoiceBox can pull info from chunkSelectionChoiceBox
        chunkSelectionChoiceBox.setOnAction(event -> Platform.runLater(()->{
                updateModelSelectionChoiceBox();
                updateLoadedIndicators();

        }));

        fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
    }

    @Override
    public boolean isNextButtonValid() {
        return isMetashapeObjectLoaded() && hasModels();
    }

    @Override
    public void shareInfo() {
        //update metashapeObjectChunk with selected chunk from chunkSelectionChoiceBox
        updateMetashapeChunk();

        InputSource source = hostScrollerController.getInfo(Info.INPUT_SOURCE);
        if (source instanceof MetashapeProjectInputSource){
            //overwrite old source so we can compare old and new versions in PrimaryViewSelectController
            hostScrollerController.addInfo(Info.INPUT_SOURCE,
                    new MetashapeProjectInputSource().setMetashapeObjectChunk(metashapeObjectChunk));
        }
        else{
            log.error("Error sending Metashape project info to host controller. MetashapeProjectInputSource expected.");
        }
    }

    private void updateMetashapeChunk() {
        if (metashapeObjectChunk != null){
            MetashapeObject metashapeObject = metashapeObjectChunk.getMetashapeObject();
            String chunkName = (String) chunkSelectionChoiceBox.getSelectionModel().getSelectedItem();
            String modelID = getSelectedModelID();

            metashapeObjectChunk = new MetashapeObjectChunk(metashapeObject, chunkName, modelID);
        }
    }

    private String getSelectedModelID() {
        String selectionAsString = (String) modelSelectionChoiceBox.getSelectionModel().getSelectedItem();
        return getModelIDFromSelection(selectionAsString);
    }

    private static String getModelIDFromSelection(String selectionAsString) {
        //TODO: need to revisit this when formatting of model selection choice box changes
        if (selectionAsString.startsWith(NO_MODEL_ID_MSG)){
            return null;
        }

        return selectionAsString.substring(0, selectionAsString.indexOf(' '));
    }

    @FXML
    private void psxFileSelect(ActionEvent actionEvent) {
        Stage stage = (Stage) ((Node)actionEvent.getSource()).getScene().getWindow();
        metashapePsxFile = fileChooser.showOpenDialog(stage);

        if(metashapePsxFile != null){
            metashapeObjectChunk = null;
            fileNameTxtField.setText(metashapePsxFile.getName());
            updateChoiceBoxes();
            updateLoadedIndicators();

            RecentProjects.setMostRecentDirectory(metashapePsxFile.getParentFile());
            fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
        }
    }

    private void updateChoiceBoxes() {
        updateChunkSelectionChoiceBox();
        updateModelSelectionChoiceBox();
        updateLoadedIndicators();
    }

    private void updateModelSelectionChoiceBox() {
        modelSelectionChoiceBox.setDisable(true);

        if (metashapeObjectChunk != null){
            String chunkName = (String) chunkSelectionChoiceBox.getSelectionModel().getSelectedItem();
            metashapeObjectChunk.updateChunk(chunkName);
        }
        else{
            MetashapeObject metashapeObject = new MetashapeObject(metashapePsxFile.getAbsolutePath());
            String chunkName = (String) chunkSelectionChoiceBox.getSelectionModel().getSelectedItem();
            String modelID = "0";

            metashapeObjectChunk = new MetashapeObjectChunk(metashapeObject, chunkName, modelID);
        }


        ArrayList<Triplet<String, String, String>> modelInfo = metashapeObjectChunk.getModelInfo();

        modelSelectionChoiceBox.getItems().clear();
        for (Triplet<String, String, String> triplet : modelInfo){
            String modelID = triplet.first != null ? String.valueOf(triplet.first) : NO_MODEL_ID_MSG;
            String modelName = !triplet.second.isBlank() ? triplet.second : NO_MODEL_NAME_MSG;
            modelSelectionChoiceBox.getItems().add(modelID + "   " + modelName);
        }


        if (!hasModels()){
            showNoModelsAlert();
            return;
        }

        //initialize choice box to first option instead of null option
        //then set to default model if the chunk has one
        modelSelectionChoiceBox.setValue(modelSelectionChoiceBox.getItems().get(0));
        modelSelectionChoiceBox.setDisable(false);

        if (metashapeObjectChunk.getDefaultModelID() == null){return;}

        for (int i = 0; i < modelSelectionChoiceBox.getItems().size(); ++i){
            Object obj = modelSelectionChoiceBox.getItems().get(i);
            String modelID = getModelIDFromSelection((String) obj);
            if (modelID == null){continue;}

            if (modelID.equals(metashapeObjectChunk.getDefaultModelID())){
                modelSelectionChoiceBox.setValue(obj);
                break;
            }
        }

    }

    private void showNoModelsAlert() {
        if (alertShown){
            return;
        } //prevent multiple alerts from showing at once

        alertShown = true;
        Platform.runLater(() ->
        {
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType openCustomProj = new ButtonType("Create Custom Project", ButtonBar.ButtonData.YES);

            Alert alert = new Alert(Alert.AlertType.NONE,"Please select another chunk or create a custom project.", ok, openCustomProj);

            ((ButtonBase) alert.getDialogPane().lookupButton(openCustomProj)).setOnAction(event -> {
                //manually navigate though pages to get to custom loader
                hostScrollerController.prevPage();//go to SelectImportOptions.fxml
                SelectImportOptionsController controller = (SelectImportOptionsController)
                        hostScrollerController.getCurrentPage().getController();
                controller.looseFilesSelect();
                alertShown = false;
            });

            ((ButtonBase) alert.getDialogPane().lookupButton(ok)).setOnAction(event -> alertShown = false);

            alert.setTitle("Metashape chunk has no models.");
            alert.show();
        });
    }

    private boolean hasModels() {
        return modelSelectionChoiceBox.getItems() != null &&
                !modelSelectionChoiceBox.getItems().isEmpty() &&
                modelSelectionChoiceBox.getItems().get(0) != null;
    }

    private void updateChunkSelectionChoiceBox() {
        chunkSelectionChoiceBox.setDisable(true);

        //load chunks into chunk selection module
        MetashapeObject metashapeObject = new MetashapeObject(metashapePsxFile.getPath());

        ArrayList<String> chunkNames = (ArrayList<String>) metashapeObject.
                getChunkNamesDynamic(metashapeObject.getPsxFilePath());

        chunkSelectionChoiceBox.getItems().clear();
        chunkSelectionChoiceBox.getItems().addAll(chunkNames);


        //initialize choice box to first option instead of null option
        if (chunkSelectionChoiceBox.getItems() != null &&
                chunkSelectionChoiceBox.getItems().get(0) != null){
            chunkSelectionChoiceBox.setValue(chunkSelectionChoiceBox.getItems().get(0));
            chunkSelectionChoiceBox.setDisable(false);

            //set chunk to default chunk if it has one
            Integer activeChunkID = metashapeObject.getActiveChunkID();
            if (activeChunkID != null){
                String chunkName = metashapeObject.getChunkNameFromID(activeChunkID);

                for (Object obj : chunkSelectionChoiceBox.getItems()){
                    String str = (String) obj;
                    if (str.equals(chunkName)){
                        chunkSelectionChoiceBox.setValue(obj);
                        break;
                    }
                }
            }
        }
    }

    private void updateLoadedIndicators() {
        if (isMetashapeObjectLoaded() && hasModels()) {//TODO: change condition to check for metashapeObjectChunk != null?
            loadMetashapeObject.setText("Loaded");
            loadMetashapeObject.setFill(Paint.valueOf("Green"));

            hostScrollerController.setNextButtonDisable(false);
        }
        else{
            loadMetashapeObject.setText("Unloaded");
            loadMetashapeObject.setFill(Paint.valueOf("Red"));

            hostScrollerController.setNextButtonDisable(true);
        }
    }

    private boolean isMetashapeObjectLoaded() {
        return metashapePsxFile != null;
    }
}
