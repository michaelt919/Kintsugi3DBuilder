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

import com.agisoft.metashape.Camera;
import com.agisoft.metashape.Chunk;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import kintsugi3d.builder.io.ViewSetReader;
import kintsugi3d.builder.io.ViewSetReaderFromAgisoftXML;
import kintsugi3d.builder.io.primaryview.AgisoftPrimaryViewSelectionModel;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.controllers.menubar.MenubarController;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.builder.resources.ibr.MissingImagesException;
import kintsugi3d.util.ImageFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MetashapeProjectInputSource extends InputSource{
    private static final Logger log = LoggerFactory.getLogger(MetashapeProjectInputSource.class);
    private MetashapeObjectChunk metashapeObjectChunk;
    private Chunk chunk;
    private File fullResOverride;

    @Override
    public List<FileChooser.ExtensionFilter> getExtensionFilters() {
        return Collections.singletonList(new FileChooser.ExtensionFilter("Agisoft Metashape XML file", "*.xml"));
    }

    @Override
    public ViewSetReader getCameraFileReader() {
        return ViewSetReaderFromAgisoftXML.getInstance();
    }

    public MetashapeProjectInputSource setChunk(Chunk chunk){
        this.chunk = chunk;
        return this;
    }
    @Override
    public void verifyInfo(File fullResDirectoryOverride){
        int numMissingFiles = 0;
        File exceptionFolder = null;


        if (fullResDirectoryOverride == null){
            for (String path : Arrays.stream(chunk.getCameras())
                    .map(camera -> camera.getPhoto().get()
                            .getPath())
                    .collect(Collectors.toList())){
                File photo = new File(path);
                try{
                    photo = ImageFinder.getInstance().findImageFile(photo);
                } catch (FileNotFoundException e) {
                    numMissingFiles++;
                    exceptionFolder = photo.getParentFile();
                }
            }
        }
        else{
            this.fullResOverride = fullResDirectoryOverride;
            for (String path : Arrays.stream(chunk.getCameras())
                    .map(Camera::getLabel)
                    .collect(Collectors.toList())){
                File photo = new File(fullResDirectoryOverride, path);
                try{
                    photo = ImageFinder.getInstance().findImageFile(photo);
                } catch (FileNotFoundException e) {
                    numMissingFiles++;
                    exceptionFolder = photo.getParentFile();
                }
            }
        }

        if (numMissingFiles > 0) {
            throw new MissingImagesException("Project is missing images.", numMissingFiles, exceptionFolder);
        }
    }

    @Override
    public void initTreeView() {
        //TODO: Build a wrapper to put fullResOverride and chunk in one object?
        primaryViewSelectionModel = AgisoftPrimaryViewSelectionModel.createInstance(chunk, fullResOverride);

        addTreeElems(primaryViewSelectionModel);
        searchableTreeView.bind();
    }


    //TODO: uncouple loadProject() from orientationView
    @Override
    public void loadProject(String orientationView, double rotate) {
        metashapeObjectChunk.getLoadPreferences().orientationViewName = orientationView;
        metashapeObjectChunk.getLoadPreferences().orientationViewRotateDegrees = rotate;
        new Thread(() -> MultithreadModels.getInstance().getIOModel().loadAgisoftFromZIP(metashapeObjectChunk)).start();
    }

    @Override
    public boolean equals(Object obj) {
        //TODO: metashape's api doesn't have an equals so we'll need to work around that
        return false;
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
            directoryChooser.setInitialDirectory(new File(chunk.getDocument().get().getPath()).getParentFile());

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
            //TODO: imp this
//            metashapeObjectChunk.getLoadPreferences().fullResOverride = prevTriedDirectory;
//            metashapeObjectChunk.getLoadPreferences().doSkipMissingCams = true;
//            initTreeView();
        });

        alert.setTitle("Project is Missing Images");
        alert.show();
    }
}
