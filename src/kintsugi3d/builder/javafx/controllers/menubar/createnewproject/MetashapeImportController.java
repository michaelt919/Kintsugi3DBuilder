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


import com.agisoft.metashape.Chunk;
import com.agisoft.metashape.Document;
import com.agisoft.metashape.Model;
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
import kintsugi3d.util.RecentProjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class MetashapeImportController extends FXMLPageController implements ShareInfo {
    private static final Logger log = LoggerFactory.getLogger(MetashapeImportController.class);

    @FXML private Text fileNameTxtField;
    @FXML private AnchorPane anchorPane;
    @FXML private Text loadMetashapeObject;

    @FXML private ChoiceBox<String> chunkSelectionChoiceBox;
    @FXML private ChoiceBox<String> modelSelectionChoiceBox;

    private File metashapePsxFile;
    private Chunk chunk;
    private Document doc;

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
                updateDocumentChunk();
                updateModelSelectionChoiceBox();
                updateLoadedIndicators();

        }));

        fileChooser.setInitialDirectory(RecentProjects.getMostRecentDirectory());
    }

    private void updateDocumentChunk() {
        String chunkName = chunkSelectionChoiceBox.getSelectionModel().getSelectedItem();
        Optional<Chunk> selectedChunk = Arrays.stream(doc.getChunks()).filter(chunk -> chunk.getLabel().equals(chunkName)).findFirst();
        selectedChunk.ifPresent(value -> doc.setActiveChunk(value));
    }

    @Override
    public boolean isNextButtonValid() {
        return chunk != null && chunk.hasModel();
    }

    @Override
    public void shareInfo() {
        //update metashapeObjectChunk with selected chunk from chunkSelectionChoiceBox
        updateMetashapeChunk();

        InputSource source = hostScrollerController.getInfo(Info.INPUT_SOURCE);
        if (source instanceof MetashapeProjectInputSource){
            //overwrite old source so we can compare old and new versions in PrimaryViewSelectController
            hostScrollerController.addInfo(Info.INPUT_SOURCE,
                    new MetashapeProjectInputSource().setChunk(chunk));
        }
        else{
            log.error("Error sending Metashape project info to host controller. MetashapeProjectInputSource expected.");
        }
    }

    private void updateMetashapeChunk() {
        if (doc != null){
            updateDocumentChunk();
            Optional<Integer> modelID = getSelectedModelID();
            modelID.ifPresent(integer -> doc.getActiveChunk().setModelKey(integer));
        }
    }

    private Optional<Integer> getSelectedModelID() {
        String selectionAsString = modelSelectionChoiceBox.getSelectionModel().getSelectedItem();
        return getModelIDFromSelection(selectionAsString);
    }

    private static Optional<Integer> getModelIDFromSelection(String selectionAsString) {
        //TODO: need to revisit this when formatting of model selection choice box changes
        if (selectionAsString.startsWith(NO_MODEL_ID_MSG)){
            return Optional.empty();
        }

        return Optional.of(Integer.parseInt(selectionAsString.substring(0, selectionAsString.indexOf(' '))));
    }

    @FXML
    private void psxFileSelect(ActionEvent actionEvent) {
        Stage stage = (Stage) ((Node)actionEvent.getSource()).getScene().getWindow();
        metashapePsxFile = fileChooser.showOpenDialog(stage);

        if(metashapePsxFile != null){
            fileNameTxtField.setText(metashapePsxFile.getName());
            updateChoiceBoxes();

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

        chunk = doc.getActiveChunk();
        Model[] models = chunk.getModels();

        modelSelectionChoiceBox.getItems().clear();
        if (models.length == 0){
            showNoModelsAlert();
            return;
        }
        for (Model model : models){
            String modelName = !Objects.equals(model.getLabel(), "") ?  model.getLabel() : NO_MODEL_NAME_MSG;
            int modelID = model.getKey();
            modelSelectionChoiceBox.getItems().add(modelID + "   " + modelName);
        }

        //initialize choice box to first option instead of null option
        //then set to default model if the chunk has one
        modelSelectionChoiceBox.setValue(modelSelectionChoiceBox.getItems().get(0));
        modelSelectionChoiceBox.setDisable(false);

        for (int i = 0; i < modelSelectionChoiceBox.getItems().size(); ++i){
            String modelName = modelSelectionChoiceBox.getItems().get(i);
            Optional<Integer> modelID = getModelIDFromSelection(modelName);
            if (modelID.isEmpty()){continue;}

            if (chunk.getModel().isPresent() &&
                    modelID.get().equals( chunk.getModel().get().getKey())){
                modelSelectionChoiceBox.setValue(modelName);
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

    private void updateChunkSelectionChoiceBox() {
        chunkSelectionChoiceBox.setDisable(true);

        try{
            doc = new Document();
            doc.open(metashapePsxFile.getAbsolutePath(), false, null);
        }
        catch(Throwable t){
            log.error("Error reading Metashape document.", t);
        }

        List<String> chunkNames = Arrays.stream(doc.getChunks()).map(Chunk::getLabel).collect(Collectors.toList());

        chunkSelectionChoiceBox.getItems().clear();
        chunkSelectionChoiceBox.getItems().addAll(chunkNames);

        if (chunkSelectionChoiceBox.getItems() != null &&
                chunkSelectionChoiceBox.getItems().get(0) != null){

            //set chunk to default chunk if it has one
            Chunk activeChunk = doc.getActiveChunk();
            if (activeChunk != null){
                chunkSelectionChoiceBox.setValue(activeChunk.getLabel());
            }
            else{ //otherwise, the first chunk is fine
                chunkSelectionChoiceBox.setValue(chunkSelectionChoiceBox.getItems().get(0));
            }
            chunkSelectionChoiceBox.setDisable(false);
        }
    }

    private void updateLoadedIndicators() {
        if (doc != null && chunk.hasModel()) {
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
