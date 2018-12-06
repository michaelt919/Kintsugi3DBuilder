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
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import umn.gl.nativebuffer.NativeDataType;
import umn.gl.nativebuffer.NativeVectorBuffer;
import umn.gl.nativebuffer.NativeVectorBufferFactory;
import umn.gl.vecmath.Matrix4;
import umn.gl.vecmath.Vector3;
import umn.imagedata.ViewSetImpl.Parameters;

public abstract class ViewSetBase implements ViewSet
{
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
    private float gamma;
    /**
     * If false, inverse-square light attenuation should be applied.
     */
    private boolean infiniteLightSources;
    /**
     * The index of the view that sets the initial orientation when viewing, is used for color calibration, etc.
     */
    private int primaryViewIndex = 0;

    public static class ParametersBase
    {
        public final List<Vector3> lightPositionList = new ArrayList<>(128);
        public final List<Vector3> lightIntensityList = new ArrayList<>(128);
        public String relativeImagePath;
        public String geometryFileName;
        public File directory;
        public float gamma = 2.2f;
        public boolean infiniteLightSources;
        public double[] linearLuminanceValues;
        public byte[] encodedLuminanceValues;
        public float recommendedNearPlane;
        public float recommendedFarPlane;
    }

    protected ViewSetBase(ParametersBase params)
    {
        this.lightPositionList = params.lightPositionList;
        this.lightIntensityList = params.lightIntensityList;
        this.linearLuminanceValues = params.linearLuminanceValues;
        this.encodedLuminanceValues = params.encodedLuminanceValues;
        this.rootDirectory = params.directory;
        this.relativeImagePath = params.relativeImagePath;
        this.geometryFileName = params.geometryFileName;
        this.gamma = params.gamma;
        this.infiniteLightSources = params.infiniteLightSources;
    }

    @Override
    public File getRootDirectory()
    {
        return this.rootDirectory;
    }

    @Override
    public void setRootDirectory(File rootDirectory)
    {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public String getGeometryFileName()
    {
        return geometryFileName;
    }

    @Override
    public void setGeometryFileName(String fileName)
    {
        this.geometryFileName = fileName;
    }

    @Override
    public File getGeometryFile()
    {
        return new File(this.rootDirectory, geometryFileName);
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

    @Override
    public void setRelativeImagePathName(String relativeImagePath)
    {
        this.relativeImagePath = relativeImagePath;
    }

    @Override
    public int getPrimaryViewIndex()
    {
        return this.primaryViewIndex;
    }

    @Override
    public void setPrimaryView(int poseIndex)
    {
        this.primaryViewIndex = poseIndex;
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

    @Override
    public void setLightPosition(int lightIndex, Vector3 lightPosition)
    {
        this.lightPositionList.set(lightIndex, lightPosition);
    }

    @Override
    public void setLightIntensity(int lightIndex, Vector3 lightIntensity)
    {
        this.lightIntensityList.set(lightIndex, lightIntensity);
    }

    @Override
    public int getLightCount()
    {
        return this.lightPositionList.size();
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

    @Override
    public void setInfiniteLightSources(boolean infiniteLightSources)
    {
        this.infiniteLightSources = infiniteLightSources;
    }

    @Override
    public NativeVectorBuffer getCameraPoseData()
    {
        // Store the poses in a uniform buffer
        if (this.getCameraPoseCount() > 0)
        {
            // Flatten the camera pose matrices into 16-component vectors and store them in the vertex list data structure.
            NativeVectorBuffer cameraPoseData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 16,
                this.getCameraPoseCount());

            for (int k = 0; k < this.getCameraPoseCount(); k++)
            {
                int d = 0;
                for (int col = 0; col < 4; col++) // column
                {
                    for (int row = 0; row < 4; row++) // row
                    {
                        cameraPoseData.set(k, d, this.getCameraPose(k).get(row, col));
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
    public NativeVectorBuffer getCameraProjectionData()
    {
        // Store the camera projections in a uniform buffer
        if (this.getCameraProjectionCount() > 0)
        {
            // Flatten the camera projection matrices into 16-component vectors and store them in the vertex list data structure.
            NativeVectorBuffer cameraProjectionData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 16,
                this.getCameraProjectionCount());

            for (int k = 0; k < this.getCameraProjectionCount(); k++)
            {
                int d = 0;
                for (int col = 0; col < 4; col++) // column
                {
                    for (int row = 0; row < 4; row++) // row
                    {
                        Matrix4 projection =
                            this.getCameraProjection(k).getProjectionMatrix(this.getRecommendedNearPlane(), this.getRecommendedFarPlane());
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
    public NativeVectorBuffer getCameraProjectionIndexData()
    {
        // Store the camera projection indices in a uniform buffer
        if (this.getCameraPoseCount() > 0)
        {
            return NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, this.getCameraPoseCount(),
                IntStream.range(0, this.getCameraPoseCount()).map(this::getCameraProjectionIndex).toArray());
        }
        else
        {
            return null;
        }
    }

    @Override
    public NativeVectorBuffer getLightPositionData()
    {
        // Store the light positions in a uniform buffer
        if (lightPositionList != null && !lightPositionList.isEmpty())
        {
            NativeVectorBuffer lightPositionData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 4, getLightCount());
            for (int k = 0; k < getLightCount(); k++)
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
    public NativeVectorBuffer getLightIntensityData()
    {
        // Store the light positions in a uniform buffer
        if (lightIntensityList != null && !lightIntensityList.isEmpty())
        {
            NativeVectorBuffer lightIntensityData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 4, lightIntensityList.size());
            for (int k = 0; k < getLightCount(); k++)
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
    public NativeVectorBuffer getLightIndexData()
    {
        // Store the light indices indices in a uniform buffer
        if (this.getCameraPoseCount() > 0)
        {
            return NativeVectorBufferFactory.getInstance().createFromIntArray(false, 1, this.getCameraPoseCount(),
                IntStream.range(0, this.getCameraPoseCount()).map(this::getLightIndex).toArray());
        }
        else
        {
            return null;
        }
    }

    @Override
    public ViewSet createPermutation(Iterable<Integer> permutationIndices)
    {
        Parameters params = new Parameters();

        for (int i : permutationIndices)
        {
            params.cameraPoseList.add(this.getCameraPose(i));
            params.cameraPoseInvList.add(this.getCameraPoseInverse(i));
            params.cameraProjectionIndexList.add(this.getCameraProjectionIndex(i));
            params.lightIndexList.add(this.getLightIndex(i));
            params.imageFileNames.add(this.getImageFileName(i));
        }

        for (int i = 0; i < this.getCameraProjectionCount(); i++)
        {
            params.cameraProjectionList.add(this.getCameraProjection(i));
        }

        params.lightIntensityList.addAll(this.lightIntensityList);
        params.lightPositionList.addAll(this.lightPositionList);

        params.relativeImagePath = this.relativeImagePath;
        params.geometryFileName = this.geometryFileName;
        params.directory = this.rootDirectory;
        params.gamma = this.gamma;
        params.infiniteLightSources = this.infiniteLightSources;
        params.recommendedNearPlane = this.getRecommendedNearPlane();
        params.recommendedFarPlane = this.getRecommendedFarPlane();

        params.linearLuminanceValues = Arrays.copyOf(this.linearLuminanceValues, this.linearLuminanceValues.length);
        params.encodedLuminanceValues = Arrays.copyOf(this.encodedLuminanceValues, this.encodedLuminanceValues.length);

        return new ViewSetImpl(params);
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

        out.println("\n# Geometry file name (mesh)");
        out.println("m " + (parentDirectory == null ? geometryFileName : parentDirectory.relativize(getGeometryFile().toPath())));
        out.println("\n# Image file path");
        out.println("i " + (parentDirectory == null ? relativeImagePath : parentDirectory.relativize(getImageFilePath().toPath())));

        out.println("\n# Estimated near and far planes");
        out.printf("c\t%.8f\t%.8f\n", this.getRecommendedNearPlane(), this.getRecommendedFarPlane());

        out.println("\n# " + this.getCameraProjectionCount() + (this.getCameraProjectionCount()==1?" Sensor":" Sensors"));
        for (int i = 0; i < this.getCameraProjectionCount(); i++)
        {
            out.println(this.getCameraProjection(i).toVSETString());
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

        out.println("\n# " + this.getCameraPoseCount() + (this.getCameraPoseCount()==1?" Camera":" Cameras"));
        for (int i = 0; i < this.getCameraPoseCount(); i++)
        {
            Matrix4 pose = this.getCameraPose(i);

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

        out.println("\n# " + this.getCameraPoseCount() + (this.getCameraPoseCount()==1?" View":" Views"));

        // Primary view first (so that next time the view set is loaded it will be index 0)
        out.printf("v\t%d\t%d\t%d\t%s\n", primaryViewIndex, this.getCameraProjectionIndex(primaryViewIndex),
            this.getLightIndex(primaryViewIndex), this.getImageFileName(primaryViewIndex));
        for (int ID=0; ID<this.getCameraPoseCount(); ID++)
        {
            if (ID != primaryViewIndex)
            {
                out.printf("v\t%d\t%d\t%d\t%s\n", ID,  this.getCameraProjectionIndex(ID), this.getLightIndex(ID), this.getImageFileName(ID));
            }
        }

        out.close();
    }
}
