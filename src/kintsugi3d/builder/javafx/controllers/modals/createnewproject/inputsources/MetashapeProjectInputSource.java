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
import kintsugi3d.builder.io.metashape.MetashapeModel;
import kintsugi3d.builder.io.primaryview.MetashapeViewSelectionModel;
import kintsugi3d.builder.io.primaryview.ViewSelectionModel;
import kintsugi3d.builder.javafx.controllers.modals.viewselect.ViewSelectable;
import kintsugi3d.builder.resources.project.MissingImagesException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.function.Consumer;

public class MetashapeProjectInputSource extends InputSourceBase
{
    private MetashapeModel model;

    @Override
    public File getMasksDirectory()
    {
        return model.getChunk().getMasksDirectory();
    }

    @Override
    public File getInitialMasksDirectory()
    {
        File masksDir = model.getChunk().getMasksDirectory();
        //TODO: might change this because it dumps the user deep into metashape project structure
        return masksDir != null ? masksDir.getParentFile() : model.getChunk().getPsxFile().getParentFile();
    }

    @Override
    public boolean hasProjectMasks()
    {
        return model.getChunk().getMasksDirectory() != null;
    }

    @Override
    public void setMasksDirectory(File file)
    {
        model.getChunk().setMasksDirectoryOverride(file);
    }

    @Override
    public boolean needsRefresh(ViewSelectable oldInstance)
    {
        if (oldInstance instanceof MetashapeProjectInputSource)
        {
            MetashapeProjectInputSource other = (MetashapeProjectInputSource) oldInstance;

            // model and mask directory must be the same to not need refresh
            return !Objects.equals(this.model, other.model)
                || !Objects.equals(this.model.getChunk().getMasksDirectory(), other.model.getChunk().getMasksDirectory());
        }
        else
        {
            return true;
        }
    }

    @Override
    protected void loadForViewSelectionOrThrow(Consumer<ViewSelectionModel> onLoadComplete)
        throws FileNotFoundException, MissingImagesException
    {
        setViewSelectionModel(new MetashapeViewSelectionModel(model));
        onLoadComplete.accept(getViewSelectionModel());
    }

    //TODO: uncouple loadProject() from orientationView
    @Override
    public void confirm()
    {
        model.getLoadPreferences().orientationViewName = getViewSelection();
        model.getLoadPreferences().orientationViewRotateDegrees = getViewRotation();
        new Thread(() -> Global.state().getIOModel().loadFromMetashapeModel(model)).start();
    }

    public MetashapeProjectInputSource setMetashapeModel(MetashapeModel model)
    {
        this.model = model;
        return this;
    }

    @Override
    public File getInitialPhotosDirectory()
    {
        return new File(model.getChunk().getParentDocument().getPsxFilePath()).getParentFile();
    }

    @Override
    public void overrideFullResImageDirectory(File directory)
    {
        model.getLoadPreferences().fullResOverride = directory;
    }
}
