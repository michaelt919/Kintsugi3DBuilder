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

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//wrapper class for controllers which need to use the ImgSelectionThread class
public abstract class ImgThreadCompatibleController {
    private static final int THUMBNAIL_SIZE = 30;
    public ImageView chunkViewerImgView;
    public Text imgViewText;
    public MetashapeObjectChunk metashapeObjectChunk;
    public TextFlow textFlow;
    public TreeView<String> chunkTreeView;


    public void updateImageText(String imageName) {
        String psxFilePath = this.metashapeObjectChunk.getPsxFilePath();
        String chunkName = this.metashapeObjectChunk.getChunkName();

//      set label to: psx name + chunk name + cameraID
        File psxFile = new File(psxFilePath);

        imgViewText.setText("File: " + psxFile.getName() +
                "\nChunk: " + chunkName +
                "\nImage: " + imageName);

        textFlow.setTextAlignment(TextAlignment.LEFT);
    }

    protected void initTreeView() {
        //add chunk name to tree
        String chunkName = metashapeObjectChunk.getChunkName();
        TreeItem<String> rootItem = new TreeItem<>(chunkName);
        chunkTreeView.setRoot(rootItem);

        //fill thumbnail list
        Map<String, Image> thumbnailImagePairsList = metashapeObjectChunk.loadThumbnailImageWithFileNamesList();

        //add full-res images as children to the chunk name in treeview
        //cameras taken from chunk.zip --> doc.xml
        ArrayList<Element> cameras = (ArrayList<Element>) metashapeObjectChunk.findThumbnailCameras();

        for (int i = 0; i < cameras.size(); ++i) {
            Element camera = cameras.get(i);
            String imageName = camera.getAttribute("label");

            TreeItem<String> destinationItem = addCameraToTreeView(rootItem, camera);

            //set image and thumbnail
            ImageView thumbnailImgView;
            try {
                int cameraID = Integer.parseInt(camera.getAttribute("id"));
                String thumbnailImgPath = metashapeObjectChunk.getThumbnailImgPathFromCamId(cameraID);

                thumbnailImgView = new ImageView(thumbnailImagePairsList.get(thumbnailImgPath));
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

    private static TreeItem<String> addCameraToTreeView(TreeItem<String> rootItem, Element camera) {
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
        return destinationItem;
    }
}
