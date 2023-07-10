/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.core;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.nativebuffer.ReadonlyNativeVectorBuffer;
import tetzlaff.gl.vecmath.Matrix3;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.util.ImageFinder;

/**
 * A class representing a collection of photographs, or views.
 * @author Michael Tetzlaff
 */
public final class ViewSet implements ReadonlyViewSet
{
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
    private String relativeImagePath;

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
        if (!cameraPoseList.isEmpty())
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
        else
        {
            return null;
        }
    }

    @Override
    public ReadonlyNativeVectorBuffer getCameraProjectionData()
    {
        // Store the camera projections in a uniform buffer
        if (!cameraProjectionList.isEmpty())
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
        else
        {
            return null;
        }
    }

    @Override
    public ReadonlyNativeVectorBuffer getCameraProjectionIndexData()
    {
        // Store the camera projection indices in a uniform buffer
        if (!cameraProjectionIndexList.isEmpty())
        {
            int[] indexArray = new int[cameraProjectionIndexList.size()];
            for (int i = 0; i < indexArray.length; i++)
            {
                indexArray[i] = cameraProjectionIndexList.get(i);
            }
            return NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, cameraProjectionIndexList.size(), indexArray);
        }
        else
        {
            return null;
        }
    }

    @Override
    public ReadonlyNativeVectorBuffer getLightPositionData()
    {
        // Store the light positions in a uniform buffer
        if (!lightPositionList.isEmpty())
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
        else
        {
            return null;
        }
    }

    @Override
    public ReadonlyNativeVectorBuffer getLightIntensityData()
    {
        // Store the light positions in a uniform buffer
        if (!lightIntensityList.isEmpty())
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
        else
        {
            return null;
        }
    }

    @Override
    public ReadonlyNativeVectorBuffer getLightIndexData()
    {
        // Store the light indices in a uniform buffer
        if (!lightIndexList.isEmpty())
        {
            int[] indexArray = new int[lightIndexList.size()];
            for (int i = 0; i < indexArray.length; i++)
            {
                indexArray[i] = lightIndexList.get(i);
            }
            return NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, lightIndexList.size(), indexArray);
        }
        else
        {
            return null;
        }
    }

    @Override
    public ReadonlyViewSet createPermutation(Collection<Integer> permutationIndices)
    {
        ViewSet result = new ViewSet(permutationIndices.size());

        for (int i : permutationIndices)
        {
            result.getCameraPoseList().add(this.cameraPoseList.get(i));
            result.getCameraPoseInvList().add(this.cameraPoseInvList.get(i));
            result.getCameraProjectionIndexList().add(this.cameraProjectionIndexList.get(i));
            result.getLightIndexList().add(this.lightIndexList.get(i));
            result.getImageFileNames().add(this.imageFileNames.get(i));
        }

        result.getCameraProjectionList().addAll(this.cameraProjectionList);
        result.getLightIntensityList().addAll(this.lightIntensityList);
        result.getLightPositionList().addAll(this.lightPositionList);

        result.setTonemapping(this.gamma,
            Arrays.copyOf(this.linearLuminanceValues, this.linearLuminanceValues.length),
            Arrays.copyOf(this.encodedLuminanceValues, this.encodedLuminanceValues.length));

        result.setRootDirectory(this.rootDirectory);
        result.setRelativeImagePathName(this.relativeImagePath);
        result.setGeometryFileName(this.geometryFileName);
        result.setInfiniteLightSources(this.infiniteLightSources);
        result.setRecommendedNearPlane(this.recommendedNearPlane);
        result.setRecommendedFarPlane(this.recommendedFarPlane);
        result.setPrimaryView(primaryViewIndex);

        return result;
    }

    @Override
    public ViewSet copy()
    {
        ViewSet result = new ViewSet(this.getCameraPoseCount());

        result.getCameraPoseList().addAll(this.cameraPoseList);
        result.getCameraPoseInvList().addAll(this.cameraPoseInvList);
        result.getCameraProjectionList().addAll(this.cameraProjectionList);
        result.getCameraProjectionIndexList().addAll(this.cameraProjectionIndexList);
        result.getLightPositionList().addAll(this.lightPositionList);
        result.getLightIntensityList().addAll(this.lightIntensityList);
        result.getLightIndexList().addAll(this.lightIndexList);
        result.getImageFileNames().addAll(this.imageFileNames);

        result.setTonemapping(this.gamma,
            Arrays.copyOf(this.linearLuminanceValues, this.linearLuminanceValues.length),
            Arrays.copyOf(this.encodedLuminanceValues, this.encodedLuminanceValues.length));

        result.setRootDirectory(this.rootDirectory);
        result.setRelativeImagePathName(this.relativeImagePath);
        result.setGeometryFileName(this.geometryFileName);
        result.setInfiniteLightSources(this.infiniteLightSources);
        result.setRecommendedNearPlane(this.recommendedNearPlane);
        result.setRecommendedFarPlane(this.recommendedFarPlane);
        result.setPrimaryView(primaryViewIndex);

        return result;
    }

    public static ReadonlyViewSet createFromLookAt(List<Vector3> viewDir, Vector3 center, Vector3 up, float distance,
        float nearPlane, float aspect, float sensorWidth, float focalLength)
    {
        ViewSet result = new ViewSet(viewDir.size());

        result.getCameraProjectionList().add(new DistortionProjection(sensorWidth, sensorWidth / aspect, focalLength));

        result.setRecommendedNearPlane(nearPlane);
        result.setRecommendedFarPlane(2 * distance - nearPlane);

        result.getLightIntensityList().add(new Vector3(distance * distance));
        result.getLightPositionList().add(Vector3.ZERO);

        for (int i = 0; i < viewDir.size(); i++)
        {
            result.getCameraProjectionIndexList().add(0);
            result.getLightIndexList().add(0);
            result.getImageFileNames().add(String.format("%04d.png", i + 1));

            Matrix4 cameraPose = Matrix4.lookAt(viewDir.get(i).times(-distance).plus(center), center, up);

            result.getCameraPoseList().add(cameraPose);
            result.getCameraPoseInvList().add(cameraPose.quickInverse(0.001f));
        }

        return result;
    }

    @Override
    public void writeVSETFileToStream(OutputStream outputStream)
    {
        writeVSETFileToStream(outputStream, null);
    }

    @Override
    public void writeVSETFileToStream(OutputStream outputStream, Path parentDirectory)
    {
        PrintStream out = new PrintStream(outputStream);
        out.println("# Created by IBRelight");

        if (getGeometryFile() != null)
        {
            out.println("\n# Geometry file name (mesh)");
            out.println("m " + (parentDirectory == null ? geometryFileName : parentDirectory.relativize(getGeometryFile().toPath())));
        }

        out.println("\n# Image file path");
        out.println("i " + (parentDirectory == null ? relativeImagePath : parentDirectory.relativize(getImageFilePath().toPath())));

        out.println("\n# Estimated near and far planes");
        out.printf("c\t%.8f\t%.8f\n", recommendedNearPlane, recommendedFarPlane);

        out.println("\n# " + cameraProjectionList.size() + (cameraProjectionList.size()==1?" Sensor":" Sensors"));
        for (Projection proj : cameraProjectionList)
        {
            out.println(proj.toVSETString());
        }

        if (linearLuminanceValues != null && encodedLuminanceValues != null)
        {
            out.println("\n# Luminance encoding: Munsell 2/3.5/5.6.5/8/9.5");
            out.println("#\tCIE-Y/100\tEncoded");
            for(int i = 0; i < linearLuminanceValues.length && i < encodedLuminanceValues.length; i++)
            {
                out.printf("e\t%.8f\t\t%3d\n", linearLuminanceValues[i], 0x00FF & encodedLuminanceValues[i]);
            }
        }

        out.println("\n# " + cameraPoseList.size() + (cameraPoseList.size()==1?" Camera":" Cameras"));
        for (Matrix4 pose : cameraPoseList)
        {
            // TODO validate quaternion computation
//            Matrix3 rot = new Matrix3(pose);
//            if (rot.determinant() == 1.0f)
//            {
//                // No scale - use quaternion
//                Vector4 quat = rot.toQuaternion();
//                Vector4 loc = pose.getColumn(3);
//                out.printf("p\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\n",
//                            loc.x, loc.y, loc.z, quat.x, quat.y, quat.z, quat.w);
//            }
//            else
            //{
                // Write a general 4x4 matrix
                out.printf("P\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\n",
                        pose.get(0, 0), pose.get(0, 1), pose.get(0, 2), pose.get(0, 3),
                        pose.get(1, 0), pose.get(1, 1), pose.get(1, 2), pose.get(1, 3),
                        pose.get(2, 0), pose.get(2, 1), pose.get(2, 2), pose.get(2, 3),
                        pose.get(3, 0), pose.get(3, 1), pose.get(3, 2), pose.get(3, 3));
            //}
        }

        if(!lightPositionList.isEmpty())
        {
            out.println("\n# " + lightPositionList.size() + (lightPositionList.size()==1?" Light":" Lights"));
            for (int ID=0; ID < lightPositionList.size(); ID++)
            {
                Vector3 pos = lightPositionList.get(ID);
                Vector3 intensity = lightIntensityList.get(ID);
                out.printf("l\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\n", pos.x, pos.y, pos.z, intensity.x, intensity.y, intensity.z);
            }
        }

        out.println("\n# " + cameraPoseList.size() + (cameraPoseList.size()==1?" View":" Views"));

        // Primary view first (so that next time the view set is loaded it will be index 0)
        out.printf("v\t%d\t%d\t%d\t%s\n", primaryViewIndex,  cameraProjectionIndexList.get(primaryViewIndex), lightIndexList.get(primaryViewIndex), imageFileNames.get(primaryViewIndex));
        for (int ID=0; ID<cameraPoseList.size(); ID++)
        {
            if (ID != primaryViewIndex)
            {
                out.printf("v\t%d\t%d\t%d\t%s\n", ID,  cameraProjectionIndexList.get(ID), lightIndexList.get(ID), imageFileNames.get(ID));
            }
        }

        out.close();
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
     * Sets the root directory for this view set.
     * @param rootDirectory The root directory.
     */
    public void setRootDirectory(File rootDirectory)
    {
        this.rootDirectory = rootDirectory;
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
    public File getImageFilePath()
    {
        return this.relativeImagePath == null ? this.rootDirectory : new File(this.rootDirectory, relativeImagePath);
    }

    @Override
    public String getRelativeImagePathName()
    {
        return this.relativeImagePath;
    }

    /**
     * Sets the image file path associated with this view set.
     * @param relativeImagePath The image file path.
     */
    public void setRelativeImagePathName(String relativeImagePath)
    {
        this.relativeImagePath = relativeImagePath;
    }

    @Override
    public String getImageFileName(int poseIndex)
    {
        return this.imageFileNames.get(poseIndex);
    }

    @Override
    public File getImageFile(int poseIndex)
    {
        return new File(this.getImageFilePath(), this.imageFileNames.get(poseIndex));
    }
    
    @Override
    public int getPrimaryViewIndex()
    {
        return this.primaryViewIndex;
    }

    public void setPrimaryView(int poseIndex)
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
    public File findImageFile(int index) throws FileNotFoundException
    {
        return ImageFinder.getInstance().findImageFile(getImageFile(index));
    }

    @Override
    public File findPrimaryImageFile() throws FileNotFoundException
    {
        return findImageFile(getPrimaryViewIndex());
    }
}
