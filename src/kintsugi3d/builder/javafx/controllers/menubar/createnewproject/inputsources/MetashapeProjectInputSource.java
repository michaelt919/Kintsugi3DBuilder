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

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import kintsugi3d.builder.io.ViewSetReader;
import kintsugi3d.builder.io.ViewSetReaderFromAgisoftXML;
import kintsugi3d.builder.io.primaryview.AgisoftPrimaryViewSelectionModel;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.ProjectIO;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.builder.javafx.controllers.menubar.metashape.MetashapeChunk;
import kintsugi3d.builder.resources.ibr.MissingImagesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class MetashapeProjectInputSource extends InputSource{
    private static final Logger log = LoggerFactory.getLogger(MetashapeProjectInputSource.class);
    private MetashapeChunk metashapeChunk;
    @Override
    public List<FileChooser.ExtensionFilter> getExtensionFilters() {
        return Collections.singletonList(new FileChooser.ExtensionFilter("Agisoft Metashape XML file", "*.xml"));
    }

    @Override
    public ViewSetReader getCameraFileReader() {
        return ViewSetReaderFromAgisoftXML.getInstance();
    }
    public MetashapeProjectInputSource setMetashapeObjectChunk(MetashapeChunk moc){
        this.metashapeChunk = moc;
        return this;
    }
    @Override
    public void verifyInfo(File fullResDirectoryOverride){
        metashapeChunk.getLoadPreferences().fullResOverride = fullResDirectoryOverride;

        // Get reference to the chunk directory
        File chunkDirectory = new File(metashapeChunk.getChunkDirectoryPath());
        if (!chunkDirectory.exists()) {
            log.error("Chunk directory does not exist: " + chunkDirectory);
        }
        File rootDirectory = new File(metashapeChunk.getMetashapeObject().getPsxFilePath()).getParentFile();
        if (!rootDirectory.exists()) {
            log.error("Root directory does not exist: " + rootDirectory);
        }

        // Open the xml files that contains all the cameras' ids and file paths
        Document frame = metashapeChunk.getFrameXML();
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
            fullResSearchDirectory = new File(metashapeChunk.getFramePath()).getParentFile();
        } else {
            fullResSearchDirectory = fullResDirectoryOverride;
        }

        File exceptionFolder = null;

        for (int i = 0; i < cameraList.getLength(); i++) {

            Element cameraElement = (Element) cameraList.item(i);

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

            if (!imageFile.exists() || finalPath.isBlank()) {
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
    public void initTreeView() {
        String chunkName = metashapeChunk.getLabel();

        List <Image> thumbnailImageList = metashapeChunk.loadThumbnailImageList();
        List<Element> cameras = metashapeChunk.findEnabledCameras();

        File fullResOverride = metashapeChunk.getLoadPreferences().fullResOverride;
        File fullResDir = fullResOverride != null ? fullResOverride : metashapeChunk.findFullResImgDirectory();
        primaryViewSelectionModel = AgisoftPrimaryViewSelectionModel.createInstance(chunkName, cameras, thumbnailImageList, fullResDir);

        addTreeElems(primaryViewSelectionModel);
        searchableTreeView.bind();
    }

    //TODO: uncouple loadProject() from orientationView
    @Override
    public void loadProject(String orientationView, double rotate) {
        metashapeChunk.getLoadPreferences().orientationViewName = orientationView;
        metashapeChunk.getLoadPreferences().orientationViewRotateDegrees = rotate;
        new Thread(() -> MultithreadModels.getInstance().getIOModel().loadAgisoftFromZIP(metashapeChunk)).start();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MetashapeProjectInputSource)){
            return false;
        }

        MetashapeProjectInputSource other = (MetashapeProjectInputSource) obj;

        return this.metashapeChunk.equals(other.metashapeChunk);
    }

    public void showMissingImgsAlert(MissingImagesException mie) {
        int numMissingImgs = mie.getNumMissingImgs();
        File prevTriedDirectory = mie.getImgDirectory();

        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.OTHER);
        ButtonType newDirectory = new ButtonType("Choose Different Image Directory", ButtonBar.ButtonData.YES);
        ButtonType skipMissingCams = new ButtonType("Skip Missing Cameras", ButtonBar.ButtonData.NO);

        Alert alert = new Alert(Alert.AlertType.NONE,
                "Imported object is missing " + numMissingImgs + " images.",
                cancel, newDirectory, skipMissingCams/*, openDirectory*/);

        ((ButtonBase) alert.getDialogPane().lookupButton(cancel)).setOnAction(event -> {
            //TODO: get this later
            //getHostScrollerController().prevPage();
        });

        ((ButtonBase) alert.getDialogPane().lookupButton(newDirectory)).setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(new File(metashapeChunk.getMetashapeObject().getPsxFilePath()).getParentFile());

            directoryChooser.setTitle("Choose New Image Directory");
            File newCamsFile = directoryChooser.showDialog(MenubarController.getInstance().getWindow());

            try
            {
                verifyInfo(newCamsFile);
                initTreeView();
            }
            catch(MissingImagesException mie2)
            {
                    Platform.runLater(() -> showMissingImgsAlert(mie2));
            }
        });

        ((ButtonBase) alert.getDialogPane().lookupButton(skipMissingCams)).setOnAction(event -> {
            metashapeChunk.getLoadPreferences().fullResOverride = prevTriedDirectory;
            metashapeChunk.getLoadPreferences().doSkipMissingCams = true;
            initTreeView();
        });

        alert.setTitle("Project is Missing Images");
        alert.show();
    }
}
