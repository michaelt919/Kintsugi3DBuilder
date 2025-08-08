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

package kintsugi3d.builder.javafx.controllers.modals;

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
import kintsugi3d.builder.javafx.controllers.menubar.ImageThreadable;
import kintsugi3d.builder.javafx.controllers.menubar.SearchableTreeView;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.ImgSelectionThread;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.CurrentProjectInputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.MetashapeProjectInputSource;
import kintsugi3d.builder.javafx.controllers.paged.DataReceiverPageControllerBase;
import kintsugi3d.builder.resources.project.MissingImagesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class ViewSelectController extends DataReceiverPageControllerBase<InputSource> implements ImageThreadable
{
    private static final Logger log = LoggerFactory.getLogger(ViewSelectController.class);
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
    protected VBox orientationControlsVBox;
    @FXML
    protected Label hintTextLabel;
    protected InputSource newSource;
    protected InputSource source;
    protected HashMap<String, Image> imgCache;
    @FXML
    private AnchorPane hostAnchorPane;
    private ImgSelectionThread loadImgThread;

    protected abstract String getHintText();

    protected abstract boolean showFixOrientation();

    @Override
    public Region getRootNode()
    {
        return hostAnchorPane;
    }

    @Override
    public void initPage()
    {
        //TODO: temp hack to make text visible, need to change textflow css?
        imgViewText.setFill(Paint.valueOf("white"));

        chunkTreeView.getSelectionModel().selectedIndexProperty()
            .addListener((a, b, c) -> selectImageInTreeView());
        this.imgCache = new HashMap<>();
        hintTextLabel.setText(getHintText());

        setCanAdvance(true);
    }

    @Override
    public void refresh()
    {
        if (!Objects.equals(newSource, source))
        { //check if sources are equal so we don't have to unzip images multiple times
            //sometimes this saves processing time, other times it leads to buggy behavior :/
            //really the only use case is saving time if a user flips back and forth between primary view selection and the previous page
            source = newSource;

            //create an unbound instance and only bind elements when we know chunkTreeView.getRoot() != null
            source.setSearchableTreeView(SearchableTreeView.createUnboundInstance(chunkTreeView, imgSearchTxtField, regexMode));
            try
            {
                source.verifyInfo();
                source.initTreeView();
                source.setOrientationViewDefaultSelections(this);
            }
            catch (MissingImagesException mie)
            {
                if (source instanceof MetashapeProjectInputSource)
                {
                    MetashapeProjectInputSource metaSource = (MetashapeProjectInputSource) source;
                    Platform.runLater(() -> metaSource.showMissingImgsAlert(mie, getPageFrameController()));
                }
            }
        }

        orientationControlsVBox.setVisible(showFixOrientation());
    }

    public void selectImageInTreeView()
    {
        //selectedItem holds the cameraID associated with the image
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

            if (selectedItem == InputSource.NONE_ITEM)
            {
                // Hide orientation controls
                orientationControlsVBox.setVisible(false);

                // Don't change the button text to "Skip" when working with an existing project
                if (!(source instanceof CurrentProjectInputSource))
                {
                    // Set confirm button text
                    setAdvanceLabelOverride("Skip");
                }

                imgViewText.setText("Keep model orientation as imported.");

                // Remove any image currently in the thumbnail viewer
                primaryImgView.setImage(null);
                return;
            }
            else
            {
                // Show orientation controls
                orientationControlsVBox.setVisible(showFixOrientation());

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

            loadImgThread = new ImgSelectionThread(imageName, this, source.getPrimarySelectionModel());
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

    public void setImageRotation(double rotation)
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

    @Override
    public void receiveData(InputSource data)
    {
        this.newSource = data;
    }
}
