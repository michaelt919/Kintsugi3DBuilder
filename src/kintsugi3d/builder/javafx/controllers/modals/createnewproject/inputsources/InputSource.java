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

package kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources;

import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import kintsugi3d.builder.io.primaryview.PrimaryViewSelectionModel;
import kintsugi3d.builder.io.primaryview.View;
import kintsugi3d.builder.javafx.controllers.menubar.SearchableTreeView;
import kintsugi3d.builder.javafx.controllers.modals.ViewSelectController;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.OrientationViewSelectController;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class InputSource {
    protected PrimaryViewSelectionModel primaryViewSelectionModel;

    protected SearchableTreeView searchableTreeView;
    public static final TreeItem<String> NONE_ITEM = new TreeItem<>("Keep Imported Orientation");
    private boolean includeNoneItem = true;

    private String primaryView;
    private double primaryViewRotation;

    private static final int THUMBNAIL_SIZE = 30;

    public abstract List<FileChooser.ExtensionFilter> getExtensionFilters();

    public abstract void initTreeView();

    public abstract void loadProject();

    @Override
    public abstract boolean equals(Object obj);

    public void verifyInfo()
    {
    }

    public void setSearchableTreeView(SearchableTreeView searchableTreeView){
        this.searchableTreeView = searchableTreeView;
    }

    public PrimaryViewSelectionModel getPrimarySelectionModel(){
        return this.primaryViewSelectionModel;
    }

    public void setOrientationViewDefaultSelections(ViewSelectController controller)
    {
        searchableTreeView.getTreeView().getSelectionModel().select(1);
        controller.setImageRotation(0);
    }

    public void setIncludeNoneItem(boolean include)
    {
        this.includeNoneItem = include;
    }

    /**
     * Return the known masks directory.
     * @return the masks directory
     */
    public abstract File getMasksDirectory();

    /**
     * For setting the initial directory of the masks directory chooser. Not guaranteed to be the actual masks directory.
     * @return a directory which will hopefully be close to the actual masks directory so the user will find it quickly
     */
    public abstract File getInitialMasksDirectory();

    public abstract boolean doEnableProjectMasksButton();

    public abstract void setMasksDirectory(File file);

    protected void addTreeElems(PrimaryViewSelectionModel primaryViewSelectionModel){
        TreeItem<String> rootItem = new TreeItem<>(primaryViewSelectionModel.getName());
        searchableTreeView.getTreeView().setRoot(rootItem);

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
        searchableTreeView.getTreeView().getRoot().setExpanded(true);
    }

    private static TreeItem<String> createTreeItem(Map<Integer, Image> thumbnailImgList, View view) {
        ImageView thumbnailImgView;
        Image img = thumbnailImgList.get(view.id);
        if (img != null){
            thumbnailImgView = new ImageView(img);
        }
        else{
            //thumbnail not found in thumbnailImgList
            thumbnailImgView = new ImageView(new Image(new File("question-mark.png").toURI().toString()));
        }
        thumbnailImgView.setFitWidth(THUMBNAIL_SIZE);
        thumbnailImgView.setFitHeight(THUMBNAIL_SIZE);

        return new TreeItem<>(view.name, thumbnailImgView);
    }

    public String getPrimaryView()
    {
        return primaryView;
    }

    public void setPrimaryView(String primaryView, double primaryViewRotation)
    {
        this.primaryView = primaryView;
        this.primaryViewRotation = primaryViewRotation;
    }

    public double getPrimaryViewRotation()
    {
        return primaryViewRotation;
    }
}
