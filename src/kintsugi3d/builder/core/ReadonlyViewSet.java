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

import kintsugi3d.builder.core.metrics.ViewRMSE;
import kintsugi3d.builder.state.settings.ReadonlyGeneralSettingsModel;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    UUID getUUID();
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
    File getSupportingFilesDirectory();

    /**
     * Gets the file path of the supporting files (i.e. texture fit results) associated with this view set, relative to the root directory.
     * @return The relative file path of the supporting files.
     */
    String getRelativeSupportingFilesPathName();

    /**
     * Gets the full resolution image file path associated with this view set.
     * @return The image file path.
     */
    File getFullResImageDirectory();

    /**
     * Gets the full resolution image file path string associated with this view set, relative to the root directory.
     * @return imageFilePath The image file path.
     */
    String getRelativeFullResImagePathName();

    /**
     * Gets the image file path for downscaled "preview" images for real-time rendering
     * @return The image file path.
     */
    File getPreviewImageDirectory();

    /**
     * Gets the image file path for the downscaled "thumbnail" images for display.
     * @return The image file path
     */
    File getThumbnailImageDirectory();

    /**
     * Gets the image file path string for downscaled "preview" images for real-time rendering, relative to the root directory.
     * @return The image file path.
     */
    String getRelativePreviewImagePathName();

    /**
     * Gets a list of all image files.
     * This method should be used to retrieve a filename representing the actual location of the full-res image file.
     * In contexts when the relative path is unwanted, use getImageFileName() instead.
     * @return The list of image files
     */
    List<File> getImageFiles();

    /**
     * Gets the image file corresponding to a particular view, relative to the full res image directory.
     * This method should be used to retrieve a filename representing the actual location of the full-res image file.
     * In contexts when the relative path is unwanted, use getImageFileName() instead.
     * @param poseIndex The index of the image file to retrieve.
     * @return The image file's relative name.
     */
    File getImageFile(int poseIndex);

    /**
     * Gets the name of the image file corresponding to a particular view.
     * This method should be used to retrieve the filename in contexts when the relative path is unwanted.
     * (i.e. for creating or finding images with the same name as the full-res image file, but which are in a different directory).
     * To retrieve a filename representing the actual location of the full-res image file, use getImageFile() instead.
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
     * @param extension The file extension without the dot (i.e. "png", "jpeg", "tiff") to use for the file.
     * @return The image file.
     */
    File getPreviewImageFile(int poseIndex, String extension);

    /**
     * Gets the downscaled "preview" image file corresponding to a particular view.
     * @param poseIndex The index of the image file to retrieve.
     * @return The image file.
     */
    File getPreviewImageFile(int poseIndex);

    /**
     * Gets the downscaled "thumbnail" image file corresponding to a partiulr view.
     * @param poseIndex The index of the image file to retrieve.
     * @param extension The file extension without the dot (i.e. "png", "jpeg", "tiff") to use for the file.
     * @return The image file.
     */
    File getThumbnailImageFile(int poseIndex, String extension);

    /**
     * Gets the downscaled "thumbnail" image file corresponding to a partiulr view.
     * @param poseIndex The index of the image file to retrieve.
     * @return The image file.
     */
    File getThumbnailImageFile(int poseIndex);

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
     * @param projectionIndex The index of the projection transformation to retrieve.
     * IMPORTANT: this is NOT usually the same as the index of the view to be retrieved.
     * @return The projection transformation.
     */
    Projection getCameraProjection(int projectionIndex);

    /**
     * Gets the projection transformation defining the intrinsic properties for a specific view.
     * @param viewIndex The index of the view whose projection transformation should be retrieved.
     * This works by first finding the camera projection index for the view and then looking up the projection itself by that index.
     * @return The projection transformation.
     */
    Projection getCameraProjectionForViewIndex(int viewIndex);

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

    File findThumbnailImageFile(int index) throws FileNotFoundException;

    File tryFindFullResImageFile(int index);

    File tryFindPreviewImageFile(int index);

    File tryFindThumbnailImageFile(int index);

    File findPreviewPrimaryImageFile() throws FileNotFoundException;

    boolean hasMasks();

    File getMasksDirectory();

    Map<Integer, File> getMasksMap();

    ImageHelper loadFullResMaskedImage(int index) throws IOException;

    /**
     * Gets additional settings associated with this view set
     * @return A model containing the settings for this view set.
     */
    ReadonlyGeneralSettingsModel getProjectSettings();

    Map<String, File> getResourceMap();

    <ContextType extends Context<ContextType>> ProgramBuilder<ContextType> getShaderProgramBuilder(ContextType context);

    <ContextType extends Context<ContextType>> void setupShaderProgram(Program<ContextType> program);
}
