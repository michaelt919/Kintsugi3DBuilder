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
import kintsugi3d.builder.io.imageset.MetashapeImageSetInfo;
import kintsugi3d.builder.io.metashape.MetashapeModel;

import java.io.File;
import java.util.function.Supplier;

public class MetashapeProjectInputSource extends NonValidatedInputSourceBase
{
    private MetashapeModel model;

    public MetashapeProjectInputSource(Supplier<File> specifyProjectFileToSave)
    {
        super(specifyProjectFileToSave);
    }

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
        return (masksDir != null ? masksDir : model.getChunk().getPsxFile()).getParentFile();
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
    public MetashapeProjectInputSource overrideFullResImageDirectory(File directory)
    {
        model.getLoadPreferences().setFullResOverride(directory);
        return this;
    }

    @Override
    public ValidatedInputSource validate() throws Exception
    {
        return new ValidatedInputSourceBase(this, new MetashapeImageSetInfo(model, getDisabledImages()))
        {
            @Override
            public void confirm(File projectFileToSave)
            {
                model.getLoadPreferences().setOrientationViewName(getViewSelection());
                model.getLoadPreferences().setOrientationViewRotateDegrees(getViewRotation());
                model.getLoadPreferences().setDisabledImageFiles(getDisabledImages());
                Global.state().getIOModel().loadFromMetashapeModel(projectFileToSave, model);
            }
        };
    }
}
