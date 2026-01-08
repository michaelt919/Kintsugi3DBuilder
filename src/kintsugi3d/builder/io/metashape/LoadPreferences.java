/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.io.metashape;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class LoadPreferences
{
    private Collection<File> disabledImageFiles = new ArrayList<>(8);
    private File fullResOverride;
    private String orientationViewName;
    private double orientationViewRotateDegrees = 0;

    public Collection<File> getDisabledImageFiles()
    {
        return Collections.unmodifiableCollection(disabledImageFiles);
    }

    public void setDisabledImageFiles(Collection<File> disabledImageFiles)
    {
        this.disabledImageFiles = List.copyOf(disabledImageFiles);
    }

    public File getFullResOverride()
    {
        return fullResOverride;
    }

    public void setFullResOverride(File fullResOverride)
    {
        this.fullResOverride = fullResOverride;
    }

    public String getOrientationViewName()
    {
        return orientationViewName;
    }

    public void setOrientationViewName(String orientationViewName)
    {
        this.orientationViewName = orientationViewName;
    }

    public double getOrientationViewRotateDegrees()
    {
        return orientationViewRotateDegrees;
    }

    public void setOrientationViewRotateDegrees(double orientationViewRotateDegrees)
    {
        this.orientationViewRotateDegrees = orientationViewRotateDegrees;
    }
}
