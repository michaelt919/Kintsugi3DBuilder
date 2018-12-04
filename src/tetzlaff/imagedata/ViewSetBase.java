package tetzlaff.imagedata;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.imagedata.ViewSetImpl.Parameters;

public abstract class ViewSetBase implements ViewSet
{
    /**
     * A list of light source positions, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * Assumed by convention to be in camera space.
     * This list can be much smaller than the number of views if the same illumination conditions apply for multiple views.
     */
    protected final List<Vector3> lightPositionList;
    /**
     * A list of light source intensities, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * This list can be much smaller than the number of views if the same illumination conditions apply for multiple views.
     */
    protected final List<Vector3> lightIntensityList;
    /**
     * The reference linear luminance values used for decoding pixel colors.
     */
    protected double[] linearLuminanceValues;
    /**
     * The reference encoded luminance values used for decoding pixel colors.
     */
    protected byte[] encodedLuminanceValues;
    /**
     * The absolute file path to be used for loading all resources.
     */
    protected File rootDirectory;
    /**
     * The relative file path to be used for loading images.
     */
    protected String relativeImagePath;
    /**
     * The relative name of the mesh file.
     */
    protected String geometryFileName;
    /**
     * Used to decode pixel colors according to a gamma curve if reference values are unavailable, otherwise, affects the absolute brightness of the decoded colors.
     */
    protected float gamma;
    /**
     * If false, inverse-square light attenuation should be applied.
     */
    protected boolean infiniteLightSources;
    /**
     * The index of the view that sets the initial orientation when viewing, is used for color calibration, etc.
     */
    protected int primaryViewIndex = 0;

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

    public ViewSetBase(ParametersBase params)
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

    /**
     * Gets the root directory for this view set.
     * @return The root directory.
     */
    @Override
    public File getRootDirectory()
    {
        return this.rootDirectory;
    }

    /**
     * Sets the root directory for this view set.
     * @param rootDirectory The root directory.
     */
    @Override
    public void setRootDirectory(File rootDirectory)
    {
        this.rootDirectory = rootDirectory;
    }

    /**
     * Gets the name of the geometry file associated with this view set.
     * @return The name of the geometry file.
     */
    @Override
    public String getGeometryFileName()
    {
        return geometryFileName;
    }

    /**
     * Sets the name of the geometry file associated with this view set.
     * @param fileName The name of the geometry file.
     */
    @Override
    public void setGeometryFileName(String fileName)
    {
        this.geometryFileName = fileName;
    }

    /**
     * Gets the geometry file associated with this view set.
     * @return The geometry file.
     */
    @Override
    public File getGeometryFile()
    {
        return new File(this.rootDirectory, geometryFileName);
    }

    /**
     * Gets the image file path associated with this view set.
     * @return The image file path.
     */
    @Override
    public File getImageFilePath()
    {
        return this.relativeImagePath == null ? this.rootDirectory : new File(this.rootDirectory, relativeImagePath);
    }

    /**
     * Sets the image file path associated with this view set.
     * @return imageFilePath The image file path.
     */
    @Override
    public String getRelativeImagePathName()
    {
        return this.relativeImagePath;
    }

    /**
     * Sets the image file path associated with this view set.
     * @param relativeImagePath The image file path.
     */
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

    /**
     * Gets the position of a particular light source.
     * Used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * Assumed by convention to be in camera space.
     * @param lightIndex The index of the light source.
     * IMPORTANT: this is NOT usually the same as the index of the view to be retrieved.
     * @return The position of the light source.
     */
    @Override
    public Vector3 getLightPosition(int lightIndex)
    {
        return this.lightPositionList.get(lightIndex);
    }

    /**
     * Gets the intensity of a particular light source.
     * Used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * Assumed by convention to be in camera space.
     * @param lightIndex The index of the light source.
     * IMPORTANT: this is NOT usually the same as the index of the view to be retrieved.
     * @return The position of the light source.
     */
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

}
