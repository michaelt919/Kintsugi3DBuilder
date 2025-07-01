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
import java.util.*;

import kintsugi3d.builder.metrics.ViewRMSE;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;
import kintsugi3d.util.ImageFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class representing a collection of photographs, or views.
 * @author Michael Tetzlaff
 */
public final class ViewSet implements ReadonlyViewSet
{
    private static final Logger log = LoggerFactory.getLogger(ViewSet.class);

    /**
     * A unique id given to each view set that can be used to prevent cache collisions on disk.
     */
    private UUID uuid = UUID.randomUUID();

    /**
     * A list of camera poses defining the transformation from object space to camera space for each view.
     * These are necessary to perform projective texture mapping.
     */
    private final List<Matrix4> cameraPoseList;

    /**
     * A list of inverted camera poses defining the transformation from camera space to object space for each view.
     * (Useful for visualizing the cameras on screen).
     */
    private final List<Matrix4> cameraPoseInvList;

    /**
     * A list of projection transformations defining the intrinsic properties of each camera.
     * This list can be much smaller than the number of views if the same intrinsic properties apply for multiple views.
     */
    private final List<Projection> cameraProjectionList;

    /**
     * A list containing an entry for every view which designates the index of the projection transformation that should be used for each view.
     */
    private final List<Integer> cameraProjectionIndexList;

    /**
     * A list of light source positions, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * Assumed by convention to be in camera space.
     * This list can be much smaller than the number of views if the same illumination conditions apply for multiple views.
     */
    private final List<Vector3> lightPositionList;

    /**
     * A list of light source intensities, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * This list can be much smaller than the number of views if the same illumination conditions apply for multiple views.
     */
    private final List<Vector3> lightIntensityList;

    /**
     * A list containing an entry for every view which designates the index of the light source position and intensity that should be used for each view.
     */
    private final List<Integer> lightIndexList;

    /**
     * A list containing the relative path of the image file corresponding to each view.
     * The file paths are relative to the fullResImageDirectory
     */
    private final List<File> imageFiles;

    private final List<File> maskFiles;

    private final List<ViewRMSE> viewErrorMetrics;

    /**
     * The reference linear luminance values used for decoding pixel colors.
     */
    private double[] linearLuminanceValues;

    /**
     * The reference encoded luminance values used for decoding pixel colors.
     */
    private byte[] encodedLuminanceValues;

    /**
     * The absolute file path to be used for loading all resources.
     */
    private File rootDirectory;

    /**
     * The directory to be used for loading images. It is an absolute file path.
     */
    private File fullResImageDirectory;

    /**
     * The directory to be used for saving preview images.
     */
    private File previewImageDirectory;

    /**
     * The directory where the results of the texture / specular fitting are stored
     */
    private File supportingFilesDirectory;

    /**
     * The directory where the masks are stored, if any are present (null if no masks)
     */
    private File masksDirectory;

    /**
     * The mesh file.
     */
    private File geometryFile;

    /**
     * If false, inverse-square light attenuation should be applied.
     */
    private boolean infiniteLightSources = false;

    /**
     * The recommended near plane to use when rendering this view set.
     */
    private float recommendedNearPlane = 0.01f;

    /**
     * The recommended far plane to use when rendering this view set.
     */
    private float recommendedFarPlane = 100.0f;

    /**
     * The index of the view used for color calibration
     */
    private int primaryViewIndex = 0;

    /**
     * The index of the view used to reorient the model
     */
    private int orientationViewIndex = 0;

    /**
     * Roll rotation of the orientation view, used to correct sideways or upside down images
     */
    private double orientationViewRotationDegrees = 0;

    private int previewWidth = 0;
    private int previewHeight = 0;

    public static final class Builder
    {
        private final ViewSet result;
        private boolean needsClipPlanes = true;

        private Matrix4 cameraPose = null;
        private int cameraProjectionIndex = 0;
        private int lightIndex = 0;
        private File imageFile;

        /**
         * Uses root directory as supporting files directory by default
         * @param rootDirectory
         * @param initialCapacity
         */
        Builder(File rootDirectory, int initialCapacity)
        {
            this(rootDirectory, rootDirectory, initialCapacity);
        }

        Builder(File rootDirectory, File supportingFilesDirectory, int initialCapacity)
        {
            result = new ViewSet(initialCapacity);
            result.setRootDirectory(rootDirectory);
            result.setSupportingFilesDirectory(supportingFilesDirectory);
        }

        public Builder setCurrentCameraPose(Matrix4 cameraPose)
        {
            this.cameraPose = cameraPose;
            return this;
        }

        public Builder setCurrentCameraProjectionIndex(int cameraProjectionIndex)
        {
            this.cameraProjectionIndex = cameraProjectionIndex;
            return this;
        }

        public Builder setCurrentLightIndex(int lightIndex)
        {
            this.lightIndex = lightIndex;
            return this;
        }

        public Builder setCurrentImageFile(File imageFile)
        {
            this.imageFile = imageFile;
            return this;
        }

        public Builder commitCurrentCameraPose()
        {
            result.cameraPoseList.add(cameraPose);
            result.cameraPoseInvList.add(cameraPose.quickInverse(0.002f));
            result.cameraProjectionIndexList.add(cameraProjectionIndex);
            result.lightIndexList.add(lightIndex);
            result.imageFiles.add(imageFile);
            result.viewErrorMetrics.add(new ViewRMSE());
            return this;
        }

        public Builder addCameraProjection(Projection projection)
        {
            result.cameraProjectionList.add(projection);
            return this;
        }

        public int getNextCameraProjectionIndex()
        {
            return result.cameraProjectionList.size();
        }

        public Builder addLight(Vector3 position, Vector3 intensity)
        {
            result.lightPositionList.add(position);
            result.lightIntensityList.add(intensity);
            return this;
        }

        public int getNextLightIndex()
        {
            return result.lightPositionList.size();
        }

        public Builder setUUID(UUID uuid)
        {
            result.uuid = uuid;
            return this;
        }

        public Builder setRecommendedClipPlanes(float near, float far)
        {
            result.recommendedNearPlane = near;
            result.recommendedFarPlane = far;
            needsClipPlanes = false;
            return this;
        }

        /**
         * Sets the name of the geometry file associated with this view set.
         * @param geometryFileName The name of the geometry file.
         */
        public Builder setGeometryFileName(String geometryFileName)
        {
            result.geometryFile = geometryFileName == null ? null : result.rootDirectory.toPath().resolve(geometryFileName).toFile();
            return this;
        }

        public Builder setRelativeFullResImagePathName(String relativePath)
        {
            result.setRelativeFullResImagePathName(relativePath);
            return this;
        }

        /**
         * Sets the relative file path of the supporting files (i.e. texture fit results) associated with this view set.
         * @param relativePath The file path of the supporting files directory.
         */
        public Builder setRelativeSupportingFilesPathName(String relativePath)
        {
            result.supportingFilesDirectory = result.rootDirectory.toPath().resolve(relativePath).toFile();
            return this;
        }

        public Builder setRelativePreviewImagePathName(String relativePath)
        {
            result.setRelativePreviewImagePathName(relativePath);
            return this;
        }

        public Builder setOrientationViewIndex(int viewIndex)
        {
            result.orientationViewIndex = viewIndex;
            return this;
        }

        public Builder setOrientationViewRotation(float rotation){
            result.setOrientationViewRotationDegrees(rotation);
            return this;
        }

        public ViewSet finish()
        {
            if (needsClipPlanes)
            {
                result.recommendedFarPlane = findFarPlane(result.cameraPoseInvList);
                result.recommendedNearPlane = result.getRecommendedFarPlane() / 32.0f;
                log.debug("Near and far planes: {}, {}", result.getRecommendedNearPlane(), result.getRecommendedFarPlane());
            }

            // Fill with default lights if not specified
            int maxLightIndex = result.lightIndexList.stream().max(Comparator.naturalOrder()).orElse(-1);
            for (int i = getNextLightIndex(); i <= maxLightIndex; i = getNextLightIndex())
            {
                result.lightPositionList.add(Vector3.ZERO);
                result.lightIntensityList.add(Vector3.ZERO);
            }

            if (result.geometryFile == null && result.rootDirectory != null)
            {
                setGeometryFileName("manifold.obj"); // Used by some really old datasets
            }

            return result;
        }

        /**
         * A subroutine for guessing an appropriate far plane from an Agisoft PhotoScan/Metashape XML file.
         * Assumes that the object must lie between all of the cameras in the file.
         * @param cameraPoseInvList The list of camera poses.
         * @return A far plane estimate.
         */
        private static float findFarPlane(Iterable<Matrix4> cameraPoseInvList)
        {
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;

            for (Matrix4 aCameraPoseInvList : cameraPoseInvList)
            {
                Vector4 position = aCameraPoseInvList.getColumn(3);
                minX = Math.min(minX, position.x);
                minY = Math.min(minY, position.y);
                minZ = Math.min(minZ, position.z);
                maxX = Math.max(maxX, position.x);
                maxY = Math.max(maxY, position.y);
                maxZ = Math.max(maxZ, position.z);
            }

            // Corner-to-corner
            float dX = maxX-minX;
            float dY = maxY-minY;
            float dZ = maxZ-minZ;
            return (float)Math.sqrt(dX*dX + dY*dY + dZ*dZ);

            // Longest Side approach
//        return Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
        }
    }

    public static Builder getBuilder(File rootDirectory, int initialCapacity)
    {
        return new Builder(rootDirectory, initialCapacity);
    }

    public static Builder getBuilder(File rootDirectory, File supportingFilesDirectory, int initialCapacity)
    {
        return new Builder(rootDirectory, supportingFilesDirectory, initialCapacity);
    }


    /**
     * Creates a new view set object.
     * @param initialCapacity The capacity to use for initializing array-based lists that scale with the number of views
     */
    public ViewSet(int initialCapacity)
    {
        this.cameraPoseList = new ArrayList<>(initialCapacity);
        this.cameraPoseInvList = new ArrayList<>(initialCapacity);
        this.cameraProjectionIndexList = new ArrayList<>(initialCapacity);
        this.lightIndexList = new ArrayList<>(initialCapacity);
        this.imageFiles = new ArrayList<>(initialCapacity);
        this.maskFiles = new ArrayList<>(initialCapacity);
        this.viewErrorMetrics = new ArrayList<>(initialCapacity);

        // Often these lists will have just one element
        this.cameraProjectionList = new ArrayList<>(1);
        this.lightIntensityList = new ArrayList<>(1);
        this.lightPositionList = new ArrayList<>(1);
    }

    /**
     * Relative paths from full res image directory
     * @return List of relative paths from full res image directory
     */
    public List<File> getImageFiles()
    {
        return Collections.unmodifiableList(imageFiles);
    }

    @Override
    public ReadonlyNativeVectorBuffer getCameraPoseData()
    {
        // Store the poses in a uniform buffer
        if (cameraPoseList.isEmpty())
        {
            return null;
        }
        else
        {
            // Flatten the camera pose matrices into 16-component vectors and store them in the vertex list data structure.
            NativeVectorBuffer cameraPoseData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 16, cameraPoseList.size());

            for (int k = 0; k < cameraPoseList.size(); k++)
            {
                int d = 0;
                for (int col = 0; col < 4; col++) // column
                {
                    for (int row = 0; row < 4; row++) // row
                    {
                        cameraPoseData.set(k, d, cameraPoseList.get(k).get(row, col));
                        d++;
                    }
                }
            }

            return cameraPoseData;
        }
    }

    @Override
    public ReadonlyNativeVectorBuffer getCameraProjectionData()
    {
        // Store the camera projections in a uniform buffer
        if (cameraProjectionList.isEmpty())
        {
            return null;
        }
        else
        {
            // Flatten the camera projection matrices into 16-component vectors and store them in the vertex list data structure.
            NativeVectorBuffer cameraProjectionData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 16, cameraProjectionList.size());

            for (int k = 0; k < cameraProjectionList.size(); k++)
            {
                int d = 0;
                for (int col = 0; col < 4; col++) // column
                {
                    for (int row = 0; row < 4; row++) // row
                    {
                        Matrix4 projection = cameraProjectionList.get(k).getProjectionMatrix(recommendedNearPlane, recommendedFarPlane);
                        cameraProjectionData.set(k, d, projection.get(row, col));
                        d++;
                    }
                }
            }
            return cameraProjectionData;
        }
    }

    @Override
    public ReadonlyNativeVectorBuffer getCameraProjectionIndexData()
    {
        // Store the camera projection indices in a uniform buffer
        if (cameraProjectionIndexList.isEmpty())
        {
            return null;
        }
        else
        {
            int[] indexArray = new int[cameraProjectionIndexList.size()];
            Arrays.setAll(indexArray, cameraProjectionIndexList::get);
            return NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, cameraProjectionIndexList.size(), indexArray);
        }
    }

    @Override
    public ReadonlyNativeVectorBuffer getLightPositionData()
    {
        // Store the light positions in a uniform buffer
        if (lightPositionList.isEmpty())
        {
            return null;
        }
        else
        {
            NativeVectorBuffer lightPositionData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 4, lightPositionList.size());
            for (int k = 0; k < lightPositionList.size(); k++)
            {
                lightPositionData.set(k, 0, lightPositionList.get(k).x);
                lightPositionData.set(k, 1, lightPositionList.get(k).y);
                lightPositionData.set(k, 2, lightPositionList.get(k).z);
                lightPositionData.set(k, 3, 1.0f);
            }

            return lightPositionData;
        }
    }

    @Override
    public ReadonlyNativeVectorBuffer getLightIntensityData()
    {
        // Store the light positions in a uniform buffer
        if (lightIntensityList.isEmpty())
        {
            return null;
        }
        else
        {
            NativeVectorBuffer lightIntensityData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 4, lightIntensityList.size());
            for (int k = 0; k < lightIntensityList.size(); k++)
            {
                lightIntensityData.set(k, 0, lightIntensityList.get(k).x);
                lightIntensityData.set(k, 1, lightIntensityList.get(k).y);
                lightIntensityData.set(k, 2, lightIntensityList.get(k).z);
                lightIntensityData.set(k, 3, 1.0f);
            }
            return lightIntensityData;
        }
    }

    @Override
    public ReadonlyNativeVectorBuffer getLightIndexData()
    {
        // Store the light indices in a uniform buffer
        if (lightIndexList.isEmpty())
        {
            return null;
        }
        else
        {
            int[] indexArray = new int[lightIndexList.size()];
            Arrays.setAll(indexArray, lightIndexList::get);
            return NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, lightIndexList.size(), indexArray);
        }
    }

    @Override
    public ReadonlyViewSet createPermutation(Collection<Integer> permutationIndices)
    {
        ViewSet result = new ViewSet(permutationIndices.size());

        for (int i : permutationIndices)
        {
            result.cameraPoseList.add(this.cameraPoseList.get(i));
            result.cameraPoseInvList.add(this.cameraPoseInvList.get(i));
            result.cameraProjectionIndexList.add(this.cameraProjectionIndexList.get(i));
            result.lightIndexList.add(this.lightIndexList.get(i));
            result.imageFiles.add(this.imageFiles.get(i));
            result.viewErrorMetrics.add(this.viewErrorMetrics.get(i));
        }

        result.cameraProjectionList.addAll(this.cameraProjectionList);
        result.lightIntensityList.addAll(this.lightIntensityList);
        result.lightPositionList.addAll(this.lightPositionList);

        if (this.linearLuminanceValues != null && this.encodedLuminanceValues != null)
        {
            result.setTonemapping(
                    Arrays.copyOf(this.linearLuminanceValues, this.linearLuminanceValues.length),
                    Arrays.copyOf(this.encodedLuminanceValues, this.encodedLuminanceValues.length));
        }

        result.rootDirectory = this.rootDirectory;
        result.fullResImageDirectory = this.fullResImageDirectory;
        result.previewImageDirectory = this.previewImageDirectory;
        result.supportingFilesDirectory = this.supportingFilesDirectory;
        result.geometryFile = this.geometryFile;
        result.infiniteLightSources = this.infiniteLightSources;
        result.recommendedNearPlane = this.recommendedNearPlane;
        result.recommendedFarPlane = this.recommendedFarPlane;
        result.primaryViewIndex = this.primaryViewIndex;
        result.orientationViewIndex = this.orientationViewIndex;
        result.orientationViewRotationDegrees = this.orientationViewRotationDegrees;

        return result;
    }

    @Override
    public ViewSet copy()
    {
        ViewSet result = new ViewSet(this.getCameraPoseCount());

        result.uuid = this.uuid;
        result.cameraPoseList.addAll(this.cameraPoseList);
        result.cameraPoseInvList.addAll(this.cameraPoseInvList);
        result.cameraProjectionList.addAll(this.cameraProjectionList);
        result.cameraProjectionIndexList.addAll(this.cameraProjectionIndexList);
        result.lightPositionList.addAll(this.lightPositionList);
        result.lightIntensityList.addAll(this.lightIntensityList);
        result.lightIndexList.addAll(this.lightIndexList);
        result.imageFiles.addAll(this.imageFiles);
        result.viewErrorMetrics.addAll(this.viewErrorMetrics);

        if (this.linearLuminanceValues != null && this.encodedLuminanceValues != null)
        {
            result.setTonemapping(
                    Arrays.copyOf(this.linearLuminanceValues, this.linearLuminanceValues.length),
                    Arrays.copyOf(this.encodedLuminanceValues, this.encodedLuminanceValues.length));
        }

        result.rootDirectory = this.rootDirectory;
        result.fullResImageDirectory = this.fullResImageDirectory;
        result.previewImageDirectory = this.previewImageDirectory;
        result.supportingFilesDirectory = this.supportingFilesDirectory;
        result.geometryFile = this.geometryFile;
        result.infiniteLightSources = this.infiniteLightSources;
        result.recommendedNearPlane = this.recommendedNearPlane;
        result.recommendedFarPlane = this.recommendedFarPlane;
        result.primaryViewIndex = this.primaryViewIndex;
        result.orientationViewIndex = this.orientationViewIndex;
        result.orientationViewRotationDegrees = this.orientationViewRotationDegrees;

        return result;
    }

    public static ReadonlyViewSet createFromLookAt(List<Vector3> viewDir, Vector3 center, Vector3 up, float distance,
                                                   float nearPlane, float aspect, float sensorWidth, float focalLength)
    {
        ViewSet result = new ViewSet(viewDir.size());

        result.cameraProjectionList.add(new DistortionProjection(sensorWidth, sensorWidth / aspect, focalLength));

        result.recommendedNearPlane = nearPlane;
        result.recommendedFarPlane = 2 * distance - nearPlane;

        result.lightIntensityList.add(new Vector3(distance * distance));
        result.lightPositionList.add(Vector3.ZERO);

        for (int i = 0; i < viewDir.size(); i++)
        {
            result.cameraProjectionIndexList.add(0);
            result.lightIndexList.add(0);
            result.imageFiles.add(new File(String.format("%04d.png", i + 1)));

            Matrix4 cameraPose = Matrix4.lookAt(viewDir.get(i).times(-distance).plus(center), center, up);

            result.cameraPoseList.add(cameraPose);
            result.cameraPoseInvList.add(cameraPose.quickInverse(0.001f));

            result.viewErrorMetrics.add(new ViewRMSE());
        }

        return result;
    }

    @Override
    public Matrix4 getCameraPose(int poseIndex)
    {
        return this.cameraPoseList.get(poseIndex);
    }

    @Override
    public Matrix4 getCameraPoseInverse(int poseIndex)
    {
        return this.cameraPoseInvList.get(poseIndex);
    }

    @Override
    public File getRootDirectory()
    {
        return this.rootDirectory;
    }

    /**
     * Sets the root directory for this view set, while leaving other file paths unmodified.
     * @param rootDirectory The root directory.
     */
    public void setRootDirectory(File rootDirectory)
    {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public String getGeometryFileName()
    {
        try
        {
            return this.rootDirectory.toPath().relativize(this.geometryFile.toPath()).toString();
        }
        catch (IllegalArgumentException | NullPointerException e) //If the root and other directories are located under different drive letters on windows
        {
            return geometryFile == null ? null : geometryFile.toString();
        }
    }

    @Override
    public File getGeometryFile()
    {
        return geometryFile;
    }

    /**
     * Sets the absolute path of the geometry file associated with this view set.
     * @param geometryFile The geometry file.
     */
    public void setGeometryFile(File geometryFile)
    {
        this.geometryFile = geometryFile;
    }

    @Override
    public File getSupportingFilesFilePath()
    {
        // Fallback to root directory if no supporting files defined
        return this.supportingFilesDirectory == null ? this.rootDirectory : this.supportingFilesDirectory;
    }

    @Override
    public String getRelativeSupportingFilesPathName()
    {
        File supportingFilesFilePath = this.getSupportingFilesFilePath();
        try
        {
            return this.rootDirectory.toPath().relativize(supportingFilesFilePath.toPath()).toString();
        }
        catch (IllegalArgumentException | NullPointerException e) //If the root and other directories are located under different drive letters on windows
        {
            return supportingFilesFilePath == null ? null : supportingFilesFilePath.toString();
        }
    }

    /**
     * Sets the absolute file path of the supporting files (i.e. texture fit results) associated with this view set.
     * @param supportingFilesDirectory The file path of the supporting files directory.
     */
    public void setSupportingFilesDirectory(File supportingFilesDirectory)
    {
        this.supportingFilesDirectory = supportingFilesDirectory;
    }

    @Override
    public File getFullResImageFilePath()
    {
        if (this.fullResImageDirectory == null)
        {
            // If no full res images, just use preview images as full res, or root directory as last fallback
            return this.previewImageDirectory == null ? this.rootDirectory : this.previewImageDirectory;
        }
        else
        {
            return this.fullResImageDirectory;
        }
    }


    /**
     * Sets the absolute image file directory associated with this view set.
     * @param absoluteImageDirectory The image file path.
     */
    public void setFullResImageDirectory(File absoluteImageDirectory)
    {
        this.fullResImageDirectory = absoluteImageDirectory;
    }

    @Override
    public String getRelativeFullResImagePathName()
    {
        File fullResImageFilePath = getFullResImageFilePath();

        try
        {
            return this.rootDirectory.toPath().relativize(fullResImageFilePath.toPath()).toString();
        }
        catch (IllegalArgumentException | NullPointerException e) //If the root and other directories are located under different drive letters on windows
        {
            return fullResImageFilePath == null ? null : fullResImageFilePath.toString();
        }
    }

    /**
     * Sets the image file path associated with this view set from a path relative to the root directory.
     * @param relativeImagePath The image file path.
     */
    public void setRelativeFullResImagePathName(String relativeImagePath)
    {
        this.fullResImageDirectory = this.rootDirectory.toPath().resolve(relativeImagePath).toFile();
    }

    @Override
    public File getPreviewImageFilePath()
    {
        if (this.previewImageDirectory == null)
        {
            // If no preview images, default to just using full res images, or root directory as last fallback
            return this.fullResImageDirectory == null ? this.rootDirectory : this.fullResImageDirectory;
        }
        else
        {
            return this.previewImageDirectory;
        }
    }

    @Override
    public String getRelativePreviewImagePathName()
    {
        File previewImageFilePath = this.getPreviewImageFilePath();

        try
        {
            return this.rootDirectory.toPath().relativize(previewImageFilePath.toPath()).toString();
        }
        catch (IllegalArgumentException | NullPointerException e) //If the root and other directories are located under different drive letters on windows
        {
            return previewImageFilePath == null ? null : previewImageFilePath.toString();
        }
    }

    /**
     * Sets the image file path associated with this view set.
     * @param relativeImagePath The image file path.
     */
    public void setRelativePreviewImagePathName(String relativeImagePath)
    {
        if (this.fullResImageDirectory == null)
        {
            // If we didn't have a full res directory, use the old preview directory as our full res directory
            this.fullResImageDirectory = previewImageDirectory;
        }

        this.previewImageDirectory = this.rootDirectory.toPath().resolve(relativeImagePath).toFile();
    }

    @Override
    public String getImageFileName(int poseIndex)
    {
        return this.imageFiles.get(poseIndex).getName();
    }

    @Override
    public File getFullResImageFile(int poseIndex)
    {
        return new File(this.getFullResImageFilePath(), this.imageFiles.get(poseIndex).getPath());
    }

    @Override
    public File getFullResImageFile(String viewName)
    {
        return getFullResImageFile(findIndexOfView(viewName));
    }

    @Override
    public File getPreviewImageFile(int poseIndex)
    {
        // Use PNG for preview images (TODO: make this a configurable setting?)
        return new File(this.getPreviewImageFilePath(),
            ImageFinder.getInstance().getImageFileNameWithFormat(this.getImageFileName(poseIndex), "png"));
    }

    @Override
    public File getMask(int poseIndex) {
        if (maskFiles.isEmpty()){
            return null;
        }
        return maskFiles.get(poseIndex);
    }

    public int getPreviewWidth()
    {
        return previewWidth;
    }

    public int getPreviewHeight()
    {
        return previewHeight;
    }

    public void setPreviewImageResolution(int width, int height)
    {
        this.previewWidth = width;
        this.previewHeight = height;
    }

    @Override
    public int getPrimaryViewIndex()
    {
        return this.primaryViewIndex;
    }

    @Override
    public int getOrientationViewIndex()
    {
        return this.orientationViewIndex;
    }

    @Override
    public double getOrientationViewRotationDegrees()
    {
        return this.orientationViewRotationDegrees;
    }

    public void setPrimaryViewIndex(int poseIndex)
    {
        this.primaryViewIndex = poseIndex;
    }

    public int findIndexOfView(String viewName)
    {
        int poseIndex = this.imageFiles.indexOf(new File(viewName));
        if (poseIndex >= 0)
        {
            return poseIndex;
        }
        else{
            //comb through manually because imageFiles could contain parent files
            //ex. target file is photo314.jpg and imageFiles contains myPhotos/photo314.jpg

            //another possibility is an extension mismatch
            //sometimes the camera label is photo314.jpg, other times just photo314

            //this is due to inconsistencies with camera labels in frame.zip and chunk.zip xml's
            for (int i = 0; i < imageFiles.size(); ++i){
                String shortenedImgName = removeExt(getImageFileName(i));
                String shortenedViewName = removeExt(viewName);

                if (shortenedImgName.equals(shortenedViewName)){
                    return i;
                }
            }
        }

        return -1;
    }

    public void setPrimaryView(String viewName)
    {
        int viewIndex = findIndexOfView(viewName);
        if (viewIndex >= 0)
        {
            this.primaryViewIndex = viewIndex;
        }
    }

    /**
     * Set the index of the view to use as a reference pose to reorient the model
     * @param newOrientationViewIndex view index
     */
    public void setOrientationViewIndex(int newOrientationViewIndex)
    {
        this.orientationViewIndex = newOrientationViewIndex;
    }

    public void setOrientationView(String viewName)
    {
        int viewIndex = findIndexOfView(viewName);
        if (viewIndex >= 0)
        {
            this.orientationViewIndex = viewIndex;
        }
        else
        {
            this.orientationViewIndex = -1;
        }
    }

    public static String removeExt(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    @Override
    public Projection getCameraProjection(int projectionIndex)
    {
        return this.cameraProjectionList.get(projectionIndex);
    }

    @Override
    public int getCameraProjectionIndex(int poseIndex)
    {
        return this.cameraProjectionIndexList.get(poseIndex);
    }

    @Override
    public Vector3 getLightPosition(int lightIndex)
    {
        return this.lightPositionList.get(lightIndex);
    }

    @Override
    public Vector3 getLightIntensity(int lightIndex)
    {
        return this.lightIntensityList.get(lightIndex);
    }

    public void setLightPosition(int lightIndex, Vector3 lightPosition)
    {
        this.lightPositionList.set(lightIndex, lightPosition);
    }

    public void setLightIntensity(int lightIndex, Vector3 lightIntensity)
    {
        this.lightIntensityList.set(lightIndex, lightIntensity);
    }

    @Override
    public int getLightIndex(int poseIndex)
    {
        return this.lightIndexList.get(poseIndex);
    }

    @Override
    public ViewRMSE getViewErrorMetrics(int poseIndex)
    {
        return this.viewErrorMetrics.get(poseIndex);
    }

    @Override
    public int getCameraPoseCount()
    {
        return this.cameraPoseList.size();
    }

    @Override
    public int getCameraProjectionCount()
    {
        return this.cameraProjectionList.size();
    }

    @Override
    public int getLightCount()
    {
        return this.lightPositionList.size();
    }

    @Override
    public float getRecommendedNearPlane()
    {
        return this.recommendedNearPlane;
    }

    @Override
    public float getRecommendedFarPlane()
    {
        return this.recommendedFarPlane;
    }

    @Override
    public boolean hasCustomLuminanceEncoding()
    {
        return linearLuminanceValues != null && encodedLuminanceValues != null
                && linearLuminanceValues.length > 0 && encodedLuminanceValues.length > 0;
    }

    @Override
    public SampledLuminanceEncoding getLuminanceEncoding()
    {
        if (hasCustomLuminanceEncoding())
        {
            return new SampledLuminanceEncoding(linearLuminanceValues, encodedLuminanceValues);
        }
        else
        {
            return new SampledLuminanceEncoding();
        }
    }

    @Override
    public double[] getLinearLuminanceValues()
    {
        return Arrays.copyOf(this.linearLuminanceValues, this.linearLuminanceValues.length);
    }

    @Override
    public byte[] getEncodedLuminanceValues()
    {
        return Arrays.copyOf(this.encodedLuminanceValues, this.encodedLuminanceValues.length);
    }

    public void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        this.linearLuminanceValues = linearLuminanceValues.clone();
        this.encodedLuminanceValues = encodedLuminanceValues.clone();
    }

    public void clearTonemapping()
    {
        this.linearLuminanceValues = null;
        this.encodedLuminanceValues = null;
    }

    @Override
    public boolean areLightSourcesInfinite()
    {
        return infiniteLightSources;
    }

    public void setInfiniteLightSources(boolean infiniteLightSources)
    {
        this.infiniteLightSources = infiniteLightSources;
    }

    @Override
    public File findFullResImageFile(int index) throws FileNotFoundException
    {
        return ImageFinder.getInstance().findImageFile(getFullResImageFile(index));
    }

    @Override
    public File findFullResPrimaryImageFile() throws FileNotFoundException
    {
        return findFullResImageFile(primaryViewIndex);
    }

    @Override
    public File findPreviewImageFile(int index) throws FileNotFoundException
    {
        return ImageFinder.getInstance().findImageFile(getPreviewImageFile(index));
    }

    @Override
    public File findPreviewPrimaryImageFile() throws FileNotFoundException
    {
        return findPreviewImageFile(primaryViewIndex);
    }

    @Override
    public UUID getUUID()
    {
        return uuid;
    }

    public void setUuid(UUID uuid)
    {
        this.uuid = uuid;
    }

    public void setOrientationViewRotationDegrees(double rotation)
    {
        orientationViewRotationDegrees = rotation;
    }

    public void setMasksDirectory(File dir){
        masksDirectory = dir;
    }

    public void addMasks(List<File> masks) {
        maskFiles.addAll(masks);
    }
}