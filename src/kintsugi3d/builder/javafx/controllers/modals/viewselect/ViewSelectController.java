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

package kintsugi3d.builder.javafx.controllers.modals.viewselect;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import kintsugi3d.builder.io.primaryview.View;
import kintsugi3d.builder.io.primaryview.ViewSelectionModel;
import kintsugi3d.builder.javafx.controllers.paged.DataReceiverPageControllerBase;
import kintsugi3d.builder.javafx.controllers.sidebar.SearchableTreeView;
import kintsugi3d.builder.javafx.util.ImageThreadable;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ViewSelectController extends DataReceiverPageControllerBase<ViewSelectable> implements ImageThreadable
{
    //TODO: --> "INFO: index exceeds maxCellCount. Check size calculations for class javafx.scene.control.skin.TreeViewSkin$1"
    //suppress warning?

    private static final int THUMBNAIL_SIZE = 30;
    private static final TreeItem<String> NONE_ITEM = new TreeItem<>("Keep Imported Orientation");

    @FXML
    protected TreeView<String> chunkTreeView;
    @FXML
    protected ImageView primaryImgView;
    @FXML
    protected Text imgViewText;
    @FXML
    protected TextField imgSearchTxtField;
    @FXML
    protected CheckBox regexMode;
    @FXML
    protected Pane orientationControls;
    @FXML
    protected Label hintTextLabel;
    protected ViewSelectable newData;
    protected ViewSelectable data;
    protected HashMap<String, Image> imgCache;
    @FXML
    private Pane hostPane;
    private ImageSelectionThread loadImgThread;

    protected abstract String getHintText();

    protected abstract boolean allowViewRotation();

    protected abstract boolean allowNullViewSelection();

    @Override
    public Region getRootNode()
    {
        return hostPane;
    }

    @Override
    public void initPage()
    {
        this.imgCache = new HashMap<>();

        //TODO: temp hack to make text visible, need to change textflow css?
        imgViewText.setFill(Paint.valueOf("white"));

        hintTextLabel.setText(getHintText());
        orientationControls.setVisible(allowViewRotation());

        chunkTreeView.getSelectionModel().selectedIndexProperty()
            .addListener((a, b, c) -> selectImageInTreeView());

        setCanAdvance(true);
    }

    @Override
    public void refresh()
    {
        if (newData != null && newData.needsRefresh(data))
        {
            // check if sources are equal so we don't have to unzip images multiple times
            // sometimes this saves processing time, other times it leads to buggy behavior :/
            // really the only use case is saving time if a user flips back and forth between primary view selection and the previous page
            data = newData;

            //create an unbound instance and only bind elements when we know chunkTreeView.getRoot() != null
            SearchableTreeView searchableTreeView =
                SearchableTreeView.createUnboundInstance(chunkTreeView, imgSearchTxtField, regexMode);
            data.setModalWindow(getRootNode().getScene().getWindow());
            data.loadForViewSelection(viewSelectionModel ->
            {
                addTreeElems(searchableTreeView, viewSelectionModel);
                searchableTreeView.bind();

                if (data.getViewSelection() == null)
                {
                    searchableTreeView.getTreeView().getSelectionModel().select(1);
                    setImageRotation(0);
                }
                else
                {
                    setDefaultSelection(searchableTreeView);
                    setImageRotation(allowViewRotation() ? data.getViewRotation() : 0);
                }
            });
        }
        else
        {
            // Just update the reference but don't refresh.
            data = newData;
        }
    }

    private void setDefaultSelection(SearchableTreeView searchableTreeView)
    {
        // Set the initial selection to what is currently being used
        TreeItem<String> selectionItem = NONE_ITEM;
        String viewName = data.getViewSelection();
        if (viewName != null)
        {
            for (int i = 0; i < searchableTreeView.getTreeView().getExpandedItemCount(); i++)
            {
                TreeItem<String> item = searchableTreeView.getTreeView().getTreeItem(i);
                if (Objects.equals(item.getValue(), viewName))
                {
                    selectionItem = item;
                    break;
                }
            }
        }

        searchableTreeView.getTreeView().getSelectionModel().select(selectionItem);
    }

    private void addTreeElems(SearchableTreeView searchableTreeView, ViewSelectionModel viewSelectionModel)
    {
        TreeItem<String> rootItem = new TreeItem<>(viewSelectionModel.getName());
        searchableTreeView.getTreeView().setRoot(rootItem);

        List<View> views = viewSelectionModel.getViews();

        if (allowNullViewSelection())
        {
            rootItem.getChildren().add(NONE_ITEM);
        }

        for (View view : views)
        {
            //get parent of camera
            //if parent of camera is a group, create a group node and put it under the root, then add camera to it
            //unless that group already exists, then add the camera to the already created group

            TreeItem<String> destinationItem; //stores the node which the image will be added to
            if (view.group != null)
            {
                List<TreeItem<String>> rootChildren = rootItem.getChildren();
                AtomicBoolean groupAlreadyCreated = new AtomicBoolean(false);
                AtomicReference<TreeItem<String>> matchingItem = new AtomicReference<>();

                rootChildren.forEach(item ->
                {
                    if (item.getValue().equals(view.group))
                    {
                        groupAlreadyCreated.set(true);
                        matchingItem.set(item);
                    }
                });

                if (groupAlreadyCreated.get())
                {
                    //add camera to existing group
                    destinationItem = matchingItem.get();
                }
                else
                {//group has not been created yet
                    TreeItem<String> newGroup = new TreeItem<>(view.group);
                    rootItem.getChildren().add(newGroup);
                    destinationItem = newGroup;
                }
            }
            else
            {
                //parent is camera, so add image to root node
                //(camera is not part of a group)
                destinationItem = rootItem;
            }

            //set image and thumbnail
            TreeItem<String> imageTreeItem = createTreeItem(viewSelectionModel.getThumbnails(), view);
            destinationItem.getChildren().add(imageTreeItem);
        }

        //unroll treeview
        searchableTreeView.getTreeView().getRoot().setExpanded(true);
    }

    private static TreeItem<String> createTreeItem(Map<Integer, Image> thumbnailImgList, View view)
    {
        ImageView thumbnailImgView;
        Image img = thumbnailImgList.get(view.id);
        thumbnailImgView = new ImageView(Objects.requireNonNullElseGet(img, // null if thumbnail not found in thumbnailImgList
            () -> new Image(new File("question-mark.png").toURI().toString())));
        thumbnailImgView.setFitWidth(THUMBNAIL_SIZE);
        thumbnailImgView.setFitHeight(THUMBNAIL_SIZE);

        return new TreeItem<>(view.name, thumbnailImgView);
    }

    @FXML
    public void selectImageInTreeView()
    {
        // selectedItem holds the cameraID associated with the image
        TreeItem<String> selectedItem = chunkTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null)
        {
            return;
        }
        if (selectedItem == chunkTreeView.getRoot())
        {
            selectedItem.setExpanded(true);
            return;
        }

        if (!selectedItem.isLeaf())
        {
            selectedItem.setExpanded(!selectedItem.isExpanded());
            return;
        }

        if (selectedItem.getValue() != null)
        {
            //if loadImgThread is running, kill it and start a new one
            if (loadImgThread != null && loadImgThread.isActive())
            {
                loadImgThread.stopThread();
            }

            if (selectedItem == NONE_ITEM)
            {
                // Hide orientation controls
                orientationControls.setVisible(false);

                if (data.getAdvanceLabelOverride() != null)
                {
                    // Set confirm button text
                    setAdvanceLabelOverride(data.getAdvanceLabelOverride());
                }

                imgViewText.setText("Keep model orientation as imported.");

                // Remove any image currently in the thumbnail viewer
                primaryImgView.setImage(null);
                return;
            }
            else
            {
                // Show orientation controls
                orientationControls.setVisible(allowViewRotation());

                // Set confirm button text
                setAdvanceLabelOverride(null);
            }

            String imageName = selectedItem.getValue();
            imgViewText.setText(imageName + " (preview)");

            //set thumbnail as main image, then update to full resolution later
            //don't set thumbnail if img is cached, otherwise would cause a flash
            if (!imgCache.containsKey(imageName))
            {
                setThumbnailAsFullImage(selectedItem);
            }

            loadImgThread = new ImageSelectionThread(imageName, this, data.getViewSelectionModel());
            Thread myThread = new Thread(loadImgThread);
            myThread.start();
        }
    }

    private void setThumbnailAsFullImage(TreeItem<String> selectedItem)
    {
        //use thumbnail as main image
        //used if image is not found, or if larger resolution image is being loaded
        ImageView imageView = (ImageView) selectedItem.getGraphic();
        primaryImgView.setImage(imageView.getImage());
    }

    @FXML
    private void rotateRight()
    {
        //rotate in 90 degree increments
        primaryImgView.setRotate((primaryImgView.getRotate() + 90) % 360);
    }

    @FXML
    private void rotateLeft()
    {
        //rotate in 90 degree increments
        primaryImgView.setRotate((primaryImgView.getRotate() - 90) % 360);
    }

    private void setImageRotation(double rotation)
    {
        primaryImgView.setRotate(rotation % 360);
    }

    @Override
    public ImageView getImageView()
    {
        return primaryImgView;
    }

    @Override
    public String getImageViewText()
    {
        return imgViewText.getText();
    }

    @Override
    public void setImageViewText(String txt)
    {
        imgViewText.setText(txt);
    }

    @Override
    public Map<String, Image> getImageCache()
    {
        return imgCache;
    }

    protected String getSelectedViewName()
    {
        TreeItem<String> selection = chunkTreeView.getSelectionModel().getSelectedItem();

        String viewName = null;
        if (selection != NONE_ITEM)
        {
            viewName = selection.getValue();
        }

        return viewName;
    }

    @Override
    public void receiveData(ViewSelectable data)
    {
        this.newData = data;
    }
}
