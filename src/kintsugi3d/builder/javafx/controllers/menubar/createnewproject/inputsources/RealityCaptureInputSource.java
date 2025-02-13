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
import kintsugi3d.builder.io.ViewSetReader;
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

    @Override
    public List<FileChooser.ExtensionFilter> getExtensionFilters() {
        return Collections.singletonList(new FileChooser.ExtensionFilter("Reality Capture CSV file", "*.csv"));
    }

    @Override
    public ViewSetReader getCameraFileReader() {
        return ViewSetReaderFromRealityCaptureCSV.getInstance();
    }

    @Override
    public void initTreeView() {
        try {
            primaryViewSelectionModel = GenericPrimaryViewSelectionModel.createInstance(cameraFile.getName(),
                    ViewSetReaderFromRealityCaptureCSV.getInstance().readFromFile(cameraFile, meshFile, photosDir));

            addTreeElems(primaryViewSelectionModel);
            searchableTreeView.bind();
        } catch (Exception e) {
            ProjectIO.handleException("Error initializing primary view selection.", e);
        }
    }

    @Override
    public void loadProject(String primaryView, double rotate) {
        new Thread(() ->
                MultithreadModels.getInstance().getIOModel().loadFromLooseFiles(
                        cameraFile.getPath(), cameraFile, meshFile, photosDir, primaryView, rotate))
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
                this.photosDir.equals(other.photosDir);
    }

    public RealityCaptureInputSource setCameraFile(File cameraFile) {
        this.cameraFile = cameraFile;
        return this;
    }

    public RealityCaptureInputSource setMeshFile(File meshFile) {
        this.meshFile = meshFile;
        return this;
    }

    public RealityCaptureInputSource setPhotosDir(File photosDir) {
        this.photosDir = photosDir;
        return this;
    }
}
