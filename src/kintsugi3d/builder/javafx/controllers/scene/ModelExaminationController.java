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

package kintsugi3d.builder.javafx.controllers.scene;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import kintsugi3d.builder.javafx.controllers.menubar.ImgSelectionThread;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.gl.javafx.FramebufferView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class ModelExaminationController extends ImgThreadCompatibleController{
    private static final Logger log = LoggerFactory.getLogger(ModelExaminationController.class);
    private ImgSelectionThread loadImgThread;

    @FXML private FramebufferView framebufferView;

    public void init(MetashapeObjectChunk metashapeObjectChunk, Stage injectedStage){
        this.metashapeObjectChunk = metashapeObjectChunk;
        initTreeView();
        this.framebufferView.registerKeyAndWindowEventsFromStage(injectedStage);
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

    public void skipOnboarding(ActionEvent actionEvent) {
        //TODO: imp.
    }

    public void aboutObjectOrientation() {
        //TODO: finish about txt here
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        List<String> lines = null;
        try {
            lines = Files.readAllLines(new File("about-object-orientation.txt").toPath());
            String contentText = String.join(System.lineSeparator(), lines);
            alert.setContentText(contentText);
            alert.setHeaderText("About Object Orientation");
            alert.setTitle("About Object Orientation");
            alert.show();
        } catch (IOException e) {
            log.error("An error occurred showing about window:", e);
        }

    }

    public void rotateImage() {
        //rotate in 90 degree increments
        chunkViewerImgView.setRotate((chunkViewerImgView.getRotate() + 90) % 360);
    }

    public void loadData(ActionEvent actionEvent) {
        //TODO: imp.
        //activates when the continue button is pressed
    }
}