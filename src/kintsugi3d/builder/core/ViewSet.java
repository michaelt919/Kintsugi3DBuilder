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

package kintsugi3d.builder.core;

import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.core.metrics.ViewRMSE;
import kintsugi3d.builder.state.settings.DefaultSettings;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import kintsugi3d.builder.state.settings.ReadonlyGeneralSettingsModel;
import kintsugi3d.builder.state.settings.SimpleGeneralSettingsModel;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.gl.vecmath.Matrix3;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;
import kintsugi3d.util.ImageFinder;
import kintsugi3d.util.UnzipHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class representing a collection of photographs, or views.
 *
 * @author Michael Tetzlaff
 */
public final class ViewSet implements ReadonlyViewSet, ObservableViewSet
{
    private static final Logger LOG = LoggerFactory.getLogger(ViewSet.class);

    private List<ViewSetObserver> observers = new ArrayList<>();

    /**
     * A unique id given to each view set that can be used to prevent cache collisions on disk.
     */
    private UUID uuid = UUID.randomUUID();

    private final ViewSetDataCollection viewSetDataCollection;

    private final ViewSetDataCollection disabledViewSetDataCollection;

    /**
     * A list of projection transformations defining the intrinsic properties of each camera.
     * This list can be much smaller than the number of views if the same intrinsic properties apply for multiple views.
     */
    private final List<Projection> cameraProjectionList;

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
     * The directory where thumbnail images are stored
     */
    private File thumbnailImageDirectory;

    /**
     * The directory where the masks are stored, if any are present (null if no masks)
     */
    private File masksDirectory;
    /**
     * The directory where the original model and imported textures (if any) are stored
     */
    private File modelDirectory;

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

    /**
     * Orientation imported, to be applied to the model
     */
    private Matrix3 orientationMatrix = null;

    /**
     * Object translation imported, to be applied to the model
     */
    private Vector3 objectTranslation = null;

    /**
     * Object scale imported, to be applied to the model
     */
    private float objectScale = 1.0f;

    private int previewWidth = 0;
    private int previewHeight = 0;

    private final GeneralSettingsModel projectSettings = new SimpleGeneralSettingsModel();
    private final Map<String, File> resourceMap = new HashMap<>(32);

    private boolean hasUnsupportedCorrections = false;


    /**
     *
     * @return true if the camera file being loaded contains correction flag, false otherwise
     */
    public boolean hasUnsupportedCorrections()
    {
        return this.hasUnsupportedCorrections;
    }

    /**
     * Set whether the camera file being loaded contains correction flag that is currently unsupported.
     * @param hasUnsupportedCorrections true if the camera file contains correction flag, false otherwise
     * @return ViewSet.Builder instance
     */
    public void setHasUnsupportedCorrections(boolean hasUnsupportedCorrections)
    {
        this.hasUnsupportedCorrections = hasUnsupportedCorrections;
    }

    @Override
    public Matrix3 getOrientationMatrix()
    {
        return orientationMatrix;
    }

    public void setOrientationMatrix(Matrix3 orientationMatrix)
    {
        this.orientationMatrix = orientationMatrix;
    }

    @Override
    public Vector3 getObjectTranslation()
    {
        return objectTranslation;
    }

    public void setObjectTranslation(Vector3 objectTranslation)
    {
        this.objectTranslation = objectTranslation;
    }

    @Override
    public float getObjectScale()
    {
        return objectScale;
    }

    public void setObjectScale(float objectScale)
    {
        this.objectScale = objectScale;
    }

    public static final class Builder
    {
        private final ViewSet result;
        private boolean needsClipPlanes = true;

        private Matrix4 cameraPose = null;
        private int cameraProjectionIndex = 0;
        private int lightIndex = 0;
        private File imageFile;
        private File maskFile;
        private boolean hasUnsupportedCorrections;

        /**
         * Uses root directory as supporting files directory by default
         *
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

            // Initialize settings with defaults.
            DefaultSettings.applyProjectDefaults(result.projectSettings);
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

        public Builder setCurrentMaskFile(File maskFile)
        {
            this.maskFile = maskFile;
            return this;
        }

        public Builder commitCurrentCameraPose()
        {
            ViewSetData currentCamera = new ViewSetData(cameraPose, cameraPose.quickInverse(0.002f),
                cameraProjectionIndex, lightIndex, result.viewSetDataCollection.getViewSetData().size(), imageFile, maskFile, new ViewRMSE());
            result.viewSetDataCollection.getViewSetData().add(currentCamera);
            return this;
        }

        public Builder commitCurrentCameraPoseAsDisabled()
        {
            ViewSetData currentCamera = new ViewSetData(cameraPose, cameraPose.quickInverse(0.002f), cameraProjectionIndex,
                lightIndex, result.viewSetDataCollection.getViewSetData().size() + result.disabledViewSetDataCollection.getViewSetData().size(),
                imageFile, maskFile, new ViewRMSE());
            currentCamera.isDisabled = true;
            result.disabledViewSetDataCollection.getViewSetData().add(currentCamera);
            return this;
        }

        public Builder disableCamerasByImageFilename(Iterable<File> disabledImageFiles)
        {
            for (File f : disabledImageFiles)
            {
                int index = -1;
                for (int i = 0; i < result.viewSetDataCollection.getViewSetData().size(); ++i)
                {
                    if (f.equals(result.viewSetDataCollection.getViewSetData().get(i).imageFile))
                    {
                        index = i;
                        break;
                    }
                }

                result.viewSetDataCollection.getViewSetData().get(index).isDisabled = true;
                result.disabledViewSetDataCollection.getViewSetData().add(result.viewSetDataCollection.getViewSetData().get(index));
                result.viewSetDataCollection.getViewSetData().remove(index);
            }

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

        public Builder setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
        {
            result.setLuminanceEncoding(linearLuminanceValues, encodedLuminanceValues);
            return this;
        }

        /**
         * Sets the geometry file associated with this view set.
         *
         * @param geometryFile The geometry file.
         */
        public Builder setGeometryFile(File geometryFile)
        {
            result.geometryFile = geometryFile;
            return this;
        }

        /**
         * Sets the name of the geometry file associated with this view set relative to the root directory.
         *
         * @param geometryFileName The name of the geometry file.
         */
        public Builder setGeometryFileName(String geometryFileName)
        {
            result.geometryFile = geometryFileName == null ? null : result.rootDirectory
                .toPath().resolve(geometryFileName).toFile();
            return this;
        }

        /**
         * Sets the full res image directory associated with this view set.
         *
         * @param fullResImageDirectory The full res image directory.
         */
        public Builder setFullResImageDirectory(File fullResImageDirectory)
        {
            result.setFullResImageDirectory(fullResImageDirectory);
            return this;
        }

        /**
         * Sets the name of the full res image directory associated with this view set relative to the root directory.
         *
         * @param relativePath The path to the full res images.
         */
        public Builder setRelativeFullResImagePathName(String relativePath)
        {
            result.setRelativeFullResImagePathName(relativePath);
            return this;
        }

        /**
         * Sets the relative file path of the supporting files (i.e. texture fit results) associated with this view set.
         *
         * @param relativePath The file path of the supporting files directory.
         */
        public Builder setRelativeSupportingFilesPathName(String relativePath)
        {
            result.supportingFilesDirectory = result.fullResImageDirectory.toPath().resolve(relativePath).toFile();
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

        public Builder setOrientationViewName(String viewName)
        {
            result.setOrientationView(viewName);
            return this;
        }

        public Builder setOrientationViewRotation(double rotation)
        {
            result.setOrientationViewRotationDegrees(rotation);
            return this;
        }

        public Builder setOrientationMatrix(Matrix3 matrix)
        {
            result.setOrientationMatrix(matrix);
            return this;
        }

        public Builder setObjectTranslation(Vector3 objectTranslation)
        {
            result.setObjectTranslation(objectTranslation);
            return this;
        }

        public Builder setObjectScale(float objectScale)
        {
            result.setObjectScale(objectScale);
            return this;
        }

        public Builder setMasksDirectory(File file)
        {
            result.setMasksDirectory(file);
            return this;
        }

        public Builder addMask(int camId, String imgFilename)
        {
            result.addMask(camId, new File(imgFilename));
            return this;
        }

        public Builder applySettings(ReadonlyGeneralSettingsModel settings)
        {
            result.getProjectSettings().copyFrom(settings);
            return this;
        }

        public Builder addResourceFiles(Map<String, File> resourceMap)
        {
            result.getResourceMap().putAll(resourceMap);
            return this;
        }

        public Builder setHasUnsupportedCorrections(boolean hasUnsupportedCorrections) {
            this.hasUnsupportedCorrections = hasUnsupportedCorrections;
            return this;
        }

        public ViewSet finish()
        {
            if (needsClipPlanes)
            {
                result.recommendedFarPlane = findFarPlane(result.viewSetDataCollection.getViewSetData());
                result.recommendedNearPlane = result.getRecommendedFarPlane() / 32.0f;
                LOG.debug("Near and far planes: {}, {}", result.getRecommendedNearPlane(), result.getRecommendedFarPlane());
            }

            // Fill with default lights if not specified
            int maxLightIndex = result.viewSetDataCollection.getViewSetData().stream().mapToInt(data->data.lightIndex).max().orElse(1);
            for (int i = getNextLightIndex(); i <= maxLightIndex; i = getNextLightIndex())
            {
                result.lightPositionList.add(Vector3.ZERO);
                result.lightIntensityList.add(Vector3.ZERO);
            }

            if (result.geometryFile == null && result.rootDirectory != null)
            {
                setGeometryFileName("manifold.obj"); // Used by some really old datasets
            }

            if (result.getSupportingFilesDirectory() != null)
            {
                // Make sure the supporting files directory exists
                result.getSupportingFilesDirectory().mkdirs();
            }

            result.hasUnsupportedCorrections = this.hasUnsupportedCorrections;

            return result;
        }

        /**
         * A subroutine for guessing an appropriate far plane from an Agisoft PhotoScan/Metashape XML file.
         * Assumes that the object must lie between all of the cameras in the file.
         *
         * @param viewSetDataList The list of camera data.
         * @return A far plane estimate.
         */
        private static float findFarPlane(Iterable<ViewSetData> viewSetDataList)
        {
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;

            for (ViewSetData aviewSetData: viewSetDataList)
            {
                Vector4 position = aviewSetData.cameraPoseInv.getColumn(3);
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
     *
     * @param initialCapacity The capacity to use for initializing array-based lists that scale with the number of views
     */
    public ViewSet(int initialCapacity)
    {
//        viewSetDataList = new ArrayList<>(initialCapacity);
//        disabledViewSets = new ArrayList<>(initialCapacity);

        viewSetDataCollection = new ViewSetDataCollection(initialCapacity, this);
        disabledViewSetDataCollection = new ViewSetDataCollection(initialCapacity, this);

        // Often these lists will have just one element
        this.cameraProjectionList = new ArrayList<>(1);
        this.lightIntensityList = new ArrayList<>(1);
        this.lightPositionList = new ArrayList<>(1);
    }

    @Override
    public List<File> getAllImageFiles()
    {
        return Stream.concat(viewSetDataCollection.getImageFiles().stream(), disabledViewSetDataCollection.getImageFiles().stream()).collect(Collectors.toList());
    }

    @Override
    public List<File> getEnabledImageFiles()
    {
        return viewSetDataCollection.getImageFiles();
    }

    @Override
    public ReadonlyNativeVectorBuffer getCameraPoseData()
    {
        // Store the poses in a uniform buffer
        if (getAllViewSetData().isEmpty())
        {
            return null;
        }
        else
        {
            // Flatten the camera pose matrices into 16-component vectors and store them in the vertex list data structure.
            NativeVectorBuffer cameraPoseData = NativeVectorBufferFactory.getInstance().createEmpty(
                NativeDataType.FLOAT, 16, getAllViewSetData().size());


            for (int k = 0; k < getAllViewSetData().size(); k++)
            {
                int d = 0;
                for (int col = 0; col < 4; col++) // column
                {
                    for (int row = 0; row < 4; row++) // row
                    {
                        cameraPoseData.set(k, d, getAllViewSetData().get(k).cameraPose.get(row, col));
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
        if (getAllViewSetData().isEmpty())
        {
            return null;
        }
        else
        {
            int[] indexArray = new int[getAllViewSetData().size()];
            Arrays.setAll(indexArray, i->getAllViewSetData().get(i).cameraProjectionIndex);
            return NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, getAllViewSetData().size(), indexArray);
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
        if (getAllViewSetData().isEmpty())
        {
            return null;
        }
        else
        {
            int[] indexArray = new int[getAllViewSetData().size()];
            Arrays.setAll(indexArray, i->getAllViewSetData().get(i).lightIndex);
            return NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, getAllViewSetData().size(), indexArray);
        }
    }

    public ReadonlyNativeVectorBuffer getViewIndexData()
    {
        // Store the view indices in a uniform buffer
        if (viewSetDataCollection.getViewSetData().isEmpty())
        {
            return null;
        }
        else
        {
            int[] indexArray = new int[viewSetDataCollection.getViewSetData().size()];
            Arrays.setAll(indexArray, i->viewSetDataCollection.getViewSetData().get(i).viewIndex);
            return NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, viewSetDataCollection.getViewSetData().size(), indexArray);
        }
    }

    @Override
    public ReadonlyViewSet createPermutation(Collection<Integer> permutationIndices)
    {
        ViewSet result = new ViewSet(permutationIndices.size());

        for (int i : permutationIndices)
        {
            result.viewSetDataCollection.getViewSetData().add(this.viewSetDataCollection.getViewSetData().get(i));
        }

        result.cameraProjectionList.addAll(this.cameraProjectionList);
        result.lightIntensityList.addAll(this.lightIntensityList);
        result.lightPositionList.addAll(this.lightPositionList);

        if (this.linearLuminanceValues != null && this.encodedLuminanceValues != null)
        {
            result.setLuminanceEncoding(
                Arrays.copyOf(this.linearLuminanceValues, this.linearLuminanceValues.length),
                Arrays.copyOf(this.encodedLuminanceValues, this.encodedLuminanceValues.length));
        }

        result.rootDirectory = this.rootDirectory;
        result.fullResImageDirectory = this.fullResImageDirectory;
        result.previewImageDirectory = this.previewImageDirectory;
        result.supportingFilesDirectory = this.supportingFilesDirectory;
        result.thumbnailImageDirectory = this.thumbnailImageDirectory;
        result.masksDirectory = this.masksDirectory;
        result.modelDirectory = this.modelDirectory;
        result.geometryFile = this.geometryFile;
        result.infiniteLightSources = this.infiniteLightSources;
        result.recommendedNearPlane = this.recommendedNearPlane;
        result.recommendedFarPlane = this.recommendedFarPlane;
        result.primaryViewIndex = this.primaryViewIndex;
        result.orientationViewIndex = this.orientationViewIndex;
        result.orientationViewRotationDegrees = this.orientationViewRotationDegrees;
        result.orientationMatrix = this.orientationMatrix;
        result.objectTranslation = this.objectTranslation;
        result.objectScale = this.objectScale;

        result.projectSettings.copyFrom(this.projectSettings);
        result.resourceMap.putAll(this.resourceMap);

        return result;
    }

    @Override
    public ViewSet copy()
    {
        ViewSet result = new ViewSet(this.getCombinedCameraPoseCount());

        result.uuid = this.uuid;
        result.viewSetDataCollection.getViewSetData().addAll(this.viewSetDataCollection.getViewSetData());
        result.disabledViewSetDataCollection.getViewSetData().addAll(this.disabledViewSetDataCollection.getViewSetData());
        result.cameraProjectionList.addAll(this.cameraProjectionList);
        result.lightPositionList.addAll(this.lightPositionList);
        result.lightIntensityList.addAll(this.lightIntensityList);

        if (this.linearLuminanceValues != null && this.encodedLuminanceValues != null)
        {
            result.setLuminanceEncoding(
                Arrays.copyOf(this.linearLuminanceValues, this.linearLuminanceValues.length),
                Arrays.copyOf(this.encodedLuminanceValues, this.encodedLuminanceValues.length));
        }

        result.rootDirectory = this.rootDirectory;
        result.fullResImageDirectory = this.fullResImageDirectory;
        result.previewImageDirectory = this.previewImageDirectory;
        result.supportingFilesDirectory = this.supportingFilesDirectory;
        result.thumbnailImageDirectory = this.thumbnailImageDirectory;
        result.masksDirectory = this.masksDirectory;
        result.modelDirectory = this.modelDirectory;
        result.geometryFile = this.geometryFile;
        result.infiniteLightSources = this.infiniteLightSources;
        result.recommendedNearPlane = this.recommendedNearPlane;
        result.recommendedFarPlane = this.recommendedFarPlane;
        result.primaryViewIndex = this.primaryViewIndex;
        result.orientationViewIndex = this.orientationViewIndex;
        result.orientationViewRotationDegrees = this.orientationViewRotationDegrees;
        result.orientationMatrix = this.orientationMatrix;
        result.objectTranslation = this.objectTranslation;
        result.objectScale = this.objectScale;

        result.previewWidth = this.previewWidth;
        result.previewHeight = this.previewHeight;

        result.projectSettings.copyFrom(this.projectSettings);
        result.resourceMap.putAll(this.resourceMap);

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
            File imageFile = new File(String.format("%04d.png", i + 1));
            Matrix4 cameraPose = Matrix4.lookAt(viewDir.get(i).times(-distance).plus(center), center, up);
            Matrix4 cameraPoseInv = cameraPose.quickInverse(0.001f);

            ViewSetData currentViewSetData = new ViewSetData(cameraPose, cameraPoseInv, 0, 0,
                    i, imageFile, null, new ViewRMSE());
        }

        return result;
    }

    @Override
    public UUID getUUID()
    {
        return uuid;
    }

    private int findViewSetIndex(List<ViewSetData> list, File image)
    {
        File imagePath = new File(removeExt(image.getAbsolutePath()));
        for (int i = 0; i < list.size(); ++i)
        {
            File f = new File(getFullResImageDirectory(), removeExt(list.get(i).imageFile.getPath()));
            if (imagePath.equals(f))
            {
                return i;
            }
        }
        return -1;
    }


    public void deleteCamera(File image)
    {
        int index = findViewSetIndex(viewSetDataCollection.getViewSetData(), image);
        if (index != -1)
        {
            viewSetDataCollection.getViewSetData().remove(index);
            notifyObservers();
        }
        else
        {
            index = findViewSetIndex(disabledViewSetDataCollection.getViewSetData(), image);
            if (index != -1)
            {
                disabledViewSetDataCollection.getViewSetData().remove(index);
                notifyObservers();
            }
        }
    }

    public void toggleCamera(File image)
    {
        int index = findViewSetIndex(disabledViewSetDataCollection.getViewSetData(), image);
        // Currently enabled, so disable camera
        if (index == -1)
        {
            index = findViewSetIndex(viewSetDataCollection.getViewSetData(), image);
            if (index != -1)
            {
                viewSetDataCollection.getViewSetData().get(index).isDisabled = true;
                disabledViewSetDataCollection.getViewSetData().add(viewSetDataCollection.getViewSetData().get(index));
                viewSetDataCollection.getViewSetData().remove(index);
            }

        }
        // Currently disabled, so enable camera
        else
        {
            disabledViewSetDataCollection.getViewSetData().get(index).isDisabled = false;
            viewSetDataCollection.getViewSetData().add(disabledViewSetDataCollection.getViewSetData().get(index));
            disabledViewSetDataCollection.getViewSetData().remove(index);
        }
        // Notify regardless of enable or disable
        notifyObservers();
    }

    @Override
    public Matrix4 getCameraPose(int poseIndex)
    {
        return this.getAllViewSetData().get(poseIndex).cameraPose;
    }

    @Override
    public Matrix4 getCameraPoseInverse(int poseIndex)
    {
        return this.getAllViewSetData().get(poseIndex).cameraPoseInv;
    }

    @Override
    public File getRootDirectory()
    {
        return this.rootDirectory;
    }

    /**
     * Sets the root directory for this view set, while leaving other file paths unmodified.
     *
     * @param rootDirectory The root directory.
     */
    public void setRootDirectory(File rootDirectory)
    {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public File getSupportingFilesDirectory()
    {
        // Fallback to root directory if no supporting files defined
        return this.supportingFilesDirectory == null ? this.rootDirectory : this.supportingFilesDirectory;
    }

    @Override
    public String getRelativeSupportingFilesPathName()
    {
        File effectiveSupportingFilesDirectory = this.getSupportingFilesDirectory();
        try
        {
            return this.rootDirectory.toPath().relativize(effectiveSupportingFilesDirectory.toPath()).toString();
        }
        catch (IllegalArgumentException |
            NullPointerException e) //If the root and other directories are located under different drive letters on windows
        {
            return effectiveSupportingFilesDirectory == null ? null : effectiveSupportingFilesDirectory.toString();
        }
    }

    /**
     * Sets the absolute file path of the supporting files (i.e. texture fit results) associated with this view set.
     *
     * @param supportingFilesDirectory The file path of the supporting files directory.
     */
    public void setSupportingFilesDirectory(File supportingFilesDirectory)
    {
        this.supportingFilesDirectory = supportingFilesDirectory;
        setRelativeThumbnailImagePathName(new File(supportingFilesDirectory, "thumbnails").toString());
    }

    @Override
    public File getFullResImageDirectory()
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
     *
     * @param absoluteImageDirectory The image file path.
     */
    public void setFullResImageDirectory(File absoluteImageDirectory)
    {
        this.fullResImageDirectory = absoluteImageDirectory;
    }

    @Override
    public String getRelativeFullResImagePathName()
    {
        File effectiveFullResImageDirectory = getFullResImageDirectory();

        try
        {
            return this.rootDirectory.toPath().relativize(effectiveFullResImageDirectory.toPath()).toString();
        }
        catch (IllegalArgumentException |
            NullPointerException e) //If the root and other directories are located under different drive letters on windows
        {
            return effectiveFullResImageDirectory == null ? null : effectiveFullResImageDirectory.toString();
        }
    }

    /**
     * Sets the image file path associated with this view set from a path relative to the root directory.
     *
     * @param relativeImagePath The image file path.
     */
    public void setRelativeFullResImagePathName(String relativeImagePath)
    {
        this.fullResImageDirectory = this.rootDirectory.toPath().resolve(relativeImagePath).toFile();
    }

    @Override
    public File getPreviewImageDirectory()
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
    public File getThumbnailImageDirectory()
    {
        if (this.thumbnailImageDirectory == null)
        {
            // If no thumbnail images, default to just using full res images, or root directory as last fallback
            return this.fullResImageDirectory == null ? this.rootDirectory : this.fullResImageDirectory;
        }
        else
        {
            return this.thumbnailImageDirectory;
        }
    }

    @Override
    public String getRelativePreviewImagePathName()
    {
        File effectivePreviewImageDirectory = this.getPreviewImageDirectory();

        try
        {
            return this.rootDirectory.toPath().relativize(effectivePreviewImageDirectory.toPath()).toString();
        }
        catch (IllegalArgumentException |
            NullPointerException e) //If the root and other directories are located under different drive letters on windows
        {
            return effectivePreviewImageDirectory == null ? null : effectivePreviewImageDirectory.toString();
        }
    }

    /**
     * Sets the image file path associated with this view set.
     *
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

    public void setRelativeThumbnailImagePathName(String relativeImagePath)
    {
        this.thumbnailImageDirectory = this.rootDirectory.toPath().resolve(relativeImagePath).toFile();
    }

    @Override
    public File getImageFile(int poseIndex)
    {
        return this.getAllViewSetData().get(poseIndex).imageFile;
    }

    @Override
    public File getEnabledImageFile(int poseIndex)
    {
        return this.viewSetDataCollection.getViewSetData().get(poseIndex).imageFile;
    }

    @Override
    public File getDisabledImageFile(int poseIndex)
    {
        return this.disabledViewSetDataCollection.getViewSetData().get(poseIndex).imageFile;
    }


    @Override
    public String getImageFileName(int poseIndex)
    {
        return this.getAllImageFiles().get(poseIndex).getName();
    }

    @Override
    public File getFullResImageFile(int poseIndex)
    {
        return new File(getFullResImageDirectory(), this.getAllViewSetData().get(poseIndex).imageFile.getPath());
//        return viewSetDataCollection.getFullResImageFile(poseIndex);
    }

    @Override
    public File getFullResImageFile(String viewName)
    {
        return getFullResImageFile(findIndexOfView(viewName));
    }

    @Override
    public File getPreviewImageFile(int poseIndex, String extension)
    {
        return new File(this.getPreviewImageDirectory(),
            ImageFinder.getInstance().getImageFileNameWithExtension(this.getImageFileName(poseIndex), extension));
    }

    @Override
    public File getPreviewImageFile(int poseIndex)
    {
        // Use PNG for preview images (TODO: make this a configurable setting?)
        return getPreviewImageFile(poseIndex, "png");
    }

    @Override
    public File getThumbnailImageFile(int poseIndex, String extension)
    {
        return viewSetDataCollection.getThumbnailImageFile(poseIndex, extension);
    }

    @Override
    public File getThumbnailImageFile(int poseIndex)
    {
        if (poseIndex < viewSetDataCollection.getViewSetData().size())
        {
            return viewSetDataCollection.getThumbnailImageFile(poseIndex);
        }
        return disabledViewSetDataCollection.getThumbnailImageFile(poseIndex - viewSetDataCollection.getViewSetData().size());
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

    public void setPrimaryViewIndex(int poseIndex)
    {
        this.primaryViewIndex = poseIndex;
    }

    @Override
    public int getOrientationViewIndex()
    {
        return this.orientationViewIndex;
    }

    /**
     * Set the index of the view to use as a reference pose to reorient the model
     *
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

    @Override
    public double getOrientationViewRotationDegrees()
    {
        return this.orientationViewRotationDegrees;
    }

    public void setOrientationViewRotationDegrees(double rotation)
    {
        orientationViewRotationDegrees = rotation;
    }

    public int findIndexOfView(String viewName)
    {
        // Treat null as "not found" or "not present"
        // Important for allowing for orientation pose to remain unset.
        if (viewName == null)
        {
            return -1;
        }

        int poseIndex = -1;
        File key = new File(viewName);
        for (int i = 0; i < viewSetDataCollection.getViewSetData().size(); ++i)
        {
            if (this.viewSetDataCollection.getViewSetData().get(i).imageFile.equals(key))
            {
                poseIndex = i;
                break;
            }
        }

        if (poseIndex >= 0)
        {
            return poseIndex;
        }
        else
        {
            //comb through manually because imageFiles could contain parent files
            //ex. target file is photo314.jpg and imageFiles contains myPhotos/photo314.jpg

            //another possibility is an extension mismatch
            //sometimes the camera label is photo314.jpg, other times just photo314

            //this is due to inconsistencies with camera labels in frame.zip and chunk.zip xml's
            for (int i = 0; i < viewSetDataCollection.getViewSetData().size(); ++i)
            {
                String imgName = getImageFileName(i);
                String shortenedImgName = removeExt(imgName);
                String shortenedViewName = removeExt(viewName);

                if (shortenedImgName.equals(shortenedViewName) || shortenedImgName.equals(viewName) || imgName.equals(shortenedViewName))
                {
                    return i;
                }
            }
        }

        return -1;
    }

    public static String removeExt(String fileName)
    {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    public ViewSetDataCollection getViewSetData() { return viewSetDataCollection; }

    public ViewSetDataCollection getDisabledViewSetData() { return disabledViewSetDataCollection; }

    public List<ViewSetData> getAllViewSetData()
    {
        return Stream.concat(viewSetDataCollection.getViewSetData().stream(), disabledViewSetDataCollection.getViewSetData().stream()).collect(Collectors.toList());
    }

    @Override
    public Projection getCameraProjection(int projectionIndex)
    {
        return this.cameraProjectionList.get(projectionIndex);
    }

    @Override
    public Projection getCameraProjectionForViewIndex(int viewIndex)
    {
        return getCameraProjection(getCameraProjectionIndex(viewIndex));
    }

    @Override
    public int getCameraProjectionIndex(int poseIndex)
    {
        return this.getAllViewSetData().get(poseIndex).cameraProjectionIndex;
    }

    @Override
    public int getEnabledCameraProjectionIndex(int poseIndex)
    {
        return this.viewSetDataCollection.getViewSetData().get(poseIndex).cameraProjectionIndex;
    }

    @Override
    public int getDisabledCameraProjectionIndex(int poseIndex)
    {
        return this.disabledViewSetDataCollection.getViewSetData().get(poseIndex).cameraProjectionIndex;
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
        return this.getAllViewSetData().get(poseIndex).lightIndex;
    }

    @Override
    public int getEnabledLightIndex(int poseIndex)
    {
        return this.viewSetDataCollection.getViewSetData().get(poseIndex).lightIndex;
    }

    @Override
    public int getDisabledLightIndex(int poseIndex)
    {
        return this.disabledViewSetDataCollection.getViewSetData().get(poseIndex).lightIndex;
    }

    @Override
    public ViewRMSE getViewErrorMetrics(int poseIndex)
    {
        return this.viewSetDataCollection.getViewSetData().get(poseIndex).viewErrorMetric;
    }

//    @Override
//    public int getCameraPoseCount()
//    {
//        return this.getAllViewSetData().size();
////        return this.viewSetDataCollection.getViewSetData().size();
//    }

    @Override
    public int getCombinedCameraPoseCount()
    {
        return this.getAllViewSetData().size();
    }

    @Override
    public int getEnabledCameraPoseCount()
    {
        return this.viewSetDataCollection.getViewSetData().size();
    }

    @Override
    public int getDisabledCameraPoseCount()
    {
        return this.disabledViewSetDataCollection.getViewSetData().size();
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

    public void setLuminanceEncoding(double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        if (linearLuminanceValues.length != encodedLuminanceValues.length)
        {
            throw new IllegalArgumentException("Arrays must be of equal length.");
        }

        this.linearLuminanceValues = linearLuminanceValues.clone();
        this.encodedLuminanceValues = encodedLuminanceValues.clone();
    }

    public void clearLuminanceEncoding()
    {
        this.linearLuminanceValues = null;
        this.encodedLuminanceValues = null;
    }

    @Override
    public File findFullResImageFile(int index) throws FileNotFoundException
    {
        return ImageFinder.getInstance().findImageFile(getFullResImageFile(index));
    }

    @Override
    public File tryFindFullResImageFile(int index)
    {
        return ImageFinder.getInstance().tryFindImageFile(getFullResImageFile(index));
    }

    @Override
    public File findFullResPrimaryImageFile() throws FileNotFoundException
    {
        return findFullResImageFile(primaryViewIndex);
    }

    @Override
    public File tryFindPreviewImageFile(int index)
    {
        return ImageFinder.getInstance().tryFindImageFile(getPreviewImageFile(index));
    }

    @Override
    public File tryFindThumbnailImageFile(int index)
    {
        return ImageFinder.getInstance().tryFindImageFile(getThumbnailImageFile(index));
    }

    @Override
    public File findPreviewImageFile(int index) throws FileNotFoundException
    {
        return ImageFinder.getInstance().findImageFile(getPreviewImageFile(index));
    }

    @Override
    public File findThumbnailImageFile(int index) throws FileNotFoundException
    {
        return viewSetDataCollection.findThumbnailImageFile(index);
    }



    @Override
    public File findPreviewPrimaryImageFile() throws FileNotFoundException
    {
        return findPreviewImageFile(primaryViewIndex);
    }

    @Override
    public boolean hasMasks()
    {
        return masksDirectory != null;
    }

    @Override
    public File getMask(int poseIndex)
    {
        File maskFile = getAllViewSetData().get(poseIndex).maskFile;
        if (maskFile == null || getMasksDirectory() == null)
        {
            // Not all images have masks, so this file may still not exist
            return null;
        }
        else
        {
            return new File(getMasksDirectory(), maskFile.getName());
        }
    }

    @Override
    public File getMasksDirectory()
    {
        return masksDirectory;
    }

    @Override
    public Map<Integer, File> getMasksMap()
    {
        HashMap<Integer, File>maskFiles = new HashMap<>(viewSetDataCollection.getViewSetData().size());
        for (int i = 0; i < viewSetDataCollection.getViewSetData().size(); ++i)
        {
            if (viewSetDataCollection.getViewSetData().get(i).maskFile != null)
            {
                maskFiles.put(i, viewSetDataCollection.getViewSetData().get(i).maskFile);
            }
        }
        return Collections.unmodifiableMap(maskFiles);
    }

    public void setMasksDirectory(File dir)
    {
        masksDirectory = dir;
    }

    public void addMask(int camId, File mask)
    {
        viewSetDataCollection.getViewSetData().get(camId).maskFile = mask;
    }

    @Override
    public ImageHelper loadFullResMaskedImage(int index) throws IOException
    {
        return ImageHelper.read(findFullResImageFile(index)).withAlphaMask(getMask(index));
    }

    /**
     * Checks that all mask files exist.  In doing so, it tries several variations (i.e. mask filename vs. photo filename,
     * _mask vs. no _mask, various file extensions) and changes the recorded mask filename for each view to an image that was found
     * (or eliminating the mask if the file is missing).
     */
    public void validateMasks()
    {
        for (int i = 0; i < getCombinedCameraPoseCount(); i++)
        {
            File maskFile = getMask(i);
            if (maskFile != null)
            {
                File originalMaskFile = maskFile; // remember the original filename for logging

                // Could set maskFile to null if it doesn't actually exist,
                // or change the file extension if it exists with a different file extension.
                maskFile = ImageFinder.getInstance().tryFindImageFile(maskFile);

                if (maskFile == null)
                {
                    LOG.warn("Specified mask file not found: {}", originalMaskFile.getPath());
                }
            }

            if (maskFile == null)
            {
                // Search for the name of the photo in the masks directory
                // Will check both with and without _mask suffix
                maskFile = ImageFinder.getInstance().tryFindImageFile(
                    new File(getMasksDirectory(), getFullResImageFile(i).getName()),
                    "_mask");
            }

            if (maskFile == null)
            {
                // Remove if no mask file was found
                getAllViewSetData().get(i).maskFile = null;
            }
            else
            {
                // Overwrite based on the file that was found
                getAllViewSetData().get(i).maskFile = maskFile;
            }
        }
    }

    /**
     * Checks for whether srcFile is null before copying into destDir.
     *
     * @param srcFile
     * @param destDir
     */
    private static void copyFileSafe(File srcFile, File destDir)
    {
        if (srcFile != null)
        {
            File destFile = new File(destDir, srcFile.getName());

            try
            {
                Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e)
            {
                LOG.error("Failed to copy {} to {}", srcFile.getName(), destDir.getPath());
            }
        }
    }

    /**
     * Copies masks to an appropriate supporting files directory and changes the masks directory accordingly.
     * If the masks were previously stored in a ZIP file, they will be unzipped to the new masks directory.
     * Masks will be validated (see validateMasks()) as a result of this operation, possibly changing the recorded mask file name
     * based on the mask files that are actually found (or eliminating masks if missing).
     */
    public void copyMasks()
    {
        if (masksDirectory == null)
        {
            return;
        }

        File masksSrcDir = masksDirectory;

        File masksDestinationDir = supportingFilesDirectory != null ?
            new File(supportingFilesDirectory, "masks") :
            new File(ApplicationFolders.getExtensionDirectory().resolve("kintsugi3d.builder.masks").toFile(), uuid.toString());

        masksDestinationDir.mkdirs();

        // Unzip masks if needed
        if (masksSrcDir.toString().endsWith(".zip"))
        {
            LOG.info("Unzipping masks folder...");
            try
            {
                // Just unzip everything for efficiency; could clean up any unused files (i.e. non-masks) but probably not necessary
                UnzipHelper.unzipToDirectory(masksSrcDir, masksDestinationDir, null);

                // Use the destination directory as the masks directory for validating (and thereafter)
                setMasksDirectory(masksDestinationDir);

                // Make sure the masks are there after unzipping (might change the mask filenames stored)
                validateMasks();
            }
            catch (IOException e)
            {
                LOG.error("Failed to unzip masks.", e);
            }
        }
        else
        {
            // Validate masks first to make sure we're copying the right files (might change the mask filenames stored)
            validateMasks();

            // Copy the files that were actually found
            for (int i = 0; i < getCombinedCameraPoseCount(); i++)
            {
                File maskSrcFile = getMask(i);
                copyFileSafe(maskSrcFile, masksDestinationDir);
            }

            // Use the destination directory as the masks directory to use from now on.
            setMasksDirectory(masksDestinationDir);
        }
    }

    @Override
    public String getGeometryFileName()
    {
        File effectiveModelDirectory = this.getModelDirectory();

        if (this.geometryFile != null && effectiveModelDirectory != null && !effectiveModelDirectory.toString().endsWith(".zip"))
        {
            try
            {
                return effectiveModelDirectory.toPath().relativize(this.geometryFile.toPath()).toString();
            }
            catch (IllegalArgumentException | NullPointerException e)
            {
                LOG.warn("Exception relativizing {} within {}", this.geometryFile, effectiveModelDirectory);
            }
        }

        // If directories are located under different drive letters on windows, or geometry file is null, or model directory is a ZIP
        return geometryFile == null ? null : geometryFile.toString();
    }

    @Override
    public File getGeometryFile()
    {
        return geometryFile;
    }

    /**
     * Sets the absolute path of the geometry file associated with this view set.
     *
     * @param geometryFile The geometry file.
     */
    public void setGeometryFile(File geometryFile)
    {
        this.geometryFile = geometryFile;
    }

    public File getModelDirectory()
    {
        return this.modelDirectory == null ? this.rootDirectory : this.modelDirectory;
    }

    public void setModelDirectory(File modelDirectory)
    {
        this.modelDirectory = modelDirectory;
    }

    /**
     * Copies model and textures to an appropriate supporting files directory and changes the model directory accordingly.
     * If the model and textures were previously stored in a ZIP file, they will be unzipped to the new model directory.
     */
    public void copyModel()
    {
        if (modelDirectory == null)
        {
            return;
        }

        File modelSrcDir = modelDirectory;

        File modelDestDir = supportingFilesDirectory != null ?
            new File(supportingFilesDirectory, "model") :
            new File(ApplicationFolders.getExtensionDirectory().resolve("kintsugi3d.builder.model").toFile(), uuid.toString());

        modelDestDir.mkdirs();

        // Unzip model and textures if needed
        if (modelSrcDir.toString().endsWith(".zip"))
        {
            LOG.info("Unzipping model folder...");
            try
            {
                // Just unzip everything for efficiency; could clean up any unused files but probably not necessary
                UnzipHelper.unzipToDirectory(modelSrcDir, modelDestDir, null);

                // Use the destination directory as the model directory for validating (and thereafter)
                setModelDirectory(modelDestDir);
            }
            catch (IOException e)
            {
                LOG.error("Failed to unzip model / textures.", e);
            }
        }
        else
        {
            copyFileSafe(getGeometryFile(), modelDestDir);

            for (var resource : resourceMap.entrySet())
            {
                if (resource.getKey().startsWith("texture."))
                {
                    copyFileSafe(resource.getValue(), modelDestDir);
                }
            }

            // Use the destination directory as the model directory to use from now on.
            setModelDirectory(modelDestDir);
        }
    }

    @Override
    public GeneralSettingsModel getProjectSettings()
    {
        return projectSettings;
    }

    @Override
    public Map<String, File> getResourceMap()
    {
        return resourceMap;
    }

    @Override
    public boolean getIsDisabled(int poseIndex)
    {
        return getAllViewSetData().get(poseIndex).isDisabled;
    }

    @Override
    public <ContextType extends Context<ContextType>> ProgramBuilder<ContextType> getShaderProgramBuilder(ContextType context)
    {
        // Determine shader defines here that should apply globally as defaults without require specific resources other than view set data.
        // The defines can be overridden by the actual shader.
        return context.getShaderProgramBuilder()
            .define("CAMERA_POSE_COUNT", getCombinedCameraPoseCount())
            .define("CAMERA_PROJECTION_COUNT", getCameraProjectionCount())
            .define("LIGHT_COUNT", getLightCount())
            .define("INFINITE_LIGHT_SOURCES", projectSettings.getBoolean("infiniteLightSources"))
            .define("FLATFIELD_CORRECTED", projectSettings.getBoolean("flatfieldCorrected"))
            .define("LUMINANCE_MAP_ENABLED", hasCustomLuminanceEncoding())
            .define("INVERSE_LUMINANCE_MAP_ENABLED", hasCustomLuminanceEncoding())
            .define("VISIBILITY_TEST_ENABLED", projectSettings.getBoolean("occlusionEnabled"))
            .define("SHADOW_TEST_ENABLED", projectSettings.getBoolean("occlusionEnabled"))
            .define("EDGE_PROXIMITY_WEIGHT_ENABLED", projectSettings.getBoolean("edgeProximityWeightEnabled"));
    }

    @Override
    public <ContextType extends Context<ContextType>> void setupShaderProgram(Program<ContextType> program)
    {
        // Determine shader uniforms here that should apply globally as defaults without require specific resources other than view set data.
        // The uniforms can be overridden by the actual shader.
        program.setUniform("occlusionBias", projectSettings.getFloat("occlusionBias"));
        program.setUniform("edgeProximityMargin", projectSettings.getFloat("edgeProximityMargin"));
        program.setUniform("edgeProximityCutoff", projectSettings.getFloat("edgeProximityCutoff"));
    }

    @Override
    public void registerObserver(ViewSetObserver observer)
    {
        observers.add(observer);
    }

    @Override
    public void removeObserver(ViewSetObserver observer)
    {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers()
    {
        for (ViewSetObserver observer : observers)
        {
            observer.update();
        }
    }
}