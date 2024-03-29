/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.rendering.components.snap;

import kintsugi3d.builder.core.CameraViewport;
import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.core.RenderedComponent;
import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector4;

/**
 * Snaps to viewpoints from the view set
 * @param <ContextType>
 */
public class ViewSnap<ContextType extends Context<ContextType>> implements RenderedComponent<ContextType>
{
    private final SceneModel sceneModel;
    private final ReadonlyViewSet viewSet;

    private ViewSnapContent<ContextType> contentRoot;

    private Matrix4 viewSnap = Matrix4.IDENTITY;

    public ViewSnap(SceneModel sceneModel, ReadonlyViewSet viewSet)
    {
        this.sceneModel = sceneModel;
        this.viewSet = viewSet;
    }

    public ViewSnapContent<ContextType> getContentRoot()
    {
        return contentRoot;
    }

    /**
     * Transfers ownership of the lit content to this object
     * Must be called before initialize
     * @param contentRoot
     */
    public void takeContentRoot(ViewSnapContent<ContextType> contentRoot)
    {
        this.contentRoot = contentRoot;
    }

    @Override
    public void initialize()
    {
        contentRoot.initialize();
    }

    @Override
    public void reloadShaders()
    {
        contentRoot.reloadShaders();
    }

    private Matrix4 snapToView(Matrix4 targetView)
    {
        Matrix4 viewInverse = targetView.quickInverse(0.01f);
        float maxSimilarity = Float.NEGATIVE_INFINITY;
        int snapViewIndex = -1;

        // View will be overridden for light calibration so that it snaps to specific views
        Matrix4 currentViewSnap = null;

        for(int i = 0; i < this.viewSet.getCameraPoseCount(); i++)
        {
            Matrix4 candidatePose = this.viewSet.getCameraPose(i);
            Matrix4 candidateView = candidatePose.times(sceneModel.getFullModelMatrix().quickInverse(0.01f));
            float similarity = viewInverse.times(Vector4.ORIGIN).getXYZ()
                    .dot(candidateView.quickInverse(0.01f).times(Vector4.ORIGIN).getXYZ());

            if (similarity > maxSimilarity)
            {
                maxSimilarity = similarity;
                currentViewSnap = candidateView;
                snapViewIndex = i;
            }
        }

        assert currentViewSnap != null; // Should be non-null if there are any camera poses since initially maxSimilarity is -infinity

        contentRoot.setSnapView(snapViewIndex, currentViewSnap);
        return currentViewSnap;
    }

    @Override
    public void update()
    {
        viewSnap = snapToView(sceneModel.getCurrentViewMatrix());
        contentRoot.update();
    }

    @Override
    public void draw(FramebufferObject<ContextType> framebuffer, CameraViewport cameraViewport)
    {
        contentRoot.draw(framebuffer, cameraViewport.copyForView(viewSnap));
    }

    @Override
    public void drawInSubdivisions(FramebufferObject<ContextType> framebuffer, int subdivWidth, int subdivHeight, CameraViewport cameraViewport)
    {
        contentRoot.drawInSubdivisions(framebuffer, subdivWidth, subdivHeight, cameraViewport.copyForView(viewSnap));
    }

    @Override
    public void close() throws Exception
    {
        contentRoot.close();
    }
}
