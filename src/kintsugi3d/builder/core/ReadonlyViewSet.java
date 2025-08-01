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

package kintsugi3d.builder.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import kintsugi3d.builder.metrics.ViewRMSE;
import kintsugi3d.builder.state.ReadonlySettingsModel;
import kintsugi3d.builder.state.SettingsModel;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

public interface ReadonlyViewSet
{
    ReadonlyNativeVectorBuffer getCameraPoseData();

    ReadonlyNativeVectorBuffer getCameraProjectionData();

    ReadonlyNativeVectorBuffer getCameraProjectionIndexData();

    ReadonlyNativeVectorBuffer getLightPositionData();

    ReadonlyNativeVectorBuffer getLightIntensityData();

    ReadonlyNativeVectorBuffer getLightIndexData();

    ReadonlyViewSet createPermutation(Collection<Integer> permutationIndices);

    ViewSet copy();

    /**
     * Gets the camera pose defining the transformation from object space to camera space for a particular view.
     * @param poseIndex The index of the camera pose to retrieve.
     * @return The camera pose as a 4x4 affine transformation matrix.
     */
    Matrix4 getCameraPose(int poseIndex);

    /**
     * Gets the inverse of the camera pose, defining the transformation from camera space to object space for a particular view.
     * @param poseIndex The index of the camera pose to retrieve.
     * @return The inverse camera pose as a 4x4 affine transformation matrix.
     */
    Matrix4 getCameraPoseInverse(int poseIndex);

    /**
     * Gets the root directory for this view set.
     * @return The root directory.
     */
    File getRootDirectory();

    /**
     * Gets the name of the geometry file associated with this view set.
     * @return The name of the geometry file.
     */
    String getGeometryFileName();

    /**
     * Gets the geometry file associated with this view set.
     * @return The geometry file.
     */
    File getGeometryFile();

    /**
     * Gets the file path of the supporting files (i.e. texture fit results) associated with this view set.
     * @return The absolute file path of the supporting files.
     */
    File getSupportingFilesFilePath();

    /**
     * Gets the file path of the supporting files (i.e. texture fit results) associated with this view set, relative to the root directory.
     * @return The relative file path of the supporting files.
     */
    String getRelativeSupportingFilesPathName();

    /**
     * Gets the full resolution image file path associated with this view set.
     * @return The image file path.
     */
    File getFullResImageFilePath();

    /**
     * Gets the full resolution image file path string associated with this view set, relative to the root directory.
     * @return imageFilePath The image file path.
     */
    String getRelativeFullResImagePathName();

    /**
     * Gets the image file path for downscaled "preview" images for real-time rendering
     * @return The image file path.
     */
    File getPreviewImageFilePath();

    /**
     * Gets the image file path string for downscaled "preview" images for real-time rendering, relative to the root directory.
     * @return The image file path.
     */
    String getRelativePreviewImagePathName();

    /**
     * Gets the relative name of the image file corresponding to a particular view.
     * @param poseIndex The index of the image file to retrieve.
     * @return The image file's relative name.
     */
    String getImageFileName(int poseIndex);

    /**
     * Gets the full resolution image file corresponding to a particular view.
     * @param poseIndex The index of the image file to retrieve.
     * @return The image file.
     */
    File getFullResImageFile(int poseIndex);

    File getFullResImageFile(String viewName);

    /**
     * Gets the downscaled "preview" image file corresponding to a particular view.
     * @param poseIndex The index of the image file to retrieve.
     * @return The image file.
     */
    File getPreviewImageFile(int poseIndex);

    /**
     * Gets the mask of a particular view, if it exists
     * @param poseIndex The index of the image file to retrieve.
     * @return The image file.
     */
    File getMask(int poseIndex);

    /**
     * Gets the view index to be used for color calibration and tonemapping operations
     * @return view index
     */
    int getPrimaryViewIndex();

    /**
     * Gets the view index to use as a reference pose for reorienting the model
     * @return view index
     */
    int getOrientationViewIndex();

    /**
     * Roll rotation of the reference view pose to correct upside down and sideways images
     * @return view index
     */
    double getOrientationViewRotationDegrees();

    /**
     * Gets the projection transformation defining the intrinsic properties of a particular camera.
     * @param projectionIndex The index of the camera whose projection transformation is to be retrieved.
     * IMPORTANT: this is NOT usually the same as the index of the view to be retrieved.
     * @return The projection transformation.
     */
    Projection getCameraProjection(int projectionIndex);

    /**
     * Gets the index of the projection transformation to be used for a particular view,
     * which can subsequently be used with getCameraProjection() to obtain the corresponding projection transformation itself.
     * @param poseIndex The index of the view.
     * @return The index of the projection transformation.
     */
    int getCameraProjectionIndex(int poseIndex);

    /**
     * Gets the position of a particular light source.
     * Used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * Assumed by convention to be in camera space.
     * @param lightIndex The index of the light source.
     * IMPORTANT: this is NOT usually the same as the index of the view to be retrieved.
     * @return The position of the light source.
     */
    Vector3 getLightPosition(int lightIndex);

    /**
     * Gets the intensity of a particular light source.
     * Used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * Assumed by convention to be in camera space.
     * @param lightIndex The index of the light source.
     * IMPORTANT: this is NOT usually the same as the index of the view to be retrieved.
     * @return The position of the light source.
     */
    Vector3 getLightIntensity(int lightIndex);

    /**
     * Gets the index of the light source to be used for a particular view,
     * which can subsequently be used with getLightPosition() and getLightIntensity() to obtain the actual position and intensity of the light source.
     * @param poseIndex The index of the view.
     * @return The index of the light source.
     */
    int getLightIndex(int poseIndex);

    ViewRMSE getViewErrorMetrics(int poseIndex);

    /**
     * Gets the number of camera poses defined in this view set.
     * @return The number of camera poses defined in this view set.
     */
    int getCameraPoseCount();

    /**
     * Gets the number of projection transformations defined in this view set.
     * @return The number of projection transformations defined in this view set.
     */
    int getCameraProjectionCount();

    /**
     * Gets the number of lights defined in this view set.
     * @return The number of projection transformations defined in this view set.
     */
    int getLightCount();

    /**
     * Gets the recommended near plane to use when rendering this view set.
     * @return The near plane value.
     */
    float getRecommendedNearPlane();

    /**
     * Gets the recommended far plane to use when rendering this view set.
     * @return The far plane value.
     */
    float getRecommendedFarPlane();

    boolean hasCustomLuminanceEncoding();

    SampledLuminanceEncoding getLuminanceEncoding();

    double[] getLinearLuminanceValues();
    byte[] getEncodedLuminanceValues();

    boolean areLightSourcesInfinite();

    /**
     * Finds the image file for a particular view index.
     * @param index The index of the view to find.
     * @return The image file at the specified view index.
     * @throws FileNotFoundException if the image file cannot be found.
     */
    File findFullResImageFile(int index) throws FileNotFoundException;

    /**
     * Finds the image file for the primary view index.
     * @return The image file at the primary view index.
     * @throws FileNotFoundException if the image file cannot be found.
     */
    File findFullResPrimaryImageFile() throws FileNotFoundException;
    File findPreviewImageFile(int index) throws FileNotFoundException;
    File findPreviewPrimaryImageFile() throws FileNotFoundException;

    UUID getUUID();

    boolean hasMasks();

    File getMasksDirectory();

    Map<Integer, File> getMasksMap();

    /**
     * Gets additional settings associated with this view set
     * @return A model containing the settings for this view set.
     */
    ReadonlySettingsModel getViewSetSettings();
}
