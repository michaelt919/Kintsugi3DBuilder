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

import kintsugi3d.builder.javafx.controllers.modals.viewselect.ViewSelectable;

import java.io.File;
import java.util.Collection;

public interface InputSource extends ViewSelectable
{
    /**
     * Return the known masks directory.
     *
     * @return the masks directory
     */
    File getMasksDirectory();

    void setMasksDirectory(File file);

    /**
     * For setting the initial directory of the masks directory chooser. Not guaranteed to be the actual masks directory.
     *
     * @return a directory which will hopefully be close to the actual masks directory so the user will find it quickly
     */
    File getInitialMasksDirectory();

    boolean hasProjectMasks();

    File getInitialPhotosDirectory();

    void overrideFullResImageDirectory(File directory);

    Collection<File> getDisabledImages();
}
