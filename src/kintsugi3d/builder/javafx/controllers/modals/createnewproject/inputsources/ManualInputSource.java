/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
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
import kintsugi3d.builder.core.viewset.ViewSet;
import kintsugi3d.builder.io.ViewSetDirectories;
import kintsugi3d.builder.io.ViewSetLoadOptions;
import kintsugi3d.builder.io.ViewSetReaderFromRealityCaptureCSV;
import kintsugi3d.builder.io.imageset.GenericImageSetInfo;
import kintsugi3d.builder.io.imageset.ImageSetInfo;
import kintsugi3d.builder.io.imageset.MetashapeImageSetInfo;

import java.io.File;
import java.text.MessageFormat;
import java.util.function.Supplier;

public class ManualInputSource extends NonValidatedInputSourceBase
{
    private File cameraFile;
    private File meshFile;
    private File photosDir;
    private File masksDir;
    private boolean needsUndistort;

    private boolean hotSwap;

    public ManualInputSource(Supplier<File> specifyProjectFileToSave)
    {
        super(specifyProjectFileToSave);
    }

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

    @Override
    public ManualInputSource overrideFullResImageDirectory(File directory)
    {
        this.photosDir = directory;
        return this;
    }

    @Override
    public ValidatedInputSource validate() throws Exception
    {
        return new ValidatedInputSourceBase(this, loadImageSetInfo())
        {
            @Override
            public boolean confirm()
            {
                if (hotSwap)
                {
                    Global.state().getIOModel().hotSwapLooseFiles(cameraFile.getPath(), cameraFile, getViewSetLoadOptions());
                    return true;
                }
                else
                {
                    return super.confirm();
                }
            }

            @Override
            public void confirm(File projectFileToSave)
            {
                Global.state().getIOModel().loadFromLooseFiles(projectFileToSave, cameraFile.getPath(), cameraFile, getViewSetLoadOptions());
            }

            private ViewSetLoadOptions getViewSetLoadOptions()
            {
                ViewSetLoadOptions loadOptions = new ViewSetLoadOptions();
                loadOptions.mainDirectories.projectRoot = cameraFile.getParentFile();
                loadOptions.geometryFile = meshFile;
                loadOptions.masksDirectory = masksDir;
                loadOptions.mainDirectories.fullResImageDirectory = photosDir;
                loadOptions.mainDirectories.fullResImagesNeedUndistort = needsUndistort;
                loadOptions.orientationViewName = getViewSelection();
                loadOptions.orientationViewRotation = getViewRotation();
                loadOptions.disabledImages = getDisabledImages();
                return loadOptions;
            }
        };
    }

    private ImageSetInfo loadImageSetInfo() throws Exception
    {
        if (cameraFile.getName().endsWith(".xml")) // Agisoft Metashape
        {
            return new MetashapeImageSetInfo(cameraFile, photosDir, getDisabledImages());
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

            return new GenericImageSetInfo(cameraFile.getName(), viewSet);
        }
        else
        {
            throw new IllegalArgumentException(
                MessageFormat.format("File extension not recognized for {0}", cameraFile.getName()));
        }
    }

    public ManualInputSource setNeedsUndistort(boolean needsUndistort)
    {
        this.needsUndistort = needsUndistort;
        return this;
    }

    public boolean shouldHotSwap()
    {
        return hotSwap;
    }

    public ManualInputSource setHotSwap(boolean hotSwap)
    {
        this.hotSwap = hotSwap;
        return this;
    }

    @Override
    public File getInitialPhotosDirectory()
    {
        return photosDir != null ? photosDir : cameraFile.getParentFile();
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
