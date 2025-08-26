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

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.io.ViewSetDirectories;
import kintsugi3d.builder.io.ViewSetLoadOptions;
import kintsugi3d.builder.io.ViewSetReaderFromRealityCaptureCSV;
import kintsugi3d.builder.io.primaryview.GenericViewSelectionModel;
import kintsugi3d.builder.io.primaryview.MetashapeViewSelectionModel;
import kintsugi3d.builder.io.primaryview.ViewSelectionModel;
import kintsugi3d.builder.javafx.controllers.modals.viewselect.ViewSelectable;

import java.io.File;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.function.Consumer;

public class ManualInputSource extends InputSourceBase
{
    private File cameraFile;
    private File meshFile;
    private File photosDir;
    private File masksDir;
    private boolean needsUndistort;
    private boolean hotSwap;

    public File getCameraFile()
    {
        return this.cameraFile;
    }

    public ManualInputSource setCameraFile(File cameraFile)
    {
        this.cameraFile = cameraFile;
        return this;
    }

    public File getMeshFile()
    {
        return this.meshFile;
    }

    public ManualInputSource setMeshFile(File meshFile)
    {
        this.meshFile = meshFile;
        return this;
    }

    public File getPhotosDir()
    {
        return this.photosDir;
    }

    public ManualInputSource setPhotosDir(File photosDir)
    {
        this.photosDir = photosDir;
        return this;
    }

    public ManualInputSource setNeedsUndistort(boolean needsUndistort)
    {
        this.needsUndistort = needsUndistort;
        return this;
    }

    public ManualInputSource setHotSwap(boolean hotSwap)
    {
        this.hotSwap = hotSwap;
        return this;
    }

    @Override
    protected void loadForViewSelectionOrThrow(Consumer<ViewSelectionModel> onLoadComplete) throws Exception
    {
        if (cameraFile.getName().endsWith(".xml")) // Agisoft Metashape
        {
            setViewSelectionModel(new MetashapeViewSelectionModel(cameraFile, photosDir));
            onLoadComplete.accept(getViewSelectionModel());
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

            setViewSelectionModel(new GenericViewSelectionModel(cameraFile.getName(), viewSet));
            onLoadComplete.accept(getViewSelectionModel());
        }
        else
        {
            throw new IllegalArgumentException(
                MessageFormat.format("File extension not recognized for {0}", cameraFile.getName()));
        }
    }

    @Override
    public void confirm()
    {
        ViewSetLoadOptions loadOptions = new ViewSetLoadOptions();
        loadOptions.mainDirectories.projectRoot = cameraFile.getParentFile();
        loadOptions.geometryFile = meshFile;
        loadOptions.masksDirectory = masksDir;
        loadOptions.mainDirectories.fullResImageDirectory = photosDir;
        loadOptions.mainDirectories.fullResImagesNeedUndistort = needsUndistort;
        loadOptions.orientationViewName = getViewSelection();
        loadOptions.orientationViewRotation = getViewRotation();

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
    public boolean needsRefresh(ViewSelectable oldInstance)
    {
        if (oldInstance instanceof ManualInputSource)
        {
            ManualInputSource other = (ManualInputSource) oldInstance;
            return !(Objects.equals(this.cameraFile, other.cameraFile) ||
                !Objects.equals(this.meshFile, other.meshFile) ||
                !Objects.equals(this.photosDir, other.photosDir) ||
                !Objects.equals(this.masksDir, other.masksDir));
        }
        else
        {
            return true;
        }
    }

    @Override
    public File getInitialPhotosDirectory()
    {
        return photosDir != null ? photosDir : cameraFile.getParentFile();
    }

    @Override
    public void overrideFullResImageDirectory(File directory)
    {
        setPhotosDir(directory);
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
    public boolean hasProjectMasks()
    {
        return false;
    }

    @Override
    public void setMasksDirectory(File file)
    {
        masksDir = file;
    }
}
