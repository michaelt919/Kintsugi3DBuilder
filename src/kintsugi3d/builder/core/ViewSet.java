/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.ImageFinder;

/**
 * A class representing a collection of photographs, or views.
 * @author Michael Tetzlaff
 */
@SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
public final class ViewSet implements ReadonlyViewSet
{
    private static final Logger log = LoggerFactory.getLogger(ViewSet.class);

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
     * A list containing the relative name of the image file corresponding to each view.
     */
    private final List<String> imageFileNames;

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
     * The relative file path to be used for loading images.
     */
    private String relativeFullResImagePathName;

    /**
     * The relative file path to be used for loading images.
     */
    private String relativePreviewImagePathName;

    /**
     * The relative name of the mesh file.
     */
    private String geometryFileName;

    /**
     * Used to decode pixel colors according to a gamma curve if reference values are unavailable, otherwise, affects the absolute brightness of the decoded colors.
     */
    private float gamma = 2.2f;

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
     * The index of the view that sets the initial orientation when viewing, is used for color calibration, etc.
     */
    private int primaryViewIndex = 0;
    private int previewWidth = 0;
    private int previewHeight = 0;

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
        this.imageFileNames = new ArrayList<>(initialCapacity);

        // Often these lists will have just one element
        this.cameraProjectionList = new ArrayList<>(1);
        this.lightIntensityList = new ArrayList<>(1);
        this.lightPositionList = new ArrayList<>(1);
    }

    public List<Matrix4> getCameraPoseList()
    {
        return cameraPoseList;
    }

    public List<Matrix4> getCameraPoseInvList()
    {
        return cameraPoseInvList;
    }

    public List<Projection> getCameraProjectionList()
    {
        return cameraProjectionList;
    }

    public List<Integer> getCameraProjectionIndexList()
    {
        return cameraProjectionIndexList;
    }

    public List<Vector3> getLightPositionList()
    {
        return lightPositionList;
    }

    public List<Vector3> getLightIntensityList()
    {
        return lightIntensityList;
    }

    public List<Integer> getLightIndexList()
    {
        return lightIndexList;
    }

    public List<String> getImageFileNames()
    {
        return imageFileNames;
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
            for (int k = 0; k < lightPositionList.size(); k++)
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
            result.imageFileNames.add(this.imageFileNames.get(i));
        }

        result.cameraProjectionList.addAll(this.cameraProjectionList);
        result.lightIntensityList.addAll(this.lightIntensityList);
        result.lightPositionList.addAll(this.lightPositionList);

        if (this.linearLuminanceValues != null && this.encodedLuminanceValues != null)
        {
            result.setTonemapping(this.gamma,
                Arrays.copyOf(this.linearLuminanceValues, this.linearLuminanceValues.length),
                Arrays.copyOf(this.encodedLuminanceValues, this.encodedLuminanceValues.length));
        }

        result.rootDirectory = this.rootDirectory;
        result.relativeFullResImagePathName = this.relativeFullResImagePathName;
        result.relativePreviewImagePathName = this.relativePreviewImagePathName;
        result.geometryFileName = this.geometryFileName;
        result.infiniteLightSources = this.infiniteLightSources;
        result.recommendedNearPlane = this.recommendedNearPlane;
        result.recommendedFarPlane = this.recommendedFarPlane;
        result.primaryViewIndex = primaryViewIndex;

        return result;
    }

    @Override
    public ViewSet copy()
    {
        ViewSet result = new ViewSet(this.getCameraPoseCount());

        result.cameraPoseList.addAll(this.cameraPoseList);
        result.cameraPoseInvList.addAll(this.cameraPoseInvList);
        result.cameraProjectionList.addAll(this.cameraProjectionList);
        result.cameraProjectionIndexList.addAll(this.cameraProjectionIndexList);
        result.lightPositionList.addAll(this.lightPositionList);
        result.lightIntensityList.addAll(this.lightIntensityList);
        result.lightIndexList.addAll(this.lightIndexList);
        result.imageFileNames.addAll(this.imageFileNames);

        if (this.linearLuminanceValues != null && this.encodedLuminanceValues != null)
        {
            result.setTonemapping(this.gamma,
                Arrays.copyOf(this.linearLuminanceValues, this.linearLuminanceValues.length),
                Arrays.copyOf(this.encodedLuminanceValues, this.encodedLuminanceValues.length));
        }

        result.rootDirectory = this.rootDirectory;
        result.relativeFullResImagePathName = this.relativeFullResImagePathName;
        result.relativePreviewImagePathName = this.relativePreviewImagePathName;
        result.geometryFileName = this.geometryFileName;
        result.infiniteLightSources = this.infiniteLightSources;
        result.recommendedNearPlane = this.recommendedNearPlane;
        result.recommendedFarPlane = this.recommendedFarPlane;
        result.primaryViewIndex = primaryViewIndex;

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
            result.imageFileNames.add(String.format("%04d.png", i + 1));

            Matrix4 cameraPose = Matrix4.lookAt(viewDir.get(i).times(-distance).plus(center), center, up);

            result.cameraPoseList.add(cameraPose);
            result.cameraPoseInvList.add(cameraPose.quickInverse(0.001f));
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

    /**
     * Changes the root directory while adjusting other file paths to still reference the original files.
     * @param newRootDirectory The new root directory.
     */
    public void moveRootDirectory(Path newRootDirectory)
    {
        //noinspection VariableNotUsedInsideIf
        if (this.rootDirectory != null)
        {
            if (this.getGeometryFile() != null)
            {
                this.geometryFileName = newRootDirectory.relativize(getGeometryFile().toPath()).toString();
            }

            if (this.getFullResImageFilePath() != null)
            {
                this.relativeFullResImagePathName = newRootDirectory.relativize(getFullResImageFilePath().toPath()).toString();
            }

            if (this.getPreviewImageFilePath() != null)
            {
                this.relativePreviewImagePathName = newRootDirectory.relativize(getPreviewImageFilePath().toPath()).toString();
            }
        }

        this.rootDirectory = newRootDirectory.toFile();
    }

    @Override
    public String getGeometryFileName()
    {
        return geometryFileName;
    }

    /**
     * Sets the name of the geometry file associated with this view set.
     * @param fileName The name of the geometry file.
     */
    public void setGeometryFileName(String fileName)
    {
        this.geometryFileName = fileName;
    }

    @Override
    public File getGeometryFile()
    {
        return geometryFileName == null ? null : new File(this.rootDirectory, geometryFileName);
    }

    @Override
    public File getFullResImageFilePath()
    {
        return this.relativeFullResImagePathName == null ? this.rootDirectory : new File(this.rootDirectory, relativeFullResImagePathName);
    }

    @Override
    public String getRelativeFullResImagePathName()
    {
        return this.relativeFullResImagePathName;
    }

    /**
     * Sets the image file path associated with this view set.
     * @param relativeImagePath The image file path.
     */
    public void setRelativeFullResImagePathName(String relativeImagePath)
    {
        this.relativeFullResImagePathName = relativeImagePath;
    }

    @Override
    public File getPreviewImageFilePath()
    {
        return this.relativePreviewImagePathName == null ? this.rootDirectory : new File(this.rootDirectory, relativePreviewImagePathName);
    }

    @Override
    public String getRelativePreviewImagePathName()
    {
        return this.relativePreviewImagePathName;
    }

    /**
     * Sets the image file path associated with this view set.
     * @param relativeImagePath The image file path.
     */
    public void setRelativePreviewImagePathName(String relativeImagePath)
    {
        this.relativePreviewImagePathName = relativeImagePath;
    }

    @Override
    public String getImageFileName(int poseIndex)
    {
        return this.imageFileNames.get(poseIndex);
    }

    @Override
    public String getImageFileNameWithFormat(int poseIndex, String format)
    {
        String[] parts = this.getImageFileName(poseIndex).split("\\.");
        return Stream.concat(Arrays.stream(parts, 0, Math.max(1, parts.length - 1)), Stream.of(format))
                .collect(Collectors.joining("."));
    }

    @Override
    public File getFullResImageFile(int poseIndex)
    {
        return new File(this.getFullResImageFilePath(), this.imageFileNames.get(poseIndex));
    }

    @Override
    public File getPreviewImageFile(int poseIndex)
    {
        // Use PNG for preview images (TODO: make this a configurable setting?)
        return new File(this.getPreviewImageFilePath(), this.getImageFileNameWithFormat(poseIndex, "PNG"));
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

    public void setPrimaryView(String viewName)
    {
        int poseIndex = this.imageFileNames.indexOf(viewName);
        if (poseIndex >= 0)
        {
            this.primaryViewIndex = poseIndex;
        }
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

    public void setRecommendedNearPlane(float recommendedNearPlane)
    {
        this.recommendedNearPlane = recommendedNearPlane;
    }

    @Override
    public float getRecommendedFarPlane()
    {
        return this.recommendedFarPlane;
    }

    public void setRecommendedFarPlane(float recommendedFarPlane)
    {
        this.recommendedFarPlane = recommendedFarPlane;
    }

    @Override
    public float getGamma()
    {
        return gamma;
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
            return new SampledLuminanceEncoding(linearLuminanceValues, encodedLuminanceValues, gamma);
        }
        else
        {
            return new SampledLuminanceEncoding(gamma);
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

    public void setTonemapping(float gamma, double[] linearLuminanceValues, byte[] encodedLuminanceValues)
    {
        this.gamma = gamma;
        this.linearLuminanceValues = linearLuminanceValues;
        this.encodedLuminanceValues = encodedLuminanceValues;
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
}
