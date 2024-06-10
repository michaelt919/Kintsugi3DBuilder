/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.rendering.components.snap;

import kintsugi3d.builder.core.Projection;
import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.gl.vecmath.Matrix4;

public interface ViewSelection
{
    ReadonlyViewSet getViewSet();

    int getSelectedViewIndex();

    /**
     * Gets the view matrix for a particular view index, relative to the world space used by rendered components.
     * This is not generally the same as the camera pose matrix in the view set as it is in reference to a
     * recentered, reoriented, and rescaled model.
     * @param index
     * @return
     */
    Matrix4 getViewForIndex(int index);

    default Matrix4 getSelectedView()
    {
        return getViewForIndex(getSelectedViewIndex());
    }

    default Matrix4 getSelectedCameraPose()
    {
        return getViewSet().getCameraPose(getSelectedViewIndex());
    }

    default Matrix4 getSelectedCameraPoseInverse()
    {
        return getViewSet().getCameraPoseInverse(getSelectedViewIndex());
    }

    default Projection getSelectedCameraProjection()
    {
        ReadonlyViewSet viewSet = getViewSet();
        return viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(getSelectedViewIndex()));
    }
}
