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

import kintsugi3d.builder.io.imageset.ImageSetInfo;
import kintsugi3d.builder.javafx.controllers.modals.viewselect.ViewSelectableBase;

import java.io.File;
import java.util.Collection;

public abstract class ValidatedInputSourceBase extends ViewSelectableBase implements ValidatedInputSource
{
    private final InputSource original;
    private final ImageSetInfo imageSetInfo;

    protected ValidatedInputSourceBase(InputSource original, ImageSetInfo imageSetInfo)
    {
        this.original = original;
        this.imageSetInfo = imageSetInfo;
    }

    @Override
    public File getMasksDirectory()
    {
        return original.getMasksDirectory();
    }

    @Override
    public void setMasksDirectory(File file)
    {
        original.setMasksDirectory(file);
    }

    @Override
    public File getInitialMasksDirectory()
    {
        return original.getInitialMasksDirectory();
    }

    @Override
    public boolean hasProjectMasks()
    {
        return original.hasProjectMasks();
    }

    @Override
    public File specifyProjectFileToSave()
    {
        return original.specifyProjectFileToSave();
    }

    @Override
    public Collection<File> getDisabledImages()
    {
        return original.getDisabledImages();
    }

    @Override
    public ImageSetInfo getImageSetInfo()
    {
        return imageSetInfo;
    }

    @Override
    public boolean confirm()
    {
        File projectFileToSave = specifyProjectFileToSave();

        if (projectFileToSave != null)
        {
            confirm(projectFileToSave);
            return true;
        }
        else
        {
            return false;
        }
    }

    protected abstract void confirm(File projectFileToSave);
}
