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
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.PrimaryViewSelectController;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class InputSource {
    private static final int THUMBNAIL_SIZE = 30;
    private TreeView<String> treeView;
    protected PrimaryViewSelectionModel primaryViewSelectionModel;
    protected SearchableTreeView searchableTreeView;
    public static final TreeItem<String> NONE_ITEM = new TreeItem<>("Keep Imported Orientation");
    private boolean includeNoneItem = true;

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

        if (includeNoneItem)
        {
            rootItem.getChildren().add(NONE_ITEM);
        }

        for (View view : views) {
            //get parent of camera
            //if parent of camera is a group, create a group node and put it under the root, then add camera to it
            //unless that group already exists, then add the camera to the already created group

            TreeItem<String> destinationItem; //stores the node which the image will be added to
            if (view.group != null) {
                List<TreeItem<String>> rootChildren = rootItem.getChildren();
                AtomicBoolean groupAlreadyCreated = new AtomicBoolean(false);
                AtomicReference<TreeItem<String>> matchingItem = new AtomicReference<>();

                rootChildren.forEach(item -> {
                    if (item.getValue().equals(view.group)) {
                        groupAlreadyCreated.set(true);
                        matchingItem.set(item);
                    }
                });

                if (groupAlreadyCreated.get()) {
                    //add camera to existing group
                    destinationItem = matchingItem.get();
                } else {//group has not been created yet
                    TreeItem<String> newGroup = new TreeItem<>(view.group);
                    rootItem.getChildren().add(newGroup);
                    destinationItem = newGroup;
                }
            } else {
                //parent is camera, so add image to root node
                //(camera is not part of a group)
                destinationItem = rootItem;
            }

            //set image and thumbnail
            TreeItem<String> imageTreeItem = createTreeItem(primaryViewSelectionModel.getThumbnails(), view);
            destinationItem.getChildren().add(imageTreeItem);
        }

        //unroll treeview
        treeView.getRoot().setExpanded(true);
    }

    private static TreeItem<String> createTreeItem(List<Image> thumbnailImgList, View view) {
        ImageView thumbnailImgView;
        try {
            //this assumes that view id's are parallel with thumbnailImgList indices
            //this is usually true (haven't found a case where it isn't)
            //a more precise implementation would read the doc.xml inside thumbnails.zip and create a map
            //  from camera id's to img paths
            thumbnailImgView = new ImageView(thumbnailImgList.get(view.id));
        } catch (IndexOutOfBoundsException e) {
            //thumbnail not found in thumbnailImgList
            thumbnailImgView = new ImageView(new Image(new File("question-mark.png").toURI().toString()));
        }
        thumbnailImgView.setFitWidth(THUMBNAIL_SIZE);
        thumbnailImgView.setFitHeight(THUMBNAIL_SIZE);

        return new TreeItem<>(view.name, thumbnailImgView);
    }

    @Override
    public abstract boolean equals(Object obj);

    public void setOrientationViewDefaultSelections(PrimaryViewSelectController controller)
    {
        treeView.getSelectionModel().select(1);
        controller.setImageRotation(0);
    }

    public void setIncludeNoneItem(boolean include)
    {
        this.includeNoneItem = include;
    }
}
