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

package kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources;

import javafx.stage.FileChooser.ExtensionFilter;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.io.ViewSetDirectories;
import kintsugi3d.builder.io.ViewSetLoadOptions;
import kintsugi3d.builder.io.ViewSetReaderFromRealityCaptureCSV;
import kintsugi3d.builder.io.primaryview.AgisoftPrimaryViewSelectionModel;
import kintsugi3d.builder.io.primaryview.GenericPrimaryViewSelectionModel;
import kintsugi3d.builder.javafx.util.ExceptionHandling;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

public class LooseFilesInputSource extends InputSource
{
    private File cameraFile;
    private File meshFile;
    private File photosDir;
    private File masksDir;
    private boolean needsUndistort;
    private boolean hotSwap;

    @Override
    public List<ExtensionFilter> getExtensionFilters()
    {
        return List.of(new ExtensionFilter("Agisoft Metashape XML file", "*.xml"),
            new ExtensionFilter("Reality Capture CSV file", "*.csv"));
    }

    public LooseFilesInputSource setCameraFile(File cameraFile)
    {
        this.cameraFile = cameraFile;
        return this;
    }

    public LooseFilesInputSource setMeshFile(File meshFile)
    {
        this.meshFile = meshFile;
        return this;
    }

    public LooseFilesInputSource setPhotosDir(File photosDir, boolean needsUndistort)
    {
        this.photosDir = photosDir;
        this.needsUndistort = needsUndistort;
        return this;
    }

    public LooseFilesInputSource setMasksDir(File masksDir)
    {
        this.masksDir = masksDir;
        return this;
    }

    public LooseFilesInputSource setHotSwap(boolean hotSwap)
    {
        this.hotSwap = hotSwap;
        return this;
    }

    @Override
    public void initTreeView()
    {
        try
        {
            if (cameraFile.getName().endsWith(".xml")) // Agisoft Metashape
            {
                primaryViewSelectionModel = new AgisoftPrimaryViewSelectionModel(cameraFile, photosDir);
            }
            else if (cameraFile.getName().endsWith(".csv")) // RealityCapture
            {
                ViewSetDirectories directories = new ViewSetDirectories();
                directories.projectRoot = cameraFile.getParentFile();
                directories.fullResImageDirectory = photosDir;
                directories.fullResImagesNeedUndistort = needsUndistort;

                ViewSet viewSet = ViewSetReaderFromRealityCaptureCSV.getInstance()
                    .readFromFile(cameraFile, directories)
                    .setGeometryFile(meshFile)
                    .setMasksDirectory(masksDir)
                    .finish();

                primaryViewSelectionModel = new GenericPrimaryViewSelectionModel(cameraFile.getName(), viewSet);
            }
            else
            {
                ExceptionHandling.error("Error initializing primary view selection.", new IllegalArgumentException(MessageFormat.format("File extension not recognized for {0}", cameraFile.getName())));
                return;
            }

            addTreeElems(primaryViewSelectionModel);
            searchableTreeView.bind();
        }
        catch (Exception e)
        {
            ExceptionHandling.error("Error initializing primary view selection.", e);
        }
    }

    @Override
    public void loadProject()
    {
        ViewSetLoadOptions loadOptions = new ViewSetLoadOptions();
        loadOptions.mainDirectories.projectRoot = cameraFile.getParentFile();
        loadOptions.geometryFile = meshFile;
        loadOptions.masksDirectory = masksDir;
        loadOptions.mainDirectories.fullResImageDirectory = photosDir;
        loadOptions.mainDirectories.fullResImagesNeedUndistort = needsUndistort;
        loadOptions.orientationViewName = getPrimaryView();
        loadOptions.orientationViewRotation = getPrimaryViewRotation();

        if (hotSwap)
        {
            new Thread(() ->
                Global.state().getIOModel().hotSwapLooseFiles(cameraFile.getPath(), cameraFile, loadOptions))
                .start();
        }
        else
        {
            new Thread(() ->
                Global.state().getIOModel().loadFromLooseFiles(cameraFile.getPath(), cameraFile, loadOptions))
                .start();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof LooseFilesInputSource))
        {
            return false;
        }

        LooseFilesInputSource other = (LooseFilesInputSource) obj;

        return this.cameraFile.equals(other.cameraFile) &&
            this.meshFile.equals(other.meshFile) &&
            this.photosDir.equals(other.photosDir) &&
            this.masksDir.equals(other.masksDir);
    }

    @Override
    public File getMasksDirectory()
    {
        return masksDir;
    }

    @Override
    public File getInitialMasksDirectory()
    {
        return masksDir != null ? masksDir : cameraFile.getParentFile();
    }

    @Override
    public boolean doEnableProjectMasksButton()
    {
        return false;
    }

    @Override
    public void setMasksDirectory(File file)
    {
        masksDir = file;
    }
}
