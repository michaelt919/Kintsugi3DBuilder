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

package kintsugi3d.builder.javafx.controllers.menubar.createnewproject;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.javafx.controllers.menubar.ImageThreadable;
import kintsugi3d.builder.javafx.controllers.menubar.SearchableTreeView;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.CurrentProjectInputSource;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.menubar.createnewproject.inputsources.MetashapeProjectInputSource;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ConfirmablePage;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import kintsugi3d.builder.resources.ibr.MissingImagesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the PrimaryViewSelector, which is now used as the orientation view selector
 */
//TODO: Rename to OrientationViewSelectController for clarity?
public class PrimaryViewSelectController extends FXMLPageController implements ConfirmablePage, ImageThreadable
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
    @FXML private VBox orientationControlsVBox;
    @FXML private Label hintTextLabel;

    private InputSource source;
    private HashMap<String, Image> imgCache;
    private ImgSelectionThread loadImgThread;

    @Override
    public void init()
    {
        //TODO: temp hack to make text visible, need to change textflow css?
        imgViewText.setFill(Paint.valueOf("white"));

        chunkTreeView.getSelectionModel().selectedIndexProperty().addListener((a, b, c)->
                selectImageInTreeView());
        this.imgCache = new HashMap<>();
        hintTextLabel.setText(getHintText());
    }

    protected String getHintText()
    {
        return "Select model orientation view";
    }

    @Override
    public void refresh()
    {
        InputSource sharedSource = hostScrollerController.getInfo(ShareInfo.Info.INPUT_SOURCE);

        //TODO: implement .equals()
//        if (!sharedSource.equals(source)){
            source = sharedSource;

            //create an unbound instance and only bind elements when we know chunkTreeView.getRoot() != null
            source.setSearchableTreeView(SearchableTreeView.createUnboundInstance(chunkTreeView, imgSearchTxtField, regexMode));
            try
            {
                source.verifyInfo(null);
                source.initTreeView();
                source.setOrientationViewDefaultSelections(this);
            }
            catch(MissingImagesException mie)
            {
                if (source instanceof MetashapeProjectInputSource){
                    MetashapeProjectInputSource metaSource = (MetashapeProjectInputSource) source;
                    Platform.runLater(() -> metaSource.showMissingImgsAlert(mie));
                }
            }
//        }

        orientationControlsVBox.setVisible(showFixOrientation());
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

        if (selectedItem.getValue() == null) {
            return;
        }
        //if loadImgThread is running, kill it and start a new one
        if (loadImgThread != null && loadImgThread.isActive()) {
            loadImgThread.stopThread();
        }

        if (selectedItem == InputSource.NONE_ITEM) {
            // Hide orientation controls
            orientationControlsVBox.setVisible(false);

            // Don't change the button text to "Skip" when working with an existing project
            if (!(source instanceof CurrentProjectInputSource)) {
                // Set confirm button text
                hostScrollerController.updateNextButtonLabel("Skip");
            }

            imgViewText.setText("Keep model orientation as imported.");

            // Remove any image currently in the thumbnail viewer
            primaryImgView.setImage(null);
            return;
        }

        // Show orientation controls
        orientationControlsVBox.setVisible(showFixOrientation());

        // Set confirm button text
        hostScrollerController.updateNextButtonLabel(canConfirm() ? "Confirm" : "Next");

        String imageName = selectedItem.getValue();
        imgViewText.setText(imageName + " (preview)");

        //set thumbnail as main image, then update to full resolution later
        //don't set thumbnail if img is cached, otherwise would cause a flash
        if (!imgCache.containsKey(imageName)) {
            setThumbnailAsFullImage(selectedItem);
        }

        loadImgThread = new ImgSelectionThread(imageName, this, source.getPrimarySelectionModel());
        Thread myThread = new Thread(loadImgThread);
        myThread.start();
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

    public void setImageRotation(double rotation)
    {
        primaryImgView.setRotate(rotation % 360);
    }

    @Override
    public boolean canConfirm()
    {
        return true;
    }

    @Override
    public void confirmButtonPress() {
        if (confirmCallback != null) {
            confirmCallback.run();
        }

        source.loadProject(getSelectedViewName(), primaryImgView.getRotate());

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

    public String getSelectedViewName()
    {
        TreeItem<String> selection = chunkTreeView.getSelectionModel().getSelectedItem();

        String viewName = null;
        if (selection != InputSource.NONE_ITEM)
        {
            viewName = selection.getValue();
        }

        return viewName;
    }

    protected boolean showFixOrientation()
    {
        return true;
    }
}