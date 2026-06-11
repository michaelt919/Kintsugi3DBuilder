/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.core;

import kintsugi3d.builder.core.metrics.ViewRMSE;
import kintsugi3d.gl.vecmath.Matrix4;

import java.io.File;

/**
 * Container for different data used by ViewSet. Contains cameraPose, cameraPoseInv, cameraProjectionIndex
 * lightIndex, imageFile, maskFile, and viewErrorMetrics.
 */
public class ViewSetData {

    /**
     * A camera pose defining the transformation from object space to camera space for each view.
     * These are necessary to perform projective texture mapping.
     */
    public final Matrix4 cameraPose;

    /**
     * An inverted camera pose defining the transformation from camera space to object space for each view.
     * (Useful for visualizing the cameras on screen).
     */
    public final Matrix4 cameraPoseInv;

    /**
     * An entry which designates the index of the projection transformation that should be used for each view.
     */
    public final int cameraProjectionIndex;

    /**
     * An entry which designates the index of the light source position and intensity that should be used for each view.
     */
    public final int lightIndex;

    /**
     * The relative path of the image file corresponding to this view.
     * The file paths are relative to the fullResImageDirectory
     */
    public final File imageFile;

    public final int viewIndex;

    public File maskFile;

    public final ViewRMSE viewErrorMetric;

    public boolean isDisabled;

    /**
     * Creates a new view set data object with parameters for each member.
     */
    public ViewSetData(Matrix4 cameraPose, Matrix4 cameraPoseInv, int cameraProjectionIndex, int lightIndex,
                       int viewIndex, File imageFile, File maskFile, ViewRMSE viewErrorMetric)
    {
        this.cameraPose = cameraPose;
        this.cameraPoseInv = cameraPoseInv;
        this.cameraProjectionIndex = cameraProjectionIndex;
        this.lightIndex = lightIndex;
        this.viewIndex = viewIndex;
        this.imageFile = imageFile;
        this.maskFile = maskFile;
        this.viewErrorMetric = viewErrorMetric;
        isDisabled = false;
    }
}
