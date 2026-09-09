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

package kintsugi3d.builder.rendering.components.snap;

import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.core.viewset.ReadonlyViewSet;
import kintsugi3d.builder.core.viewset.View;
import kintsugi3d.builder.core.viewset.ViewSet;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

public class ViewSelectionImpl implements ViewSelection
{
    private static final float FRUSTUM_VISUALIZATION_SCALE = 0.1f;

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
    public View getSelectedView()
    {
        return sceneModel.getCameraViewListModel().getSelectedCameraView();
    }

    @Override
    public Matrix4 getMatrixFromView(View view)
    {
        if (view == null)
        {
            return Matrix4.IDENTITY;
        }
        else
        {
            Matrix4 pose = view.getCameraPose();
            return pose.times(sceneModel.getFullModelMatrix().quickInverse(0.01f));
        }
    }

    @Override
    public Vector3 getFrustumDimensions()
    {
        View selectedView = getSelectedView();

        if (selectedView != null)
        {
            return selectedView.getCameraProjection().getNormalizedFrustumDimensions().times(FRUSTUM_VISUALIZATION_SCALE);
        }
        else
        {
            return new Vector3(1.0f);
        }
    }
}
