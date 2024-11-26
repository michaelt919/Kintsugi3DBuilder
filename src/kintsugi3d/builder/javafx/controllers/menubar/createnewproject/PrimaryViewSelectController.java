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

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.menubar.ImageThreadable;
import kintsugi3d.builder.javafx.controllers.menubar.SearchableTreeView;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.MetashapeProjectInputSource;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.CanConfirm;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import kintsugi3d.builder.resources.ibr.MissingImagesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class PrimaryViewSelectController extends FXMLPageController implements CanConfirm, ImageThreadable
{
    //TODO: --> "INFO: index exceeds maxCellCount. Check size calculations for class javafx.scene.control.skin.TreeViewSkin$1"
    //suppress warning?

    private static final Logger log = LoggerFactory.getLogger(PrimaryViewSelectController.class);

    @FXML private AnchorPane hostAnchorPane;

    @FXML private TreeView<String> chunkTreeView;
    @FXML private ImageView primaryImgView;
    @FXML private Text imgViewText;

    @FXML private TextField imgSearchTxtField;
    @FXML private CheckBox regexMode;
    private InputSource source;
    private HashMap<String, Image> imgCache;
    private ImgSelectionThread loadImgThread;

    @Override
    public void init()
    {
        //TODO: temp hack to make text visible, need to change textflow css?
        imgViewText.setFill(Paint.valueOf("white"));

        chunkTreeView.getSelectionModel().selectedIndexProperty().addListener((a, b, c)-> selectImageInTreeView());
        this.imgCache = new HashMap<>();
    }

    @Override
    public void refresh()
    {
        InputSource sharedSource = hostScrollerController.getInfo(ShareInfo.Info.INPUT_SOURCE);

        if (!sharedSource.equals(source)){
            source = sharedSource;

            //create an unbound instance and only bind elements when we know chunkTreeView.getRoot() != null
            source.setSearchableTreeView(SearchableTreeView.createUnboundInstance(chunkTreeView, imgSearchTxtField, regexMode));
            try
            {
                source.verifyInfo(null);
                source.initTreeView();
            }
            catch(MissingImagesException mie)
            {
                if (source instanceof MetashapeProjectInputSource){
                    MetashapeProjectInputSource metaSource = (MetashapeProjectInputSource) source;
                    Platform.runLater(() -> metaSource.showMissingImgsAlert(mie));
                }
            }
        }
    }

    @Override
    public Region getHostRegion() {
        return hostAnchorPane;
    }

    public void selectImageInTreeView() {
        //selectedItem holds the cameraID associated with the image
        TreeItem<String> selectedItem = chunkTreeView.getSelectionModel().getSelectedItem();
        if(selectedItem == null){
            return;
        }
        if (selectedItem == chunkTreeView.getRoot()){
            selectedItem.setExpanded(true);
            return;
        }

        if (!selectedItem.isLeaf()){
            selectedItem.setExpanded(!selectedItem.isExpanded());
            return;
        }

        if (selectedItem.getValue() != null) {

            String imageName = selectedItem.getValue();
            imgViewText.setText(imageName + " (preview)");

            //set thumbnail as main image, then update to full resolution later
            //don't set thumbnail if img is cached, otherwise would cause a flash
            if(!imgCache.containsKey(imageName)){
                setThumbnailAsFullImage(selectedItem);
            }

            //if loadImgThread is running, kill it and start a new one
            if(loadImgThread != null && loadImgThread.isActive()){
                loadImgThread.stopThread();
            }

            loadImgThread = new ImgSelectionThread(imageName,this, source.getPrimarySelectionModel());
            Thread myThread = new Thread(loadImgThread);
            myThread.start();
        }
    }

    private void setThumbnailAsFullImage(TreeItem<String> selectedItem) {
        //use thumbnail as main image
        //used if image is not found, or if larger resolution image is being loaded
        ImageView imageView = (ImageView) selectedItem.getGraphic();
        primaryImgView.setImage(imageView.getImage());
    }

    @FXML
    private void rotateRight() {
        //rotate in 90 degree increments
        primaryImgView.setRotate((primaryImgView.getRotate() + 90) % 360);
    }

    @FXML
    private void rotateLeft() {
        //rotate in 90 degree increments
        primaryImgView.setRotate((primaryImgView.getRotate() - 90) % 360);
    }

    @Override
    public void confirmButtonPress() {
        if (loadStartCallback != null) {
            loadStartCallback.run();
        }

        if (viewSetCallback != null) {
            //"force" the user to save their project (user can still cancel saving)
            MultithreadModels.getInstance().getIOModel().addViewSetLoadCallback(
                viewSet ->viewSetCallback.accept(viewSet));
        }

        TreeItem<String> selection = chunkTreeView.getSelectionModel().getSelectedItem();

        String primaryView = null;
        if (selection != InputSource.NONE_ITEM)
        {
            primaryView = selection.getValue();
        }

        source.loadProject(primaryView, primaryImgView.getRotate());

        WelcomeWindowController.getInstance().hide();

        Window window = hostAnchorPane.getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    @Override
    public boolean isNextButtonValid(){
        return true;
    }

    @Override
    public ImageView getImageView() {
        return primaryImgView;
    }

    @Override
    public String getImageViewText() {
        return imgViewText.getText();
    }

    @Override
    public void setImageViewText(String txt) {
        imgViewText.setText(txt);
    }

    @Override
    public Map<String, Image> getImageCache() {
        return imgCache;
    }
}