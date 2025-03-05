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
import kintsugi3d.builder.io.primaryview.AgisoftPrimaryViewSelectionModel;
import kintsugi3d.builder.io.primaryview.GenericPrimaryViewSelectionModel;
import kintsugi3d.builder.javafx.MultithreadModels;
import kintsugi3d.builder.javafx.ProjectIO;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LooseFilesInputSource extends InputSource{
    private File cameraFile;
    private File meshFile;
    private File photosDir;
    private boolean needsUndistort;
    private boolean hotSwap;

    @Override
    public List<FileChooser.ExtensionFilter> getExtensionFilters() {
        List<FileChooser.ExtensionFilter> list = new ArrayList<>();
        list.add(new FileChooser.ExtensionFilter("Agisoft Metashape XML file", "*.xml"));
        list.add(new FileChooser.ExtensionFilter("Reality Capture CSV file", "*.csv"));
        return list;
    }

    @Override
    public ViewSetReader getCameraFileReader() {
        return null;
    }

    public LooseFilesInputSource setCameraFile(File camFile){
        this.cameraFile = camFile;
        return this;
    }

    public LooseFilesInputSource setMeshFile(File meshFile){
        this.meshFile = meshFile;
        return this;
    }

    public LooseFilesInputSource setPhotosDir(File photosDir, boolean needsUndistort){
        this.photosDir = photosDir;
        this.needsUndistort = needsUndistort;
        return this;
    }

    public void setHotSwap(boolean hotSwap)
    {
        this.hotSwap = hotSwap;
    }

    @Override
    public void initTreeView() {
        try {
            if (cameraFile.getName().endsWith(".xml")) // Agisoft Metashape
            {
                primaryViewSelectionModel = AgisoftPrimaryViewSelectionModel.createInstance(cameraFile, photosDir);
            }
            else if (cameraFile.getName().endsWith(".csv")) // RealityCapture
            {
                primaryViewSelectionModel = GenericPrimaryViewSelectionModel.createInstance(cameraFile.getName(),
                        ViewSetReaderFromRealityCaptureCSV.getInstance().readFromFile(cameraFile, meshFile, photosDir, true));
            }
            else
            {
                ProjectIO.handleException("Error initializing primary view selection.",
                        new IllegalArgumentException(MessageFormat.format("File extension not recognized for {0}", cameraFile.getName())));
                return;
            }

            addTreeElems(primaryViewSelectionModel);
            searchableTreeView.bind();

        } catch (Exception e) {
            ProjectIO.handleException("Error initializing primary view selection.", e);
        }
    }

    @Override
    public void loadProject(String primaryView, double rotate) {
        new Thread(() ->
                MultithreadModels.getInstance().getIOModel().hotSwapLooseFiles(
                        cameraFile.getPath(), cameraFile, meshFile, photosDir, needsUndistort, primaryView, rotate))
                .start();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LooseFilesInputSource)){
            return false;
        }

        LooseFilesInputSource other = (LooseFilesInputSource) obj;

        return this.cameraFile.equals(other.cameraFile) &&
                this.meshFile.equals(other.meshFile) &&
                this.photosDir.equals(other.photosDir);
    }
}
