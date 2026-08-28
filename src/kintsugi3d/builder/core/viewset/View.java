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

package kintsugi3d.builder.core.viewset;

import kintsugi3d.builder.core.metrics.ReadonlyViewRMSE;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.util.ImageFinder;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Container for different data used by ViewSet. Contains cameraPose, cameraPoseInv, cameraProjectionIndex
 * lightIndex, imageFile, maskFile, and viewErrorMetrics.
 */
public class View
{
    /**
     * A camera pose defining the transformation from object space to camera space for each view.
     * These are necessary to perform projective texture mapping.
     */
    final Matrix4 cameraPose;

    /**
     * An inverted camera pose defining the transformation from camera space to object space for each view.
     * (Useful for visualizing the cameras on screen).
     */
    final Matrix4 cameraPoseInv;

    /**
     * An entry which designates the index of the projection transformation that should be used for each view.
     */
    final int cameraProjectionIndex;

    /**
     * An entry which designates the index of the light source position and intensity that should be used for each view.
     */
    final int lightIndex;

    /**
     * The relative path of the image file corresponding to this view.
     * The file paths are relative to the fullResImageDirectory
     */
    final File imageFile;

    int gpuViewIndex;

    final ReadonlyViewRMSE viewErrorMetric;

    final ReadonlyViewSet containingViewSet;

    File maskFile;

    boolean isDisabled;

    /**
     * Creates a new view set data object with parameters for each member.
     */
    View(Matrix4 cameraPose, Matrix4 cameraPoseInv, int cameraProjectionIndex, int lightIndex,
         int gpuViewIndex, File imageFile, File maskFile, ReadonlyViewRMSE viewErrorMetric, ReadonlyViewSet containingViewSet)
    {
        this.cameraPose = cameraPose;
        this.cameraPoseInv = cameraPoseInv;
        this.cameraProjectionIndex = cameraProjectionIndex;
        this.lightIndex = lightIndex;
        this.gpuViewIndex = gpuViewIndex;
        this.imageFile = imageFile;
        this.maskFile = maskFile;
        this.viewErrorMetric = viewErrorMetric;
        this.containingViewSet = containingViewSet;
        this.isDisabled = false;
    }

    public Matrix4 getCameraPose()
    {
        return cameraPose;
    }

    public Matrix4 getCameraPoseInv()
    {
        return cameraPoseInv;
    }

    public int getCameraProjectionIndex()
    {
        return cameraProjectionIndex;
    }

    public Projection getCameraProjection()
    {
        return containingViewSet.getCameraProjection(cameraProjectionIndex);
    }

    public int getLightIndex()
    {
        return lightIndex;
    }

    public File getImageFile()
    {
        return imageFile;
    }

    public int getGPUViewIndex()
    {
        return gpuViewIndex;
    }

    public File getMaskFile()
    {
        return maskFile;
    }

    public ReadonlyViewRMSE getViewErrorMetric()
    {
        return viewErrorMetric;
    }

    public boolean isDisabled()
    {
        return isDisabled;
    }

    public ReadonlyViewSet getContainingViewSet()
    {
        return containingViewSet;
    }

    public File getFullResImageFile()
    {
        return new File(containingViewSet.getFullResImageDirectory(), imageFile.getPath());
//        return viewSetDataCollection.getFullResImageFile(poseIndex);
    }

    public File findFullResImageFile() throws FileNotFoundException
    {
        return ImageFinder.getInstance().findImageFile(getFullResImageFile());
    }

    public File getPreviewImageFile(String extension)
    {
        return new File(containingViewSet.getPreviewImageDirectory(),
            ImageFinder.getInstance().getImageFileNameWithExtension(imageFile.getName(), extension));
    }

    public File getThumbnailImageFile(String extension)
    {
        return new File(containingViewSet.getThumbnailImageDirectory(),
            ImageFinder.getInstance().getImageFileNameWithExtension(imageFile.getName(), extension));
    }

    public File findThumbnailImageFile() throws FileNotFoundException
    {
        return ImageFinder.getInstance().findImageFile(getThumbnailImageFile());
    }

    public File getThumbnailImageFile()
    {
        return getThumbnailImageFile("png");
    }
}
