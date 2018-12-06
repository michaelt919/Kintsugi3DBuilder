/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.imagedata;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;

import umn.gl.nativebuffer.NativeVectorBuffer;
import umn.gl.vecmath.Matrix4;
import umn.gl.vecmath.Vector3;

public interface ViewSet
{
    /**
     * Gets a buffer containing camera poses defining the transformation from object space to camera space for each view.
     * These are necessary to perform projective texture mapping.
     * This buffer can be used to initialize a camera pose uniform buffer in a graphics context for use with shader programs.
     * @return The camera pose data buffer
     */
    NativeVectorBuffer getCameraPoseData();

    /**
     * Gets a buffer containing projection transformations defining the intrinsic properties of each camera.
     * The number of entries in this buffer can be much smaller than the number of views if the same intrinsic properties apply for multiple views.
     * This buffer can be used to initialize a camera projection uniform buffer in a graphics context for use with shader programs.
     * @return The camera projection data buffer
     */
    NativeVectorBuffer getCameraProjectionData();

    /**
     * Gets a buffer containing an entry for every view which designates the index of the projection transformation that should be used for each view.
     * This buffer can be used to initialize a camera projection index uniform buffer in a graphics context for use with shader programs.
     * @return The camera projection index data buffer.
     */
    NativeVectorBuffer getCameraProjectionIndexData();

    /**
     * Gets a buffer containing light source positions.
     * Assumed by convention to be in camera space.
     * The number of entries in this buffer can be much smaller than the number of views if the same illumination conditions apply for multiple views.
     * This buffer can be used to initialize a light position uniform buffer in a graphics context for use with shader programs.
     * @return The light position data buffer.
     */
    NativeVectorBuffer getLightPositionData();

    /**
     * Gets a buffer containing light source intensities.
     * The number of entries in this buffer can be much smaller than the number of views if the same illumination conditions apply for multiple views.
     * This buffer can be used to initialize a light intensity uniform buffer in a graphics context for use with shader programs.
     * @return The light intensity data buffer.
     */
    NativeVectorBuffer getLightIntensityData();

    /**
     * Gets a buffer containing an entry for every view which designates the index of the light source position and intensity that should be used for
     * each view.  This buffer can be used to initialize a light index uniform buffer in a graphics context for use with shader programs.
     * @return The light index data buffer.
     */
    NativeVectorBuffer getLightIndexData();

    /**
     * Creates a new view set by reordering the views in the current view set.
     * @param permutationIndices The indices of the views in the current view set, listed in the order that they are to have in the new view set.
     * @return A new view set with the views permuted according to the permutation indices.
     */
    ViewSet createPermutation(Iterable<Integer> permutationIndices);

    /**
     * Writes this view set to a text stream, using the VSET file format.
     * @param outputStream The stream to which to write the view set.
     */
    void writeVSETFileToStream(OutputStream outputStream);

    /**
     * Writes this view set to a text stream, using the VSET file format.
     * @param outputStream The stream to which to write the view set.
     * @param parentDirectory The directory to assume as the parent directory of the VSET file for the purpose of establishing relative file paths.
     */
    void writeVSETFileToStream(OutputStream outputStream, Path parentDirectory);

    /**
     * Gets the camera pose defining the transformation from object space to camera space for a particular view.
     * @param poseIndex index of the pose for which to retrieve the camera transformation.
     * @return The camera pose as a 4x4 affine transformation matrix (i.e., the model/view matrix)
     */
    Matrix4 getCameraPose(int poseIndex);

    /**
     * Gets the inverse camera pose, which defines the transformation from camera space to object space for a particular view.
     * @param poseIndex index of the pose for which to retrieve the camera transformation.
     * @return The inverse camera pose as a 4x4 affine transformation matrix (i.e., the inverse of the model/view matrix)
     */
    Matrix4 getCameraPoseInverse(int poseIndex);

    /**
     * Gets the absolute file path to be used for loading all resources.
     * @return The file path for the root directory
     */
    File getRootDirectory();

    /**
     * Sets the absolute file path to be used for loading all resources.
     * @param rootDirectory The file path for the root directory
     */
    void setRootDirectory(File rootDirectory);

    /**
     * Gets the name of the geometry file associated with this view set, using a file path relative to the root directory.
     * @return The name of the geometry file.
     */
    String getGeometryFileName();

    /**
     * Sets the name of the geometry file associated with this view set, using a file path relative to the root directory.
     * @param fileName The name of the geometry file.
     */
    void setGeometryFileName(String fileName);

    /**
     * Gets the geometry file associated with this view set.
     * @return The geometry file.
     */
    File getGeometryFile();

    /**
     * Gets a string containing the image file path associated with this view set, relative to the root directory.
     * @return The image file path string.
     */
    String getRelativeImagePathName();

    /**
     * Sets the image file path associated with this view set.
     * @param relativeImagePath A string containing the image file path, relative to the root directory.
     */
    void setRelativeImagePathName(String relativeImagePath);

    /**
     * Gets the image file path associated with this view set.
     * @return The image file path.
     */
    File getImageFilePath();

    /**
     * Gets the relative name of the image file corresponding to a particular view.
     * @param poseIndex The index of the image file to retrieve.
     * @return The image file's relative name.
     */
    String getImageFileName(int poseIndex);

    /**
     * Gets the image file corresponding to a particular view.
     * @param poseIndex The index of the image file to retrieve.
     * @return The image file.
     */
    File getImageFile(int poseIndex);

    /**
     * Gets the index of a view that should have the same camera displacement and orientation as the photo that was used to obtain any color
     * calibration measurements.  It is used to automatically compute the intensity of the light source, which is in turn used to obtain absolute
     * reflectance measurements.
     * @return The index of the primary view.
     */
    int getPrimaryViewIndex();

    /**
     * Sets the index of a view that should have the same camera displacement and orientation as the photo that was used to obtain any color
     * calibration measurements.  It is used to automatically compute the intensity of the light source, which is in turn used to obtain absolute
     * reflectance measurements.
     * @param poseIndex The index of the primary view.
     */
    void setPrimaryView(int poseIndex);

    /**
     * Sets, by name, a view that should have the same camera displacement and orientation as the photo that was used to obtain any color
     * calibration measurements.  It is used to automatically compute the intensity of the light source, which is in turn used to obtain absolute
     * reflectance measurements.
     * @param viewName The name of the primary view.
     */
    void setPrimaryView(String viewName);

    /**
     * Gets the projection transformation defining the intrinsic properties of a particular camera.
     * @param projectionIndex The index of the camera whose projection transformation is to be retrieved.
     * IMPORTANT: this is NOT usually the same as the camera pose index for a particular view.
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
     * IMPORTANT: this is NOT usually the same as the camera pose index for a particular view.
     * @return The position of the light source.
     */
    Vector3 getLightPosition(int lightIndex);

    /**
     * Gets the intensity of a particular light source.
     * Used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * @param lightIndex The index of the light source.
     * IMPORTANT: this is NOT usually the same as the camera pose index for a particular view.
     * @return The intensity of the light source.
     */
    Vector3 getLightIntensity(int lightIndex);

    /**
     * Sets the position of a particular light source.
     * Used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * Assumed by convention to be in camera space.
     * @param lightIndex The index of the light source.
     * IMPORTANT: this is NOT usually the same as the camera pose index for a particular view.
     * @param lightPosition The position of the light source.
     */
    void setLightPosition(int lightIndex, Vector3 lightPosition);

    /**
     * Sets the intensity of a particular light source.
     * Used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * @param lightIndex The index of the light source.
     * IMPORTANT: this is NOT usually the same as the camera pose index for a particular view.
     * @param lightIntensity The intensity of the light source.
     */
    void setLightIntensity(int lightIndex, Vector3 lightIntensity);

    /**
     * Gets the index of the light source to be used for a particular view,
     * which can subsequently be used with getLightPosition() and getLightIntensity() to obtain the actual position and intensity of the light source.
     * @param poseIndex The index of the view.
     * @return The index of the light source.
     */
    int getLightIndex(int poseIndex);

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

    /**
     * Gets an exponent defining the color curve of the image.
     * Used to decode pixel colors according to a gamma curve if reference values are unavailable, otherwise, affects the absolute brightness of the decoded colors.
     * @return The exponent used for gamma encoding/decoding.
     */
    float getGamma();

    /**
     * Gets whether or not the view set has a custom luminance encoding defined (generally obtained using color calibration samples).
     * @return true, if a custom luminance encoding is defined; false otherwise.
     */
    boolean hasCustomLuminanceEncoding();

    /**
     * Gets an object encapsulating the luminance encoding of the images in this view set that can be sampled to perform conversions to and from linear and tonemapped color spaces.
     * @return The luminance encoding object.
     */
    SampledLuminanceEncoding getLuminanceEncoding();

    /**
     * Sets the tonemapping curve for this view set by simultaneously setting the gamma exponent and the luminance encoding.
     * @param gamma The exponent used for gamma encoding/decoding.
     * @param linearLuminanceValues An array containing the known, linear reflectance values of one or more color calibration targets.
     * This array should have the same length as encodedLuminanceValues.  If this is not the case, behavior may be undefined.
     * Setting both linearLuminanceValues and encodedLuminanceValues to null has the effect of removing any custom luminance encoding so that an
     * exponential gamma curve is used exclusively.
     * @param encodedLuminanceValues An array containing the representative pixel values (ranging from 0-255) for one or more color calibration targets.
     * The values in the array are assumed to be unsigned bytes; this means that it is necessary to mask each value with 0x000000FF in order for Java
     * to correctly interpret the value as an exclusively positive value between 0 and 255.
     * This array should have the same length as linearLuminanceValues.  If this is not the case, behavior may be undefined.
     * Setting both linearLuminanceValues and encodedLuminanceValues to null has the effect of removing any custom luminance encoding so that an
     * exponential gamma curve is used exclusively.
     */
    void setTonemapping(float gamma, double[] linearLuminanceValues, byte[] encodedLuminanceValues);

    /**
     * Gets whether or not the light sources are infinitely far away from the object (i.e. a directional light).
     * If this is not the case, the light sources are assumed to be positioned relative to the camera.
     * @return true if light sources are assumed to be infinitely far away; false otherwise.
     */
    boolean areLightSourcesInfinite();

    /**
     * Sets whether or not the light sources are infinitely far away from the object (i.e. a directional light).
     * If this is not the case, the light sources are assumed to be positioned relative to the camera.
     * @param infiniteLightSources true if light sources are assumed to be infinitely far away; false otherwise.
     */
    void setInfiniteLightSources(boolean infiniteLightSources);
}
