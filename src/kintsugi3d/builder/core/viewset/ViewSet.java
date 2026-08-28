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

import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.core.Observable;
import kintsugi3d.builder.core.Observer;
import kintsugi3d.builder.core.metrics.ViewRMSE;
import kintsugi3d.builder.core.viewset.ViewSetChange.Type;
import kintsugi3d.builder.state.settings.GeneralSettingsModel;
import kintsugi3d.builder.state.settings.SimpleGeneralSettingsModel;
import kintsugi3d.gl.builders.ProgramBuilder;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.Program;
import kintsugi3d.gl.nativebuffer.NativeDataType;
import kintsugi3d.gl.nativebuffer.NativeVectorBuffer;
import kintsugi3d.gl.nativebuffer.NativeVectorBufferFactory;
import kintsugi3d.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import kintsugi3d.gl.vecmath.Matrix3;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.ImageFinder;
import kintsugi3d.util.UnzipHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * A class representing a collection of photographs, or views.
 *
 * @author Michael Tetzlaff
 */
public final class ViewSet implements ReadonlyViewSet, Observable
{
    private static final Logger LOG = LoggerFactory.getLogger(ViewSet.class);

    private final Collection<Observer<ViewSetChange>> observers = Collections.synchronizedList(new ArrayList<>(8));

    /**
     * A unique id given to each view set that can be used to prevent cache collisions on disk.
     */
    private UUID uuid = UUID.randomUUID();

    private final List<View> viewList;

    /**
     * A list of projection transformations defining the intrinsic properties of each camera.
     * This list can be much smaller than the number of views if the same intrinsic properties apply for multiple views.
     * This array may be added to but should never be removed from as indices are expected to be persistent.
     */
    private final List<Projection> cameraProjectionList;

    /**
     * A list of light source positions, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * Assumed by convention to be in camera space.
     * This list can be much smaller than the number of views if the same illumination conditions apply for multiple views.
     * This array may be added to but should never be removed from as indices are expected to be persistent.
     */
    private final List<Vector3> lightPositionList;

    /**
     * A list of light source intensities, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * This list can be much smaller than the number of views if the same illumination conditions apply for multiple views.
     * This array may be added to but should never be removed from as indices are expected to be persistent.
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
     * The view used for color calibration
     */
    private View primaryView;

    /**
     * The view used to reorient the model
     */
    private View orientationView;

    private int gpuBufferSize = 0;

    /**
     * Roll rotation of the orientation view, used to correct sideways or upside down images
     */
    private double orientationViewRotationDegrees = 0;

    /**
     * Orientation imported, to be applied to the model
     */
    private Matrix3 orientationMatrix;

    /**
     * Object translation imported, to be applied to the model
     */
    private Vector3 objectTranslation;

    /**
     * Object scale imported, to be applied to the model
     */
    private float objectScale = 1.0f;

    private int previewWidth = 0;
    private int previewHeight = 0;

    private final GeneralSettingsModel projectSettings = new SimpleGeneralSettingsModel();
    private final Map<String, File> resourceMap = new HashMap<>(32);

    private boolean hasUnsupportedCorrections = false;

    public static ViewSetBuilder getBuilder(File rootDirectory, int initialCapacity)
    {
        return new ViewSetBuilder(rootDirectory, initialCapacity);
    }

    public static ViewSetBuilder getBuilder(File rootDirectory, File supportingFilesDirectory, int initialCapacity)
    {
        return new ViewSetBuilder(rootDirectory, supportingFilesDirectory, initialCapacity);
    }

    /**
     * Creates a new view set object.
     *
     * @param initialCapacity The capacity to use for initializing array-based lists that scale with the number of views
     */
    public ViewSet(int initialCapacity)
    {
        viewList = Collections.synchronizedList(new ArrayList<>(initialCapacity));

        // Often these lists will have just one element
        this.cameraProjectionList = Collections.synchronizedList(new ArrayList<>(1));
        this.lightIntensityList = Collections.synchronizedList(new ArrayList<>(1));
        this.lightPositionList = Collections.synchronizedList(new ArrayList<>(1));
    }

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

    @Override
    public ReadonlyNativeVectorBuffer getCameraPoseData()
    {
        synchronized (viewList)
        {
            // Store the poses in a uniform buffer
            if (viewList.isEmpty())
            {
                return null;
            }
            else
            {
                // Flatten the camera pose matrices into 16-component vectors and store them in the vertex list data structure.
                NativeVectorBuffer cameraPoseData = NativeVectorBufferFactory.getInstance().createEmpty(
                    NativeDataType.FLOAT, 16, viewList.size());

                for (int k = 0; k < viewList.size(); k++)
                {
                    int d = 0;
                    for (int col = 0; col < 4; col++) // column
                    {
                        for (int row = 0; row < 4; row++) // row
                        {
                            cameraPoseData.set(k, d, viewList.get(k).cameraPose.get(row, col));
                            d++;
                        }
                    }
                }

                return cameraPoseData;
            }
        }
    }

    @Override
    public ReadonlyNativeVectorBuffer getCameraProjectionData()
    {
        synchronized (cameraProjectionList)
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
    }

    @Override
    public ReadonlyNativeVectorBuffer getCameraProjectionIndexData()
    {
        synchronized (viewList)
        {
            // Store the camera projection indices in a uniform buffer
            if (viewList.isEmpty())
            {
                return null;
            }
            else
            {
                int[] indexArray = new int[viewList.size()];
                Arrays.setAll(indexArray, i -> viewList.get(i).cameraProjectionIndex);
                return NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, viewList.size(), indexArray);
            }
        }
    }

    @Override
    public ReadonlyNativeVectorBuffer getLightPositionData()
    {
        synchronized (lightPositionList)
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
    }

    @Override
    public ReadonlyNativeVectorBuffer getLightIntensityData()
    {
        synchronized (lightIntensityList)
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
    }

    @Override
    public ReadonlyNativeVectorBuffer getLightIndexData()
    {
        synchronized (viewList)
        {
            // Store the light indices in a uniform buffer
            if (viewList.isEmpty())
            {
                return null;
            }
            else
            {
                int[] indexArray = new int[viewList.size()];
                Arrays.setAll(indexArray, i -> viewList.get(i).lightIndex);
                return NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, viewList.size(), indexArray);
            }
        }
    }

    public ReadonlyNativeVectorBuffer getViewIndexData()
    {
        synchronized (viewList)
        {
            int[] indexArray = viewList.stream()
                .filter(View::isEnabled)
                .mapToInt(View::getGPUViewIndex)
                .toArray();

            // Store the view indices in a uniform buffer
            if (indexArray.length == 0)
            {
                return null;
            }
            else
            {
                return NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, indexArray.length, indexArray);
            }
        }
    }

    @Override
    public ViewSet copy()
    {
        ViewSet result = new ViewSet(this.getViewCount());

        result.uuid = this.uuid;

        synchronized (viewList)
        {
            this.viewList.stream() // Deep copy for view list
                .map(view -> view.copy(result))
                .forEach(result.viewList::add);
            result.gpuBufferSize = this.gpuBufferSize;

            if (this.primaryView != null)
            {
                result.primaryView = result.viewList.stream()
                    .filter(this.primaryView::equals)
                    .findFirst().orElse(null);
            }

            if (this.orientationView != null)
            {
                result.orientationView = result.viewList.stream()
                    .filter(this.orientationView::equals)
                    .findFirst().orElse(null);
            }
        }

        synchronized (cameraProjectionList)
        {
            result.cameraProjectionList.addAll(this.cameraProjectionList);
        }

        synchronized (lightPositionList)
        {
            result.lightPositionList.addAll(this.lightPositionList);
        }

        synchronized (lightIntensityList)
        {
            result.lightIntensityList.addAll(this.lightIntensityList);
        }

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

            View currentView = new View(cameraPose, cameraPoseInv, 0, 0,
                    i, imageFile, null, new ViewRMSE(), result);
        }

        return result;
    }

    @Override
    public UUID getUUID()
    {
        return uuid;
    }

    /**
     *
     * @param uuid
     */
    void setUUID(UUID uuid)
    {
        this.uuid = uuid;
    }

    public void toggleCamera(File image)
    {
        View view = findViewByName(image.getName());
        if (view != null)
        {
            setCameraEnabled(image, !view.isEnabled);
        }
    }

    public void setCameraEnabled(File image, boolean isEnabled)
    {
        View view = findViewByName(image.getName());

        if (view.isEnabled) // Currently enabled, so disable camera
        {
            view.isEnabled = isEnabled;
            if (!isEnabled)
            {
                notifyObservers(new ViewSetChange(Type.MODIFIED, image));
            }
            // Else still enabled so nothing needs to change
        }
        else // Currently disabled, so enable camera
        {
            view.isEnabled = isEnabled;
            if (isEnabled)
            {
                notifyObservers(new ViewSetChange(Type.MODIFIED, image));
            }
            // Else still disabled so nothing needs to change
        }
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
    public int getPreviewWidth()
    {
        return previewWidth;
    }

    @Override
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
    public View getPrimaryView()
    {
        return primaryView;
    }

    public void setPrimaryView(View primaryView)
    {
        this.primaryView = primaryView;
    }

    @Override
    public View getOrientationView()
    {
        return this.orientationView;
    }

    /**
     * Set the view to use as a reference pose to reorient the model
     *
     * @param orientationView view
     */
    public void setOrientationView(View orientationView)
    {
        this.orientationView = orientationView;
    }

    public void setOrientationViewByName(String viewName)
    {
        this.orientationView = findViewByName(viewName);
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

    public View findViewByName(String viewName)
    {
        // Treat null as "not found" or "not present"
        // Important for allowing for orientation pose to remain unset.
        if (viewName == null)
        {
            return null;
        }

        synchronized (viewList)
        {
            // Try simple file comparison with full paths
            File key = new File(viewName);

            for (View view : viewList)
            {
                if (view.getImageFile().equals(key))
                {
                    LOG.debug("Matched {} for {}", view.getImageFile().getPath(), viewName);
                    return view;
                }
            }

            // Try just checking file name in case there were parent files
            // i.e. target file is photo314.jpg and imageFiles contains myPhotos/photo314.jpg

            // Also check for extension mismatch
            // i.e. the camera label is photo314.jpg, other times just photo314

            //This is all necessary due to inconsistencies with camera labels in frame.zip and chunk.zip xml's
            for (View view : viewList)
            {
                String imgName = view.getImageFile().getName();
                String shortenedImgName = removeExt(imgName);
                String shortenedViewName = removeExt(viewName);

                if (shortenedImgName.equals(shortenedViewName) || shortenedImgName.equals(viewName) || imgName.equals(shortenedViewName))
                {
                    LOG.debug("Matched {} for {}", view.getImageFile(), viewName);
                    return view;
                }
            }

            return null;
        }
    }

    public static String removeExt(String fileName)
    {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    @Override
    public List<View> getViews()
    {
        return Collections.unmodifiableList(viewList);
    }

    void addView(View view)
    {
        synchronized (viewList)
        {
            viewList.add(view);
            gpuBufferSize = Math.max(gpuBufferSize, view.getGPUViewIndex() + 1);

            if (primaryView == null)
            {
                // Set default primary view if none has been specified.
                primaryView = view;
            }
        }

        notifyObservers(new ViewSetChange(Type.ADDED, view.imageFile));
    }

    void removeView(View view)
    {
        boolean removed;

        synchronized (viewList)
        {
            removed = viewList.remove(view);
        }

        if (removed)
        {
            notifyObservers(new ViewSetChange(Type.REMOVED, view.imageFile));
        }
    }

    public void removeViewByImageFilename(File image)
    {
        boolean removed;

        synchronized (viewList)
        {
            removed = viewList.removeIf(view -> Objects.equals(view.imageFile.getName(), image.getName()));
        }

        if (removed)
        {
            notifyObservers(new ViewSetChange(Type.REMOVED, image));
        }
    }

    @Override
    public Projection getCameraProjection(int projectionIndex)
    {
        synchronized (cameraProjectionList)
        {
            return this.cameraProjectionList.get(projectionIndex);
        }
    }

    void addCameraProjection(Projection projection)
    {
        synchronized (cameraProjectionList)
        {
            this.cameraProjectionList.add(projection);
        }
    }

    @Override
    public Vector3 getLightPosition(int lightIndex)
    {
        synchronized (lightPositionList)
        {
            return this.lightPositionList.get(lightIndex);
        }
    }

    @Override
    public Vector3 getLightIntensity(int lightIndex)
    {
        synchronized (lightIntensityList)
        {
            return this.lightIntensityList.get(lightIndex);
        }
    }

    public void setLightPosition(int lightIndex, Vector3 lightPosition)
    {
        synchronized (lightPositionList)
        {
            this.lightPositionList.set(lightIndex, lightPosition);
        }
    }

    public void setLightIntensity(int lightIndex, Vector3 lightIntensity)
    {
        synchronized (lightIntensityList)
        {
            this.lightIntensityList.set(lightIndex, lightIntensity);
        }
    }

    void addLight(Vector3 position, Vector3 intensity)
    {
        synchronized (lightIntensityList)
        {
            this.lightIntensityList.add(intensity);
        }

        // Do lightPositionList last since that is what getLightCount is based on.
        // That way it should be less likely that we get an index out of bounds due to concurrency issues.
        synchronized (lightPositionList)
        {
            this.lightPositionList.add(position);
        }
    }

    @Override
    public int getViewCount()
    {
        synchronized (viewList)
        {
            return viewList.size();
        }
    }

    @Override
    public int getGPUBufferSize()
    {
        synchronized (viewList)
        {
            return this.gpuBufferSize;
        }
    }

    /**
     * Only intended to be used by ViewSetBuilder during initialization.
     */
    void optimizeGPUIndexing()
    {
        synchronized (viewList)
        {
            // Reassign view indices for smaller data set.
            for (int i = 0; i < viewList.size(); ++i)
            {
                viewList.get(i).gpuViewIndex = i;
            }
            gpuBufferSize = viewList.size();
        }
    }

    @Override
    public int getCameraProjectionCount()
    {
        synchronized (cameraProjectionList)
        {
            return this.cameraProjectionList.size();
        }
    }

    @Override
    public int getLightCount()
    {
        synchronized (lightPositionList)
        {
            return this.lightPositionList.size();
        }
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

    void setRecommmendedClipPlanes(float near, float far)
    {
        this.recommendedNearPlane = near;
        this.recommendedFarPlane = far;
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
    public boolean hasMasks()
    {
        return masksDirectory != null;
    }

    @Override
    public File getMasksDirectory()
    {
        return masksDirectory;
    }

    @Override
    public Map<Integer, File> getMasksMap()
    {
        synchronized (viewList)
        {
            Map<Integer, File> maskFiles = new HashMap<>(viewList.size());
            for (int i = 0; i < viewList.size(); ++i)
            {
                if (viewList.get(i).importedMaskFile != null)
                {
                    maskFiles.put(i, viewList.get(i).importedMaskFile);
                }
            }
            return Collections.unmodifiableMap(maskFiles);
        }
    }

    public void setMasksDirectory(File dir)
    {
        masksDirectory = dir;
    }

    /**
     * Checks that all mask files exist.  In doing so, it tries several variations (i.e. mask filename vs. photo filename,
     * _mask vs. no _mask, various file extensions) and changes the recorded mask filename for each view to an image that was found
     * (or eliminating the mask if the file is missing).
     */
    public void validateMasks()
    {
        synchronized (viewList)
        {
            for (View view : viewList)
            {
                File maskFile = view.getMaskFile();
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
                        new File(getMasksDirectory(), view.getFullResImageFile().getName()),
                        "_mask");
                }

                // Remove if no mask file was found, otherwise overwrite based on the file that was found
                view.importedMaskFile = maskFile;
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


            // Copy the list for thread safety without blocking while it copies all the files.
            Iterable<View> viewsCopy;

            synchronized (viewList)
            {
                viewsCopy = new ArrayList<>(viewList);
            }

            // Copy the files that were actually found
            for (View view : viewsCopy)
            {
                File maskSrcFile = view.getMaskFile();
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
    public <ContextType extends Context<ContextType>> ProgramBuilder<ContextType> getShaderProgramBuilder(ContextType context)
    {
        // Determine shader defines here that should apply globally as defaults without require specific resources other than view set data.
        // The defines can be overridden by the actual shader.
        return context.getShaderProgramBuilder()
            .define("VIEW_COUNT", getViewCount())
            .define("CAMERA_POSE_COUNT", getGPUBufferSize())
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
    public void registerObserver(Observer<ViewSetChange> observer)
    {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer<ViewSetChange> observer)
    {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(ViewSetChange change)
    {
        for (Observer<ViewSetChange> observer : observers)
        {
            observer.update(change);
        }
    }
}