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

import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import kintsugi3d.builder.javafx.controllers.menubar.ImgSelectionThread;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import javafx.scene.image.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class ModelExaminationController extends ImgThreadCompatibleController{
    //TODO: NEED TO FIX VIEWER NOT DISPLAYING THE CORRECT IMAGE

    private static final Logger log = LoggerFactory.getLogger(ModelExaminationController.class);
    @FXML private TreeView<String> chunkTreeView;

    @FXML private Text imgViewText;
    static final String[] VALID_EXTENSIONS = {"*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"};

    static final int THUMBNAIL_SIZE = 30;
    private ImgSelectionThread loadImgThread;

    public void initializeChunkSelectionAndTreeView(MetashapeObjectChunk metashapeObjectChunk) {
        this.metashapeObjectChunk = metashapeObjectChunk;

        //add chunk name to tree
        String chunkName = metashapeObjectChunk.getChunkName();
        TreeItem<String> rootItem = new TreeItem<>(chunkName);
        chunkTreeView.setRoot(rootItem);

        //fill thumbnail list
        ArrayList <Image> thumbnailImageList = (ArrayList<Image>) metashapeObjectChunk.loadThumbnailImageList();

        //add full-res images as children to the chunk name in treeview
        ArrayList<Element> cameras = (ArrayList<Element>) metashapeObjectChunk.findThumbnailCameras();

        for (int i = 0; i < cameras.size(); ++i) {
            Element camera = cameras.get(i);
            String imageName = camera.getAttribute("label");

            //get parent of camera
            //if parent of camera is a group, create a group node and put it under the root, then add camera to it
            //unless that group already exists, then add the camera to the already created group

            Element parent = (Element) camera.getParentNode();
            TreeItem<String> destinationItem; //stores the node which the image will be added to
            // (either a group or the root node)
            if(parent.getTagName().equals("group")){
                String groupName = parent.getAttribute("label");//TODO: CURRENTLY ONLY CHECKS TO SEE IF NAMES MATCH

                List<TreeItem<String>> rootChildren = rootItem.getChildren();
                boolean groupAlreadyCreated = false;//boolean to track if group is already present in treeview
                TreeItem<String> matchingItem = null;
                for (TreeItem<String> item : rootChildren){
                    if (item.getValue().equals(groupName)){
                        groupAlreadyCreated = true;
                        matchingItem = item;
                        break;
                    }
                }

                if (groupAlreadyCreated){
                    //add camera to this group
                    destinationItem = matchingItem;
                }
                else{//group has not been created yet
                    TreeItem<String> newGroup = new TreeItem<>(groupName);
                    rootItem.getChildren().add(newGroup);
                    destinationItem = newGroup;
                }
            }
            else{
                //parent is camera, so add image to root node
                //(camera is not part of a group)
                destinationItem = rootItem;
            }

            //set image and thumbnail
            ImageView thumbnailImgView;
            try {
                thumbnailImgView = new ImageView(thumbnailImageList.get(i));
            } catch (IndexOutOfBoundsException e) {
                //thumbnail not found in thumbnailImageList
                thumbnailImgView = new ImageView(new Image(new File("question-mark.png").toURI().toString()));
            }
            thumbnailImgView.setFitWidth(THUMBNAIL_SIZE);
            thumbnailImgView.setFitHeight(THUMBNAIL_SIZE);

            TreeItem<String> imageTreeItem = new TreeItem<>(imageName, thumbnailImgView);
            destinationItem.getChildren().add(imageTreeItem);
        }

        //unroll treeview
        chunkTreeView.getRoot().setExpanded(true);
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

    void updateImageText(String imageName) {
        String psxFilePath = this.metashapeObjectChunk.getPsxFilePath();
        String chunkName = this.metashapeObjectChunk.getChunkName();

//                      set label to: psx name + chunk name + cameraID
        File psxFile = new File(psxFilePath);

        imgViewText.setText("File: " + psxFile.getName() +
                "\nChunk: " + chunkName +
                "\nImage: " + imageName);

        textFlow.setTextAlignment(TextAlignment.LEFT);
    }

    private void setThumbnailAsFullImage(TreeItem<String> selectedItem) {
        //use thumbnail as main image
        //used if image is not found, or if larger resolution image is being loaded
        chunkViewerImgView.setImage(selectedItem.getGraphic().
                snapshot(new SnapshotParameters(), null));
    }

    private boolean isValidImageType(String path) {
        for (String extension : VALID_EXTENSIONS) {
            if (path.matches("." + extension)) {
                return true;
            }
        }
        return false;
    }
}