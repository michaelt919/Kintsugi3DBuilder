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

import javafx.stage.FileChooser;
import kintsugi3d.builder.io.ViewSetDirectories;
import kintsugi3d.builder.io.ViewSetLoadOptions;
import kintsugi3d.builder.io.ViewSetReaderFromRealityCaptureCSV;
import kintsugi3d.builder.io.primaryview.GenericPrimaryViewSelectionModel;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.ProjectIO;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class RealityCaptureInputSource extends InputSource{
    private File cameraFile;
    private File meshFile;
    private File photosDir;
    private File masksDir;
    private boolean needsUndistort;

    @Override
    public List<FileChooser.ExtensionFilter> getExtensionFilters() {
        return Collections.singletonList(new FileChooser.ExtensionFilter("Reality Capture CSV file", "*.csv"));
    }

    @Override
    public void initTreeView() {
        try {
            ViewSetDirectories directories = new ViewSetDirectories();
            directories.projectRoot = cameraFile.getParentFile();
            directories.fullResImageDirectory = photosDir;
            directories.fullResImagesNeedUndistort = true;
            primaryViewSelectionModel = new GenericPrimaryViewSelectionModel(cameraFile.getName(),
                ViewSetReaderFromRealityCaptureCSV.getInstance().readFromFile(cameraFile, directories)
                    .setGeometryFile(meshFile)
                    .setMasksDirectory(masksDir)
                    .finish());

            addTreeElems(primaryViewSelectionModel);
            searchableTreeView.bind();
        } catch (Exception e) {
            ProjectIO.handleException("Error initializing primary view selection.", e);
        }
    }

    @Override
    public void loadProject(String primaryView, double rotate) {
        ViewSetLoadOptions loadOptions = new ViewSetLoadOptions();
        loadOptions.mainDirectories.projectRoot = cameraFile.getParentFile();
        loadOptions.geometryFile = meshFile;
        loadOptions.masksDirectory = masksDir;
        loadOptions.mainDirectories.fullResImageDirectory = photosDir;
        loadOptions.mainDirectories.fullResImagesNeedUndistort = needsUndistort;
        loadOptions.orientationViewName = primaryView;
        loadOptions.orientationViewRotation = rotate;
        new Thread(() ->
            MultithreadModels.getInstance().getIOModel().loadFromLooseFiles(cameraFile.getPath(), cameraFile,loadOptions))
            .start();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RealityCaptureInputSource)){
            return false;
        }

        RealityCaptureInputSource other = (RealityCaptureInputSource) obj;

        return this.cameraFile.equals(other.cameraFile) &&
                this.meshFile.equals(other.meshFile) &&
                this.photosDir.equals(other.photosDir) &&
                this.masksDir.equals(other.masksDir);
    }

    @Override
    public File getMasksDirectory() {
        return masksDir;
    }

    @Override
    public File getInitialMasksDirectory() {
       return masksDir!= null ? masksDir : cameraFile.getParentFile();
    }

    @Override
    public boolean doEnableProjectMasksButton() {
        return false;
    }

    @Override
    public void setMasksDirectory(File file) {
        masksDir = file;
    }

    public RealityCaptureInputSource setCameraFile(File cameraFile) {
        this.cameraFile = cameraFile;
        return this;
    }

    public RealityCaptureInputSource setMeshFile(File meshFile) {
        this.meshFile = meshFile;
        return this;
    }

    public RealityCaptureInputSource setPhotosDir(File photosDir, boolean needsUndistort) {
        this.photosDir = photosDir;
        this.needsUndistort = needsUndistort;
        return this;
    }
}
