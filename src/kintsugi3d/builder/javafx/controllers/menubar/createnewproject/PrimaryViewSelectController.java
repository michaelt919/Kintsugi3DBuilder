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

package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import kintsugi3d.builder.javafx.ProjectIO;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObject;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.CanConfirm;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class PrimaryViewSelectController extends FXMLPageController implements CanConfirm {
    //TODO: --> "INFO: index exceeds maxCellCount. Check size calculations for class javafx.scene.control.skin.TreeViewSkin$1"
    //suppress warning?

    private static final Logger log = LoggerFactory.getLogger(PrimaryViewSelectController.class);

    @FXML private AnchorPane hostAnchorPane;

    @FXML private TreeView<String> chunkTreeView;
    @FXML private ImageView chunkViewerImgView;
    @FXML private Text imgViewText;
    @FXML public TextFlow textFlow;

    @FXML private Button selectChunkButton;
    @FXML private ChoiceBox<String> newChunkSelectionChoiceBox;//allows the user to select a new chunk to view
    private MetashapeObjectChunk metashapeObjectChunk;
    private File cameraFile;
    private Document cameraDocument;
    private ImgSelectionThread loadImgThread;
    static final String[] VALID_EXTENSIONS = {"*.jpg", "*.jpeg", "*.png", "*.gif", "*.tif", "*.tiff", "*.png", "*.bmp", "*.wbmp"};
    static final int THUMBNAIL_SIZE = 30;

    @Override
    public void init() {
        //TODO: temp hack to make text visible
        imgViewText.setFill(Paint.valueOf("white"));
    }

    @Override
    public void refresh() {
        //metashape import path loads from metashape project
        MetashapeObjectChunk sharedChunk = hostScrollerController.getInfo(ShareInfo.Info.METASHAPE_OBJ_CHUNK);
        File sharedCamFile = hostScrollerController.getInfo(ShareInfo.Info.CAM_FILE);
        if(hostPage.getPrevPage() == hostScrollerController.getPage("/fxml/menubar/createnewproject/MetashapeImport.fxml")){
            if(this.metashapeObjectChunk == null ||
                    this.metashapeObjectChunk != sharedChunk) {
                initializeChunkSelectionAndTreeView(sharedChunk);
            }
        }

        //custom import path loads from cameras xml file
        else{
            if(cameraFile == null || cameraFile != sharedCamFile){
                //TODO: figure this out later
            }
        }
    }

    public void initializeChunkSelectionAndTreeView(MetashapeObjectChunk metashapeObjectChunk) {
        this.metashapeObjectChunk = metashapeObjectChunk;

        //add chunk name to tree
        String chunkName = metashapeObjectChunk.getChunkName();
        TreeItem<String> rootItem = new TreeItem<>(chunkName);
        chunkTreeView.setRoot(rootItem);

//        //initialize options in new chunk selection choice box (cannot be done before chunkName is set)
//        initializeChoiceBox();
//        this.newChunkSelectionChoiceBox.setOnAction(this::updateSelectChunkButton);
//
//        //disable select chunk button if selected chunk (in choice box) matches the current chunk
//        updateSelectChunkButton();

        //fill thumbnail list
        ArrayList <Image> thumbnailImageList = (ArrayList<Image>) metashapeObjectChunk.loadThumbnailImageList();

        //add full-res images as children to the chunk name in treeview
        ArrayList<Element> cameras = (ArrayList<Element>) metashapeObjectChunk.findEnabledCameras();

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

    public void selectChunk(ActionEvent actionEvent) throws IOException {
        Scene scene;
        Parent root;
        Stage stage;

        String currentChunkName = this.metashapeObjectChunk.getChunkName();
        String selectedChunkName = this.newChunkSelectionChoiceBox.getValue();

        MetashapeObject metashapeObject = this.metashapeObjectChunk.getMetashapeObject();

        if (!selectedChunkName.equals(currentChunkName)) {//only change scene if switching to new chunk
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/menubar/PrimaryViewSelect.fxml"));
            root = fxmlLoader.load();
            PrimaryViewSelectController primaryViewSelectController = fxmlLoader.getController();

            //TODO: actually retrieve model id instead of defaulting to 0
            MetashapeObjectChunk newMetashapeObjectChunk = new MetashapeObjectChunk(metashapeObject, selectedChunkName, 0);
            primaryViewSelectController.initializeChunkSelectionAndTreeView(newMetashapeObjectChunk);

            stage = (Stage) ((javafx.scene.Node) actionEvent.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    @Override
    public Region getHostRegion() {
        return hostAnchorPane;
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

    public void updateImageText(String imageName) {
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

    //TODO: bring back chunk selection within this modal if we decide we want that
//    public void updateSelectChunkButton() {
//        //need to keep the unused ActionEvent so we can link this method to the choice box
//        String selectedChunk = this.newChunkSelectionChoiceBox.getValue();
//        String currentChunkName = this.metashapeObjectChunk.getChunkName();
//
//        if (selectedChunk == null){
//            return;
//        }
//
//        if (selectedChunk.equals(currentChunkName)) {
//            selectChunkButton.setDisable(true);
//            selectChunkButton.setText("Chunk already selected");
//        } else {
//            selectChunkButton.setDisable(false);
//            selectChunkButton.setText("Select Chunk");
//        }
//    }

    @Override
    public void confirmButtonPress() {
        ProjectIO.handleException("Congration you done it", new Exception("nothing to show"));
    }

    @Override
    public boolean isNextButtonValid(){
        return true;
        //TODO: change this if necessary
    }

    public ImageView getChunkViewerImgView() {
        return chunkViewerImgView;
    }

    public Text getImgViewText() {
        return imgViewText;
    }

    public MetashapeObjectChunk getMetashapeObjectChunk() {
        return metashapeObjectChunk;
    }
}