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

import kintsugi3d.builder.core.metrics.ViewRMSE;
import kintsugi3d.builder.state.settings.DefaultSettings;
import kintsugi3d.builder.state.settings.ReadonlyGeneralSettingsModel;
import kintsugi3d.gl.vecmath.Matrix3;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ViewSetBuilder
{
    private static final Logger LOG = LoggerFactory.getLogger(ViewSetBuilder.class);

    private final ViewSet result;
    private boolean needsClipPlanes = true;

    private Matrix4 cameraPose;
    private int cameraProjectionIndex = 0;
    private int lightIndex = 0;
    private File imageFile;
    private File maskFile;
    private final Map<Integer, File> maskMap;
    private boolean hasUnsupportedCorrections;

    private int orientationViewIndex = -1;
    private String orientationViewName;

    /**
     * Uses root directory as supporting files directory by default
     *
     * @param rootDirectory
     * @param initialCapacity
     */
    ViewSetBuilder(@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter") File rootDirectory, int initialCapacity)
    {
        this(rootDirectory, rootDirectory, initialCapacity);
    }

    ViewSetBuilder(File rootDirectory, File supportingFilesDirectory, int initialCapacity)
    {
        result = new ViewSet(initialCapacity);
        result.setRootDirectory(rootDirectory);
        result.setSupportingFilesDirectory(supportingFilesDirectory);

        maskMap = new HashMap<>(initialCapacity);

        // Initialize settings with defaults.
        DefaultSettings.applyProjectDefaults(result.getProjectSettings());
    }

    public ViewSetBuilder setCurrentCameraPose(Matrix4 cameraPose)
    {
        this.cameraPose = cameraPose;
        return this;
    }

    public ViewSetBuilder setCurrentCameraProjectionIndex(int cameraProjectionIndex)
    {
        this.cameraProjectionIndex = cameraProjectionIndex;
        return this;
    }

    public ViewSetBuilder setCurrentLightIndex(int lightIndex)
    {
        this.lightIndex = lightIndex;
        return this;
    }

    public ViewSetBuilder setCurrentImageFile(File imageFile)
    {
        this.imageFile = imageFile;
        return this;
    }

    public ViewSetBuilder setCurrentMaskFile(File maskFile)
    {
        this.maskFile = maskFile;
        return this;
    }

    public ViewSetBuilder commitCurrentView()
    {
        commitCurrentView(true);
        return this;
    }

    public ViewSetBuilder commitCurrentViewAsDisabled()
    {
        commitCurrentView(false);
        return this;
    }

    private void commitCurrentView(boolean enabled)
    {
        if (maskFile == null)
        {
            // We haven't committed this view yet, so size of the view set data will just be the current index.
            maskFile = maskMap.get(result.getGPUBufferSize());
        }

        View currentCamera = new View(cameraPose, cameraPose.quickInverse(0.002f),
            cameraProjectionIndex, lightIndex, result.getGPUBufferSize(),
            imageFile, maskFile, new ViewRMSE(), result);
        currentCamera.isEnabled = enabled;
        result.addView(currentCamera);

        // Reset maskFile to null for the next camera pose.
        maskFile = null;
    }

    public ViewSetBuilder removeViewsByImageFilename(Iterable<File> disabledImageFiles)
    {
        for (File f : disabledImageFiles)
        {
            result.removeViewByImageFilename(f);
        }

        // Reassign view indices for smaller data set.
        result.optimizeGPUIndexing();

        return this;
    }

    public ViewSetBuilder addCameraProjection(Projection projection)
    {
        result.addCameraProjection(projection);
        return this;
    }

    public int getNextCameraProjectionIndex()
    {
        return result.getCameraProjectionCount();
    }

    public ViewSetBuilder addLight(Vector3 position, Vector3 intensity)
    {
        result.addLight(position, intensity);
        return this;
    }

    public int getNextLightIndex()
    {
        return result.getLightCount();
    }

    public ViewSetBuilder setUUID(UUID uuid)
    {
        result.setUUID(uuid);
        return this;
    }

    public ViewSetBuilder setRecommendedClipPlanes(float near, float far)
    {
        result.setRecommmendedClipPlanes(near, far);
        needsClipPlanes = false;
        return this;
    }

    public ViewSetBuilder setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        result.setLuminanceEncoding(linearLuminanceValues, encodedLuminanceValues);
        return this;
    }

    /**
     * Sets the geometry file associated with this view set.
     *
     * @param geometryFile The geometry file.
     */
    public ViewSetBuilder setGeometryFile(File geometryFile)
    {
        result.setGeometryFile(geometryFile);
        return this;
    }

    /**
     * Sets the name of the geometry file associated with this view set relative to the root directory.
     *
     * @param geometryFileName The name of the geometry file.
     */
    public ViewSetBuilder setGeometryFileName(String geometryFileName)
    {
        result.setGeometryFile(
            geometryFileName == null ? null : result.getRootDirectory().toPath().resolve(geometryFileName).toFile());
        return this;
    }

    /**
     * Sets the full res image directory associated with this view set.
     *
     * @param fullResImageDirectory The full res image directory.
     */
    public ViewSetBuilder setFullResImageDirectory(File fullResImageDirectory)
    {
        result.setFullResImageDirectory(fullResImageDirectory);
        return this;
    }

    /**
     * Sets the name of the full res image directory associated with this view set relative to the root directory.
     *
     * @param relativePath The path to the full res images.
     */
    public ViewSetBuilder setRelativeFullResImagePathName(String relativePath)
    {
        result.setRelativeFullResImagePathName(relativePath);
        return this;
    }

    /**
     * Sets the relative file path of the supporting files (i.e. texture fit results) associated with this view set.
     *
     * @param relativePath The file path of the supporting files directory.
     */
    public ViewSetBuilder setRelativeSupportingFilesPathName(String relativePath)
    {
        result.setSupportingFilesDirectory(result.getRootDirectory().toPath().resolve(relativePath).toFile());
        return this;
    }

    public ViewSetBuilder setRelativePreviewImagePathName(String relativePath)
    {
        result.setRelativePreviewImagePathName(relativePath);
        return this;
    }

    public ViewSetBuilder setOrientationViewByIndex(int viewIndex)
    {
        // Defer application until all views have been loaded.
        this.orientationViewIndex = viewIndex;
        this.orientationViewName = null;
        return this;
    }

    public ViewSetBuilder setOrientationViewByName(String viewName)
    {
        // Defer application until all views have been loaded.
        this.orientationViewName = viewName;
        this.orientationViewIndex = -1;
        return this;
    }

    public ViewSetBuilder setOrientationViewRotation(double rotation)
    {
        result.setOrientationViewRotationDegrees(rotation);
        return this;
    }

    public ViewSetBuilder setOrientationMatrix(Matrix3 matrix)
    {
        result.setOrientationMatrix(matrix);
        return this;
    }

    public ViewSetBuilder setObjectTranslation(Vector3 objectTranslation)
    {
        result.setObjectTranslation(objectTranslation);
        return this;
    }

    public ViewSetBuilder setObjectScale(float objectScale)
    {
        result.setObjectScale(objectScale);
        return this;
    }

    public ViewSetBuilder setMasksDirectory(File file)
    {
        result.setMasksDirectory(file);
        return this;
    }

    public ViewSetBuilder addMask(int camId, String imgFilename)
    {
        maskMap.put(camId, new File(imgFilename));
        return this;
    }

    public ViewSetBuilder applySettings(ReadonlyGeneralSettingsModel settings)
    {
        result.getProjectSettings().copyFrom(settings);
        return this;
    }

    public ViewSetBuilder addResourceFiles(Map<String, File> resourceMap)
    {
        result.getResourceMap().putAll(resourceMap);
        return this;
    }

    public ViewSetBuilder setHasUnsupportedCorrections(boolean hasUnsupportedCorrections)
    {
        this.hasUnsupportedCorrections = hasUnsupportedCorrections;
        return this;
    }

    public ViewSet finish()
    {
        if (orientationViewName != null)
        {
            result.setOrientationViewByName(orientationViewName);
        }
        else if (orientationViewIndex >= 0)
        {
            result.setOrientationView(result.getViews().get(orientationViewIndex));
        }
        else
        {
            result.setOrientationView(null);
        }

        if (needsClipPlanes)
        {
            float farPlane = findFarPlane(result.getViews());
            result.setRecommmendedClipPlanes(farPlane / 32.0f, farPlane);
            LOG.debug("Near and far planes: {}, {}", result.getRecommendedNearPlane(), result.getRecommendedFarPlane());
        }

        // Fill with default lights if not specified
        int maxLightIndex = result.getViews().stream().mapToInt(data -> data.lightIndex).max().orElse(1);
        for (int i = getNextLightIndex(); i <= maxLightIndex; i = getNextLightIndex())
        {
            result.addLight(Vector3.ZERO, Vector3.ZERO);
        }

        if (result.getGeometryFile() == null && result.getRootDirectory() != null)
        {
            setGeometryFileName("manifold.obj"); // Used by some really old datasets
        }

        if (result.getSupportingFilesDirectory() != null)
        {
            // Make sure the supporting files directory exists
            result.getSupportingFilesDirectory().mkdirs();
        }

        result.setHasUnsupportedCorrections(this.hasUnsupportedCorrections);

        return result;
    }

    /**
     * A subroutine for guessing an appropriate far plane from an Agisoft PhotoScan/Metashape XML file.
     * Assumes that the object must lie between all of the cameras in the file.
     *
     * @param viewSetDataList The list of camera data.
     * @return A far plane estimate.
     */
    private static float findFarPlane(Iterable<View> viewSetDataList)
    {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (View view : viewSetDataList)
        {
            Vector4 position = view.cameraPoseInverse.getColumn(3);
            minX = Math.min(minX, position.x);
            minY = Math.min(minY, position.y);
            minZ = Math.min(minZ, position.z);
            maxX = Math.max(maxX, position.x);
            maxY = Math.max(maxY, position.y);
            maxZ = Math.max(maxZ, position.z);
        }

        // Corner-to-corner
        float dX = maxX - minX;
        float dY = maxY - minY;
        float dZ = maxZ - minZ;
        return (float) Math.sqrt(dX * dX + dY * dY + dZ * dZ);

        // Longest Side approach
//        return Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
    }
}
