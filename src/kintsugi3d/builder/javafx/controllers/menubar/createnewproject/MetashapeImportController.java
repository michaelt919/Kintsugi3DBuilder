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
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.MetashapeProjectInputSource;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;
import kintsugi3d.builder.javafx.controllers.menubar.metashape.MetashapeChunk;
import kintsugi3d.builder.javafx.controllers.menubar.metashape.MetashapeDocument;
import kintsugi3d.builder.javafx.controllers.menubar.metashape.MetashapeModel;
import kintsugi3d.util.RecentProjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class MetashapeImportController extends FXMLPageController implements ShareInfo {
    private static final Logger log = LoggerFactory.getLogger(MetashapeImportController.class);

    @FXML private Text fileNameTxtField;
    @FXML private AnchorPane anchorPane;
    @FXML private Text loadMetashapeObject;

    @FXML private ChoiceBox<String> chunkSelectionChoiceBox;
    @FXML private ChoiceBox<String> modelSelectionChoiceBox;

    private MetashapeDocument metashapeDocument;

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
            metashapeDocument.selectChunk(chunkSelectionChoiceBox.getValue());
            updateModelSelectionChoiceBox();
            updateLoadedIndicators();
        }));

        fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
    }

    @Override
    public boolean isNextButtonValid() {
        return metashapeDocument != null && hasModels();
    }

    @Override
    public void shareInfo() {
        InputSource source = hostScrollerController.getInfo(Info.INPUT_SOURCE);
        if (source instanceof MetashapeProjectInputSource){
            //overwrite old source so we can compare old and new versions in PrimaryViewSelectController
            hostScrollerController.addInfo(Info.INPUT_SOURCE,
                    new MetashapeProjectInputSource().setMetashapeModel(metashapeDocument.getSelectedChunk().getSelectedModel()));
        }
        else{
            log.error("Error sending Metashape project info to host controller. MetashapeProjectInputSource expected.");
        }
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
        File file = fileChooser.showOpenDialog(stage);

        if(file != null){
            RecentProjects.setMostRecentDirectory(file.getParentFile());
            fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());

            fileNameTxtField.setText(file.getName());
            updateChoiceBoxes(file);
            updateLoadedIndicators();
        }
    }

    private void updateChoiceBoxes(File psxFile) {
        updateChunkSelectionChoiceBox(psxFile);
        updateModelSelectionChoiceBox();
        updateLoadedIndicators();
    }

    private void updateModelSelectionChoiceBox() {
        if (chunkSelectionChoiceBox.getItems().isEmpty()) {
            return;
        }

        modelSelectionChoiceBox.setDisable(true);
        modelSelectionChoiceBox.getItems().clear();
        for (MetashapeModel model : metashapeDocument.getSelectedChunk().getModels()) {
            String modelID = model.getId().isPresent() ? String.valueOf(model.getId().get()) : NO_MODEL_ID_MSG;
            String modelName = !model.getLabel().isBlank() ? model.getLabel() : NO_MODEL_NAME_MSG;
            modelSelectionChoiceBox.getItems().add(modelID + "   " + modelName);
        }


        if (!hasModels()) {
            showNoModelsAlert();
            return;
        }

        //initialize choice box to first option instead of null option
        //then set to default model if the chunk has one
        modelSelectionChoiceBox.setValue(modelSelectionChoiceBox.getItems().get(0));
        modelSelectionChoiceBox.setDisable(false);

        if (metashapeDocument.getSelectedChunk().getDefaultModelID().isEmpty()) {
            return;
        }

        for (int i = 0; i < modelSelectionChoiceBox.getItems().size(); ++i) {
            String obj = modelSelectionChoiceBox.getItems().get(i);
            String modelID = getModelIDFromSelection(obj);
            if (modelID == null) {
                continue;
            }

            int id = Integer.parseInt(modelID);
            if (metashapeDocument.getSelectedChunk().getDefaultModelID().get().equals(id)) {
                modelSelectionChoiceBox.setValue(obj);
                break;
            }
        }

    }

    private void showMissingItemsAlert(String title, String msg) {
        if (alertShown) {
            return;
        } //prevent multiple alerts from showing at once

        alertShown = true;
        Platform.runLater(() ->
        {
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType openCustomProj = new ButtonType("Create Custom Project", ButtonBar.ButtonData.YES);

            Alert alert = new Alert(Alert.AlertType.NONE, msg, ok, openCustomProj);

            ((ButtonBase) alert.getDialogPane().lookupButton(openCustomProj)).setOnAction(event -> {
                //manually navigate though pages to get to custom loader
                hostScrollerController.prevPage();//go to SelectImportOptions.fxml
                SelectImportOptionsController controller = (SelectImportOptionsController)
                        hostScrollerController.getCurrentPage().getController();
                controller.looseFilesSelect();
                alertShown = false;
            });

            ((ButtonBase) alert.getDialogPane().lookupButton(ok)).setOnAction(event -> alertShown = false);

            alert.setTitle(title);
            alert.show();
        });
    }

    private void showNoChunksAlert(){
       showMissingItemsAlert("Metashape document has no chunks.",
               "Please select another document or create a custom project.");
    }

    private void showNoModelsAlert() {
        showMissingItemsAlert("Metashape chunk has no models.",
                "Please select another chunk or create a custom project.");
    }

    private boolean hasModels() {
        if (metashapeDocument == null){
            return false;
        }
        return !metashapeDocument.getSelectedChunk().getModels().isEmpty();
    }

    private void updateChunkSelectionChoiceBox(File psxFile) {
        chunkSelectionChoiceBox.setDisable(true);

        //load chunks into chunk selection module
        metashapeDocument = new MetashapeDocument(psxFile.getPath());

        List<MetashapeChunk> chunks = metashapeDocument.getChunks();

        if (chunks.isEmpty()){
            showNoChunksAlert();
            modelSelectionChoiceBox.getItems().clear();
            return;
        }

        chunkSelectionChoiceBox.getItems().clear();

        boolean missingChunks = false;
        for (MetashapeChunk chunk : chunks){
            if (chunk.hasModels()){
                chunkSelectionChoiceBox.getItems().add(chunk.getLabel());
            }
            else{
                missingChunks = true;
            }
        }

        if (chunkSelectionChoiceBox.getItems().isEmpty()) {
            showMissingItemsAlert("All chunks are missing models.",
                    "None of your chunks have valid model data. Please select another document or create a custom project.");
            modelSelectionChoiceBox.getItems().clear();
            return;
        }

        if (missingChunks) {
            showMissingItemsAlert("Some chunks are missing models.",
                    "Some of your chunks do not have models. They will not appear in the dropdown.");
        }

        //initialize choice box to first option instead of null option
        if (chunkSelectionChoiceBox.getItems() != null &&
                chunkSelectionChoiceBox.getItems().get(0) != null){
            chunkSelectionChoiceBox.setValue(chunkSelectionChoiceBox.getItems().get(0));
            chunkSelectionChoiceBox.setDisable(false);

            //set chunk to default chunk if it has one
            Integer activeChunkID = metashapeDocument.getActiveChunkID();
            if (activeChunkID != null){
                String chunkName = metashapeDocument.getChunkNameFromID(activeChunkID);

                for (String str : chunkSelectionChoiceBox.getItems()){
                    if (str.equals(chunkName)){
                        chunkSelectionChoiceBox.setValue(str);
                        break;
                    }
                }
            }
        }
    }

    private void updateLoadedIndicators() {
        if (hasModels()) {
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
}
