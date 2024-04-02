/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.rendering.components.snap;

import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.gl.vecmath.Matrix4;

public class ViewSelectionImpl implements ViewSelection
{
    private final ViewSet viewSet;
    private final SceneModel sceneModel;

    public ViewSelectionImpl(ViewSet viewSet, SceneModel sceneModel)
    {
        this.viewSet = viewSet;
        this.sceneModel = sceneModel;
    }

    @Override
    public ReadonlyViewSet getViewSet()
    {
        return viewSet;
    }

    @Override
    public int getSelectedViewIndex()
    {
        return sceneModel.getCameraViewListModel().getSelectedCameraViewIndex();
    }

    @Override
    public Matrix4 getViewForIndex(int index)
    {
        Matrix4 pose = this.viewSet.getCameraPose(index);
        return pose.times(sceneModel.getFullModelMatrix().quickInverse(0.01f));
    }
}
