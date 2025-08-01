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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.io.metashape.MetashapeChunk;
import kintsugi3d.builder.io.metashape.MetashapeModel;
import kintsugi3d.builder.io.primaryview.AgisoftPrimaryViewSelectionModel;
import kintsugi3d.builder.javafx.ProjectIO;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.builder.javafx.controllers.menubar.fxmlpageutils.FXMLPageScrollerController;
import kintsugi3d.builder.resources.ibr.MissingImagesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MetashapeProjectInputSource extends InputSource{
    private static final Logger log = LoggerFactory.getLogger(MetashapeProjectInputSource.class);
    private MetashapeModel model;

    @Override
    public List<FileChooser.ExtensionFilter> getExtensionFilters() {
        return Collections.singletonList(new FileChooser.ExtensionFilter("Agisoft Metashape XML file", "*.xml"));
    }

    @Override
    public void verifyInfo(){
        MetashapeChunk parentChunk = model.getChunk();

        // Open the xml files that contains all the cameras' ids and file paths
        Document frame = parentChunk.getFrameXML();
        if (frame == null || frame.getDocumentElement() == null) {
            ProjectIO.handleException("Error reading Metashape frame.zip document.", new NullPointerException("No frame document found"));
            return;
        }

        // Loop through the cameras and store each pair of id and path in the map
        NodeList cameraList = ((Element) frame
                .getElementsByTagName("frame").item(0)) //assuming frame 0
                .getElementsByTagName("camera");

        int numMissingFiles = 0;
        File exceptionFolder = null;

        for (int i = 0; i < cameraList.getLength(); i++) {
            Element cameraElement = (Element) cameraList.item(i);

            File imageFile = getImageFromFrameCam(cameraElement, parentChunk);

            if (!imageFile.exists()) {
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
    public File getMasksDirectory() {
       return model.getChunk().getMasksDirectory();
    }

    @Override
    public File getInitialMasksDirectory() {
        File masksDir = model.getChunk().getMasksDirectory();
        //TODO: might change this because it dumps the user deep into metashape project structure
        return masksDir != null ? masksDir.getParentFile() : model.getChunk().getPsxFile().getParentFile();
    }

    @Override
    public boolean doEnableProjectMasksButton() {
       return model.getChunk().getMasksDirectory() != null;
    }

    @Override
    public void setMasksDirectory(File file) {
       model.getChunk().setMasksDirectory(file);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MetashapeProjectInputSource)){
            return false;
        }

        MetashapeProjectInputSource other = (MetashapeProjectInputSource) obj;

        //model and mask directory must be the same
        if (!this.model.equals(other.model)){
            return false;
        }

        return Objects.equals(this.model.getChunk().getMasksDirectory(), other.model.getChunk().getMasksDirectory());
    }

    @Override
    public void initTreeView() {
        primaryViewSelectionModel = new AgisoftPrimaryViewSelectionModel(model);
        addTreeElems(primaryViewSelectionModel);
        searchableTreeView.bind();
    }

    //TODO: uncouple loadProject() from orientationView
    @Override
    public void loadProject(String orientationView, double rotate) {
        model.getLoadPreferences().orientationViewName = orientationView;
        model.getLoadPreferences().orientationViewRotateDegrees = rotate;
        new Thread(() -> Global.state().getIOModel().loadFromMetashapeModel(model)).start();
    }

    public MetashapeProjectInputSource setMetashapeModel(MetashapeModel model){
        this.model = model;
        return this;
    }

    public void showMissingImgsAlert(MissingImagesException mie, FXMLPageScrollerController hostScrollerController) {
        int numMissingImgs = mie.getNumMissingImgs();
        File prevTriedDirectory = mie.getImgDirectory();

        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.OTHER);
        ButtonType newDirectory = new ButtonType("Choose Different Image Directory", ButtonBar.ButtonData.YES);
        ButtonType skipMissingCams = new ButtonType("Skip Missing Cameras", ButtonBar.ButtonData.NO);

        Alert alert = new Alert(Alert.AlertType.NONE,
                "Imported object is missing " + numMissingImgs + " images.",
                cancel, newDirectory, skipMissingCams/*, openDirectory*/);

        ((ButtonBase) alert.getDialogPane().lookupButton(cancel)).setOnAction(event -> {
            hostScrollerController.prevPage();
        });

        ((ButtonBase) alert.getDialogPane().lookupButton(newDirectory)).setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(new File(model.getChunk().getParentDocument().getPsxFilePath()).getParentFile());

            directoryChooser.setTitle("Choose New Image Directory");

            model.getLoadPreferences().fullResOverride = directoryChooser.showDialog(MenubarController.getInstance().getWindow());
            try
            {
                verifyInfo();
                initTreeView();
            }
            catch(MissingImagesException mie2)
            {
                Platform.runLater(() -> showMissingImgsAlert(mie2, hostScrollerController));
            }
        });

        ((ButtonBase) alert.getDialogPane().lookupButton(skipMissingCams)).setOnAction(event -> {
            model.getLoadPreferences().fullResOverride = prevTriedDirectory;
            model.getLoadPreferences().doSkipMissingCams = true;
            initTreeView();
        });

        alert.setTitle("Project is Missing Images");
        alert.show();
    }

    private static File getImageFromFrameCam(Element cameraElement, MetashapeChunk chunk) {
        File fullResOverride = chunk.getSelectedModel().getLoadPreferences().fullResOverride;
        String pathAttribute = ((Element) cameraElement.getElementsByTagName("photo").item(0)).getAttribute("path");

        File imageFile;
        if (fullResOverride != null) {
            //user selected an override
            String pathAttributeName = new File(pathAttribute).getName();
            imageFile = new File(fullResOverride, pathAttributeName);
        } else {
            //no override
            File fullResDir = new File(chunk.getFramePath()).getParentFile();
            imageFile = new File(fullResDir, pathAttribute);
        }
        return imageFile;
    }
}
