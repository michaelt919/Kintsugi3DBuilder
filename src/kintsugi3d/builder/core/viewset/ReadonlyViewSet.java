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

import kintsugi3d.builder.state.settings.ReadonlyGeneralSettingsModel;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import kintsugi3d.gl.vecmath.Matrix3;
import kintsugi3d.gl.vecmath.Vector3;

import java.io.File;
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

    /**
     * Note: not currently guaranteed to be thead-safe
     * @return
     */
    ViewSet copy();

    UUID getUUID();

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
     * Gets orientation imported, to be applied to the model
     * @return Orientation as a 3x3 matrix
     */
    Matrix3 getOrientationMatrix();

    /**
     * Gets object translation imported, to be applied to the model
     * @return translation as a vector
     */
    Vector3 getObjectTranslation();

    /**
     * Gets object scale imported, to be applied to the model
     * @return uniform scale as a scalar value
     */
    float getObjectScale();

    int getPreviewWidth();

    int getPreviewHeight();

    /**
     * Gets the view to be used for color calibration and tonemapping operations
     * @return view
     */
    View getPrimaryView();

    /**
     * Gets the view index to use as a reference pose for reorienting the model
     * @return view index
     */
    View getOrientationView();

    /**
     * Roll rotation of the reference view pose to correct upside down and sideways images
     * @return view index
     */
    double getOrientationViewRotationDegrees();

    /**
     * Gets a representative view for purposes where an arbitrary view is needed.
     * This will return the orientation view if it exists, otherwise, the primary view for tonemapping,
     * otherwise, the first view in the list.
     * @return
     */
    View getRepresentativeView();

    List<View> getViews();

    /**
     * Gets the number of views defined in this view set.
     * @return The number of views defined in this view set.
     */
    int getViewCount();

    /**
     * Gets the size for any array on the GPU that is indexed by view.
     * This may be greater than the "view count" if views were deleted after the project was loaded.
     * @return
     */
    int getGPUBufferSize();


    /**
     * Gets the projection transformation defining the intrinsic properties of a particular camera.
     * @param projectionIndex The index of the projection transformation to retrieve.
     * IMPORTANT: this is NOT usually the same as the index of the view to be retrieved.
     * @return The projection transformation.
     */
    Projection getCameraProjection(int projectionIndex);
    /**
     * Gets the number of projection transformations defined in this view set.
     * @return The number of projection transformations defined in this view set.
     */
    int getCameraProjectionCount();

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

    boolean hasMasks();

    File getMasksDirectory();

    Map<Integer, File> getMasksMap();

    /**
     * Gets additional settings associated with this view set
     * @return A model containing the settings for this view set.
     */
    ReadonlyGeneralSettingsModel getProjectSettings();

    Map<String, File> getResourceMap();

    <ContextType extends Context<ContextType>> ProgramBuilder<ContextType> getShaderProgramBuilder(ContextType context);

    <ContextType extends Context<ContextType>> void setupShaderProgram(Program<ContextType> program);
}
