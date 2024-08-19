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
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import kintsugi3d.builder.io.ViewSetReaderFromRealityCaptureCSV;
import kintsugi3d.builder.io.primaryview.AgisoftPrimaryViewSelectionModel;
import kintsugi3d.builder.io.primaryview.GenericPrimaryViewSelectionModel;
import kintsugi3d.builder.io.primaryview.PrimaryViewSelectionModel;
import kintsugi3d.builder.io.primaryview.View;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.ProjectIO;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.CanConfirm;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.ShareInfo;
import kintsugi3d.builder.javafx.controllers.scene.WelcomeWindowController;
import kintsugi3d.builder.resources.ibr.MissingImagesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PrimaryViewSelectController extends FXMLPageController implements CanConfirm
{
    //TODO: --> "INFO: index exceeds maxCellCount. Check size calculations for class javafx.scene.control.skin.TreeViewSkin$1"
    //suppress warning?

    private static final Logger log = LoggerFactory.getLogger(PrimaryViewSelectController.class);

    @FXML private AnchorPane hostAnchorPane;

    @FXML private TreeView<String> chunkTreeView;
    @FXML private ImageView chunkViewerImgView;
    @FXML private Text imgViewText;
    private MetashapeObjectChunk metashapeObjectChunk;
    private File cameraFile;
    private File objFile;
    private File photosDir;
    private File fullResOverride;
    private boolean doSkipMissingCams;
    private Document cameraDocument;
    PrimaryViewSelectionModel primaryViewSelectionModel;
    private HashMap<String, Image> imgCache;
    private ImgSelectionThread loadImgThread;
    static final int THUMBNAIL_SIZE = 30;
    private boolean isMetashapeImport;

    @Override
    public void init()
    {
        //TODO: temp hack to make text visible, need to change textflow css?
        imgViewText.setFill(Paint.valueOf("white"));
        this.primaryViewSelectionModel = null;
        this.metashapeObjectChunk = null;
        this.cameraDocument = null;
        this.cameraFile = null;

        imgCache = new HashMap<>();
    }

    @Override
    public void refresh()
    {
        MetashapeObjectChunk sharedChunk = hostScrollerController.getInfo(ShareInfo.Info.METASHAPE_OBJ_CHUNK);
        File sharedCamFile = hostScrollerController.getInfo(ShareInfo.Info.CAM_FILE);
        doSkipMissingCams = false;

        boolean usingMetashapeImport =
            hostPage.getPrevPage() == hostScrollerController.getPage("/fxml/menubar/createnewproject/MetashapeImport.fxml");

        //metashape import path loads from metashape project
        if(usingMetashapeImport)
        {
            if(this.metashapeObjectChunk == null || !Objects.equals(this.metashapeObjectChunk, sharedChunk))
            {
                updateSharedInfo();

                try
                {
                    verifyInfo(null);
                    initTreeView(sharedChunk);
                }
                catch(MissingImagesException mie)
                {
                    Platform.runLater(() ->
                        showMissingImgsAlert(metashapeObjectChunk, mie));
                }
            }
        }
        //custom import path loads from cameras xml file
        else
        {
            if(cameraFile == null || !cameraFile.equals(sharedCamFile))
            {
                updateSharedInfo();
                initTreeView(sharedCamFile);
            }
        }

        if(primaryViewSelectionModel instanceof AgisoftPrimaryViewSelectionModel){
            AgisoftPrimaryViewSelectionModel model = (AgisoftPrimaryViewSelectionModel) primaryViewSelectionModel;
            cameraDocument = model.getCamDocument().orElse(null);
        }
    }

    public void initTreeView(File camFile) {
        cameraFile = camFile;

        try {
            if (camFile.getName().endsWith(".xml")) // Agisoft Metashape
            {
                primaryViewSelectionModel = AgisoftPrimaryViewSelectionModel.createInstance(camFile);
            }
            else if (cameraFile.getName().endsWith(".csv")) // RealityCapture
            {
                primaryViewSelectionModel = GenericPrimaryViewSelectionModel.createInstance(cameraFile.getName(),
                    ViewSetReaderFromRealityCaptureCSV.getInstance().readFromFile(cameraFile, objFile, photosDir));
            }
            else
            {
                ProjectIO.handleException("Error initializing primary view selection.",
                    new IllegalArgumentException(MessageFormat.format("File extension not recognized for {0}", cameraFile.getName())));
                return;
            }

            addTreeElems(primaryViewSelectionModel);

        } catch (Exception e) {
            ProjectIO.handleException("Error initializing primary view selection.", e);
        }

    }

    //TODO: either refactor this so that this function relies on this.metashapeObjectChunk or remove UnzipFileSelectionController as it is no longer needed
    public void initTreeView(MetashapeObjectChunk metashapeObjectChunk)
    {
        String chunkName = metashapeObjectChunk.getChunkName();

        ArrayList <Image> thumbnailImageList = (ArrayList<Image>) metashapeObjectChunk.loadThumbnailImageList();
        ArrayList<Element> cameras = (ArrayList<Element>) metashapeObjectChunk.findEnabledCameras();

        primaryViewSelectionModel = AgisoftPrimaryViewSelectionModel.createInstance(chunkName, cameras, thumbnailImageList);

        addTreeElems(primaryViewSelectionModel);
    }

    private void addTreeElems(PrimaryViewSelectionModel primaryViewSelectionModel){
        TreeItem<String> rootItem = new TreeItem<>(primaryViewSelectionModel.getName());
        chunkTreeView.setRoot(rootItem);

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
        chunkTreeView.getRoot().setExpanded(true);

        chunkTreeView.getSelectionModel().select(1);
        selectImageInTreeView();
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
            updateImageText(imageName);
            imgViewText.setText(imgViewText.getText() + " (preview)");

            //set thumbnail as main image, then update to full resolution later
            //don't set thumbnail if img is cached, otherwise would cause a flash
            if(!imgCache.containsKey(imageName)){
                setThumbnailAsFullImage(selectedItem);
            }

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
        imgViewText.setText(imageName);
    }

    private void setThumbnailAsFullImage(TreeItem<String> selectedItem) {
        //use thumbnail as main image
        //used if image is not found, or if larger resolution image is being loaded
        chunkViewerImgView.setImage(selectedItem.getGraphic().
                snapshot(new SnapshotParameters(), null));
    }

    @FXML
    private void rotateRight() {
        //rotate in 90 degree increments
        chunkViewerImgView.setRotate((chunkViewerImgView.getRotate() + 90) % 360);
    }

    @FXML
    private void rotateLeft() {
        //rotate in 90 degree increments
        chunkViewerImgView.setRotate((chunkViewerImgView.getRotate() - 90) % 360);
    }

    private void showMissingImgsAlert(MetashapeObjectChunk metashapeObjectChunk, MissingImagesException mie) {
        int numMissingImgs = mie.getNumMissingImgs();
        File prevTriedDirectory = mie.getImgDirectory();

        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.OTHER);
        ButtonType newDirectory = new ButtonType("Choose Different Image Directory", ButtonBar.ButtonData.YES);
        ButtonType skipMissingCams = new ButtonType("Skip Missing Cameras", ButtonBar.ButtonData.NO);

        Alert alert = new Alert(Alert.AlertType.NONE,
                "Imported object is missing " + numMissingImgs + " images.",
                cancel, newDirectory, skipMissingCams/*, openDirectory*/);

        ((ButtonBase) alert.getDialogPane().lookupButton(cancel)).setOnAction(event -> {
            getHostScrollerController().prevPage();
        });

        ((ButtonBase) alert.getDialogPane().lookupButton(newDirectory)).setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(new File(metashapeObjectChunk.getPsxFilePath()).getParentFile());

            directoryChooser.setTitle("Choose New Image Directory");
            File newCamsFile = directoryChooser.showDialog(MenubarController.getInstance().getWindow());

            try {
                verifyInfo(newCamsFile);
                initTreeView(metashapeObjectChunk);
            } catch (MissingImagesException mie2){
                Platform.runLater(() ->
                        showMissingImgsAlert(metashapeObjectChunk, mie2));
            }
        });

        ((ButtonBase) alert.getDialogPane().lookupButton(skipMissingCams)).setOnAction(event -> {
            this.fullResOverride = prevTriedDirectory;
            doSkipMissingCams = true;
            initTreeView(metashapeObjectChunk);
        });

        alert.setTitle("Project is Missing Images");
        alert.show();
    }

    private void verifyInfo(File fullResDirectoryOverride) throws MissingImagesException {
        this.fullResOverride = fullResDirectoryOverride;

        // Get reference to the chunk directory
        File chunkDirectory = new File(metashapeObjectChunk.getChunkDirectoryPath());
        if (!chunkDirectory.exists()) {
            log.error("Chunk directory does not exist: " + chunkDirectory);
        }
        File rootDirectory = new File(metashapeObjectChunk.getPsxFilePath()).getParentFile();
        if (!rootDirectory.exists()) {
            log.error("Root directory does not exist: " + rootDirectory);
        }

        // 1) Construct camera ID to filename map from frame's ZIP
        Map<Integer, String> cameraPathsMap = new HashMap<>();
        // Open the xml files that contains all the cameras' ids and file paths
        Document frame = metashapeObjectChunk.getFrameZip();
        if (frame == null || frame.getDocumentElement() == null) {
            ProjectIO.handleException("Error reading Metashape frame.zip document.", new NullPointerException("No frame document found"));
            return;
        }

        // Loop through the cameras and store each pair of id and path in the map
        NodeList cameraList = ((Element) frame.getElementsByTagName("frame").item(0))
                .getElementsByTagName("camera");

        int numMissingFiles = 0;
        File fullResSearchDirectory;
        if (fullResDirectoryOverride == null) {
            fullResSearchDirectory = new File(metashapeObjectChunk.getFramePath()).getParentFile();
        } else {
            fullResSearchDirectory = fullResDirectoryOverride;
        }

        File exceptionFolder = null;

        for (int i = 0; i < cameraList.getLength(); i++) {

            Element cameraElement = (Element) cameraList.item(i);
            int cameraId = Integer.parseInt(cameraElement.getAttribute("camera_id"));

            String pathAttribute = ((Element) cameraElement.getElementsByTagName("photo").item(0)).getAttribute("path");

            File imageFile;
            String finalPath = "";
            if (fullResDirectoryOverride == null) {
                imageFile = new File(fullResSearchDirectory, pathAttribute);
                finalPath = rootDirectory.toPath().relativize(imageFile.toPath()).toString();
            } else {
                //if this doesn't work, then replace metashapeObjectChunk.getFramePath()).getParentFile()
                //    and the first part of path with the file that the user selected
                String pathAttributeName = new File(pathAttribute).getName();
                imageFile = new File(fullResDirectoryOverride, pathAttributeName);
                finalPath = imageFile.getName();
            }

            if (imageFile.exists() && !finalPath.isBlank()) {
                // Add pair to the map
                cameraPathsMap.put(cameraId, finalPath);
            } else {
                numMissingFiles++;

                if (exceptionFolder == null) {
                    exceptionFolder = imageFile.getParentFile();
                }
            }
        }

        if (numMissingFiles > 0) {
            throw new MissingImagesException("Project is missing images.", numMissingFiles, exceptionFolder);
        }
    }

    @Override
    public void confirmButtonPress() {
        updateSharedInfo();
        if (loadStartCallback != null) {
            loadStartCallback.run();
        }

        if (viewSetCallback != null) {
            //"force" the user to save their project (user can still cancel saving)
            MultithreadModels.getInstance().getIOModel().addViewSetLoadCallback(
                viewSet ->viewSetCallback.accept(viewSet));
        }

        boolean importFromMetashape =
            hostPage.getPrevPage() == hostScrollerController.getPage(
                "/fxml/menubar/createnewproject/MetashapeImport.fxml");

        String primaryView = chunkTreeView.getSelectionModel().getSelectedItem().getValue();
        if(importFromMetashape){
            new Thread(() ->
                MultithreadModels.getInstance().getIOModel()
                    .loadAgisoftFromZIP(
                        metashapeObjectChunk.getFramePath(),
                        metashapeObjectChunk, fullResOverride, doSkipMissingCams,
                        primaryView, chunkViewerImgView.getRotate()))
                .start();
        }
        else{
            new Thread(() ->
                MultithreadModels.getInstance().getIOModel().loadFromLooseFiles(
                    cameraFile.getPath(), cameraFile, objFile, photosDir, primaryView, chunkViewerImgView.getRotate()))
                .start();
        }

        WelcomeWindowController.getInstance().hide();

        Window window = hostAnchorPane.getScene().getWindow();
        window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void updateSharedInfo() {
        metashapeObjectChunk = hostScrollerController.getInfo(ShareInfo.Info.METASHAPE_OBJ_CHUNK);
        cameraFile = hostScrollerController.getInfo(ShareInfo.Info.CAM_FILE);
        objFile = hostScrollerController.getInfo(ShareInfo.Info.OBJ_FILE);
        photosDir = hostScrollerController.getInfo(ShareInfo.Info.PHOTO_DIR);

        isMetashapeImport = metashapeObjectChunk != null;
    }

    @Override
    public boolean isNextButtonValid(){
        return true;
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

    public Document getCameraDocument(){
        return cameraDocument;
    }

    public PrimaryViewSelectionModel getPrimaryViewSelectionModel() {
        return primaryViewSelectionModel;
    }

    public File getPhotosDir() {
        return photosDir;
    }

    public File getFullResOverride() {
        return fullResOverride;
    }

    public HashMap<String, Image> getImgCache() {
        return imgCache;
    }
}