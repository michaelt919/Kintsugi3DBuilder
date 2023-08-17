/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import kintsugi3d.builder.javafx.controllers.scene.ImgThreadCompatibleController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ChunkViewerController extends ImgThreadCompatibleController {
    //TODO: --> "INFO: index exceeds maxCellCount. Check size calculations for class javafx.scene.control.skin.TreeViewSkin$1"
    //suppress warning?

    private static final Logger log = LoggerFactory.getLogger(ChunkViewerController.class);
    static final String[] VALID_EXTENSIONS = {"*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"};

    @FXML
    public ChoiceBox<String> newChunkSelectionChoiceBox;//allows the user to select a new chunk to view

    @FXML public Button selectChunkButton;

    private ImgSelectionThread loadImgThread;

    public void initializeChunkSelectionAndTreeView(MetashapeObjectChunk metashapeObjectChunk) {
        this.metashapeObjectChunk = metashapeObjectChunk;

        initTreeView();

        //initialize options in new chunk selection choice box (cannot be done before chunkName is set)
        initializeChoiceBox();
        this.newChunkSelectionChoiceBox.setOnAction(this::updateSelectChunkButton);

        //disable select chunk button if selected chunk (in choice box) matches the current chunk
        updateSelectChunkButton();
    }

    public void selectChunk(ActionEvent actionEvent) throws IOException {
        Scene scene;
        Parent root;
        Stage stage;

        String currentChunkName = this.metashapeObjectChunk.getChunkName();
        String selectedChunkName = this.newChunkSelectionChoiceBox.getValue();

        MetashapeObject metashapeObject = this.metashapeObjectChunk.getMetashapeObject();

        if (!selectedChunkName.equals(currentChunkName)) {//only change scene if switching to new chunk
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/menubar/ChunkViewer.fxml"));
            root = fxmlLoader.load();
            ChunkViewerController chunkViewerController = fxmlLoader.getController();

            MetashapeObjectChunk newMetashapeObjectChunk = new MetashapeObjectChunk(metashapeObject, selectedChunkName);
            chunkViewerController.initializeChunkSelectionAndTreeView(newMetashapeObjectChunk);

            stage = (Stage) ((javafx.scene.Node) actionEvent.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public void selectImageInTreeView() {
        //selectedItem holds the cameraID associated with the image
        TreeItem<String> selectedItem = chunkTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null &&
                selectedItem.getValue() != null &&
                selectedItem.isLeaf()) {

            String imageName = selectedItem.getValue();
            updateImageText(imageName);
            imgViewText.setText(imgViewText.getText() + " (preview)");


            //set thumbnail as main image, then update to full resolution later
            setThumbnailAsFullImage(selectedItem);

            //if loadImgThread is running, kill it and start a new one
            if(loadImgThread != null && loadImgThread.isActive()){
                loadImgThread.stopThread();
            }

            loadImgThread = new ImgSelectionThread(imageName,this);
            Thread myThread = new Thread(loadImgThread);
            myThread.start();
        }
    }

    private void setThumbnailAsFullImage(TreeItem<String> selectedItem) {
        //use thumbnail as main image
        //used if image is not found, or if larger resolution image is being loaded
        chunkViewerImgView.setImage(selectedItem.getGraphic().
                snapshot(new SnapshotParameters(), null));
    }

    private void initializeChoiceBox() {
        MetashapeObject metashapeObject = this.metashapeObjectChunk.getMetashapeObject();
        String chunkName = this.metashapeObjectChunk.getChunkName();

        //add all items from old checkbox to new checkbox
        this.newChunkSelectionChoiceBox.getItems().addAll(metashapeObject.getChunkNames());

        //initialize checkbox to selected chunk (instead of blank) if possible
        //otherwise, set to first item in list
        try {
            this.newChunkSelectionChoiceBox.setValue(chunkName);
        } catch (Exception e) {
            if (this.newChunkSelectionChoiceBox.getItems() != null) {
                this.newChunkSelectionChoiceBox.setValue(this.newChunkSelectionChoiceBox.getItems().get(0));
            }
        }

        if (this.newChunkSelectionChoiceBox.getItems().size() <= 1) {
            selectChunkButton.setDisable(true);
        }
    }

    private boolean isValidImageType(String path) {
        for (String extension : VALID_EXTENSIONS) {
            if (path.matches("." + extension)) {
                return true;
            }
        }
        return false;
    }

    public void updateSelectChunkButton(ActionEvent actionEvent) {
        //need to keep the unused ActionEvent so we can link this method to the choice box
        String selectedChunk = this.newChunkSelectionChoiceBox.getValue();
        String currentChunkName = this.metashapeObjectChunk.getChunkName();

        if (selectedChunk == null){
            return;
        }

        if (selectedChunk.equals(currentChunkName)) {
            selectChunkButton.setDisable(true);
            selectChunkButton.setText("Chunk already selected");
        } else {
            selectChunkButton.setDisable(false);
            selectChunkButton.setText("Select Chunk");
        }
    }
    public void updateSelectChunkButton() {
        updateSelectChunkButton(new ActionEvent());
    }
}