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

package kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import kintsugi3d.builder.io.ViewSetReader;
import kintsugi3d.builder.io.primaryview.PrimaryViewSelectionModel;
import kintsugi3d.builder.io.primaryview.View;
import kintsugi3d.builder.javafx.controllers.menubar.SearchableTreeView;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class InputSource {
    private static final int THUMBNAIL_SIZE = 30;
    private TreeView<String> treeView;
    protected PrimaryViewSelectionModel primaryViewSelectionModel;
    protected SearchableTreeView searchableTreeView;

    public abstract List<FileChooser.ExtensionFilter> getExtensionFilters();
    abstract ViewSetReader getCameraFileReader();
    public void verifyInfo(File fullResDirectoryOverride)
    {
    }

    public void setSearchableTreeView(SearchableTreeView searchableTreeView){
        this.searchableTreeView = searchableTreeView;
        this.treeView = searchableTreeView.getTreeView();
    }

    public PrimaryViewSelectionModel getPrimarySelectionModel(){
        return this.primaryViewSelectionModel;
    }

    public abstract void initTreeView();

    public abstract void loadProject(String primaryView, double rotate);

    protected void addTreeElems(PrimaryViewSelectionModel primaryViewSelectionModel){
        TreeItem<String> rootItem = new TreeItem<>(primaryViewSelectionModel.getName());
        treeView.setRoot(rootItem);

        List<View> views = primaryViewSelectionModel.getViews();

        for (int i = 0; i < views.size(); i++)
        {
            View view = views.get(i);

            //get parent of camera
            //if parent of camera is a group, create a group node and put it under the root, then add camera to it
            //unless that group already exists, then add the camera to the already created group

            TreeItem<String> destinationItem; //stores the node which the image will be added to
            if(view.group != null)
            {
                List<TreeItem<String>> rootChildren = rootItem.getChildren();
                AtomicBoolean groupAlreadyCreated = new AtomicBoolean(false);
                AtomicReference<TreeItem<String>> matchingItem = new AtomicReference<>();

                rootChildren.forEach(item -> {
                    if (item.getValue().equals(view.group)){
                        groupAlreadyCreated.set(true);
                        matchingItem.set(item);
                    }
                });

                if (groupAlreadyCreated.get()){
                    //add camera to existing group
                    destinationItem = matchingItem.get();
                }
                else{//group has not been created yet
                    TreeItem<String> newGroup = new TreeItem<>(view.group);
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
            TreeItem<String> imageTreeItem = getStringTreeItem(primaryViewSelectionModel.getThumbnails(), i, view.name);
            destinationItem.getChildren().add(imageTreeItem);
        }

        //unroll treeview
        treeView.getRoot().setExpanded(true);
        treeView.getSelectionModel().select(1);
    }

    private static TreeItem<String> getStringTreeItem(List<Image> thumbnailImgList, int i, String imageName) {
        ImageView thumbnailImgView;
        try {
            thumbnailImgView = new ImageView(thumbnailImgList.get(i));
        } catch (IndexOutOfBoundsException e) {
            //thumbnail not found in thumbnailImgList
            thumbnailImgView = new ImageView(new Image(new File("question-mark.png").toURI().toString()));
        }
        thumbnailImgView.setFitWidth(THUMBNAIL_SIZE);
        thumbnailImgView.setFitHeight(THUMBNAIL_SIZE);

        return new TreeItem<>(imageName, thumbnailImgView);
    }
}
