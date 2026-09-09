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
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.ImageFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

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
    final Matrix4 cameraPoseInverse;

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

    /**
     * Only intended to be modified by ViewSet.optimizeGPUIndexing, during initialization by ViewSetBuilder.
     */
    int gpuViewIndex;

    final ReadonlyViewRMSE viewErrorMetric;

    final ReadonlyViewSet containingViewSet;

    File maskFile;

    boolean isEnabled;

    /**
     * Creates a new view set data object with parameters for each member.
     * @param cameraPose
     * @param cameraPoseInverse
     * @param cameraProjectionIndex
     * @param lightIndex
     * @param gpuViewIndex
     * @param imageFile Cannot be null.
     * @param maskFile
     * @param viewErrorMetric
     * @param containingViewSet
     */
    View(Matrix4 cameraPose, Matrix4 cameraPoseInverse, int cameraProjectionIndex, int lightIndex,
         int gpuViewIndex, File imageFile, File maskFile, ReadonlyViewRMSE viewErrorMetric,
         ReadonlyViewSet containingViewSet)
    {
        if (imageFile == null)
        {
            throw new IllegalArgumentException("Image file cannot be null.");
        }

        this.cameraPose = cameraPose;
        this.cameraPoseInverse = cameraPoseInverse;
        this.cameraProjectionIndex = cameraProjectionIndex;
        this.lightIndex = lightIndex;
        this.gpuViewIndex = gpuViewIndex;
        this.imageFile = imageFile;
        this.maskFile = maskFile;
        this.viewErrorMetric = viewErrorMetric;
        this.containingViewSet = containingViewSet;
        this.isEnabled = true;
    }

    public Matrix4 getCameraPose()
    {
        return cameraPose;
    }

    public Matrix4 getCameraPoseInverse()
    {
        return cameraPoseInverse;
    }

    public int getCameraProjectionIndex()
    {
        return cameraProjectionIndex;
    }

    public Projection getCameraProjection()
    {
        return containingViewSet.getCameraProjection(cameraProjectionIndex);
    }

    public Matrix4 getProjectionMatrix()
    {
        return getCameraProjection().getProjectionMatrix(
            containingViewSet.getRecommendedNearPlane(), containingViewSet.getRecommendedFarPlane());
    }

    public int getLightIndex()
    {
        return lightIndex;
    }

    public Vector3 getLightPosition()
    {
        return containingViewSet.getLightPosition(lightIndex);
    }

    public Vector3 getLightIntensity()
    {
        return containingViewSet.getLightIntensity(lightIndex);
    }

    public File getImageFile()
    {
        return imageFile;
    }

    public int getGPUViewIndex()
    {
        return gpuViewIndex;
    }

    public ReadonlyViewRMSE getViewErrorMetric()
    {
        return viewErrorMetric;
    }

    public boolean isEnabled()
    {
        return isEnabled;
    }

    public ReadonlyViewSet getContainingViewSet()
    {
        return containingViewSet;
    }

    public File getFullResImageFile()
    {
        return new File(containingViewSet.getFullResImageDirectory(), imageFile.getPath());
    }

    public File findFullResImageFile() throws FileNotFoundException
    {
        return ImageFinder.getInstance().findImageFile(getFullResImageFile());
    }

    public File tryFindFullResImageFile()
    {
        return ImageFinder.getInstance().tryFindImageFile(getFullResImageFile());
    }

    public File getPreviewImageFile(String extension)
    {
        return new File(containingViewSet.getPreviewImageDirectory(),
            ImageFinder.getInstance().getImageFileNameWithExtension(imageFile.getName(), extension));
    }

    public File getPreviewImageFile()
    {
        // Use PNG for preview images by default (TODO: make this a configurable setting?)
        return getPreviewImageFile("png");
    }

    public File findPreviewImageFile() throws FileNotFoundException
    {
        return ImageFinder.getInstance().findImageFile(getPreviewImageFile());
    }

    public File tryFindPreviewImageFile()
    {
        return ImageFinder.getInstance().tryFindImageFile(getPreviewImageFile());
    }

    public File getThumbnailImageFile(String extension)
    {
        return new File(containingViewSet.getThumbnailImageDirectory(),
            ImageFinder.getInstance().getImageFileNameWithExtension(imageFile.getName(), extension));
    }

    public File getThumbnailImageFile()
    {
        return getThumbnailImageFile("png");
    }

    public File findThumbnailImageFile() throws FileNotFoundException
    {
        return ImageFinder.getInstance().findImageFile(getThumbnailImageFile());
    }

    public File tryFindThumbnailImageFile()
    {
        return ImageFinder.getInstance().tryFindImageFile(getThumbnailImageFile());
    }

    public File getMaskFile()
    {
        if (maskFile == null || containingViewSet.getMasksDirectory() == null)
        {
            // Not all images have masks, so this file may still not exist
            return null;
        }
        else
        {
            return new File(containingViewSet.getMasksDirectory(), maskFile.getName());
        }
    }

    public ImageHelper loadFullResMaskedImage() throws IOException
    {
        return ImageHelper.read(findFullResImageFile()).withAlphaMask(getMaskFile());
    }

    @Override
    public String toString()
    {
        return imageFile.getName();
    }

    public View copy(ViewSet newContainingViewSet)
    {
        // TODO do we need to also do a deep copy of viewErrorMetric?
        return new View(this.cameraPose, this.cameraPoseInverse, this.cameraProjectionIndex, this.lightIndex,
            this.gpuViewIndex, this.imageFile, this.maskFile, this.viewErrorMetric, newContainingViewSet);
    }

    @Override
    public boolean equals(Object obj)
    {
        // Two views are equal either if they have the same memory address / identity OR
        // if they are distinct objects with the same, non-null image file path.
        return obj == this || ((obj instanceof View) && this.imageFile.equals(((View) obj).imageFile));
    }

    @Override
    public int hashCode()
    {
        // If imageFile is not null, use it as a hash code.
        // This ensures that two objects that are equals() will have the same hashCode()
        // either when they are the same object (with a non-null image file path)
        // or if they are different objects with the same, non-null image file path.
        // Otherwise, use the default Object hashCode which covers the case when this object has a null imageFile
        // (and thus is only equal to another reference to itself).
        return Objects.hashCode(imageFile);
    }
}
