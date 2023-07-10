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
    private ViewSet(int initialCapacity)
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

    /**
     * Loads a VSET file and creates a corresponding ViewSet object.
     * @param vsetFile The VSET file to load.
     * @return The newly created ViewSet object.
     * @throws FileNotFoundException Thrown if the view set file is not found.
     */
    public static ViewSet loadFromVSETFile(File vsetFile) throws FileNotFoundException
    {
        Date timestamp = new Date();

        ViewSet result = new ViewSet(128);

        float gamma = 2.2f;

        List<Double> linearLuminanceList = new ArrayList<>(8);
        List<Byte> encodedLuminanceList = new ArrayList<>(8);

        try(Scanner scanner = new Scanner(vsetFile))
        {
            List<Matrix4> unorderedCameraPoseList = new ArrayList<>(128);
            List<Matrix4> unorderedCameraPoseInvList = new ArrayList<>(128);

            while (scanner.hasNext())
            {
                String id = scanner.next();
                switch(id)
                {
                    case "c":
                    {
                        result.setRecommendedNearPlane(scanner.nextFloat());
                        result.setRecommendedFarPlane(scanner.nextFloat());
                        scanner.nextLine();
                        break;
                    }
                    case "m":
                    {
                        result.setGeometryFileName(scanner.nextLine().trim());
                        break;
                    }
                    case "i":
                    {
                        result.setRelativeImagePathName(scanner.nextLine().trim());
                        break;
                    }
                    case "p":
                    {
                        // Pose from quaternion
                        float x = scanner.nextFloat();
                        float y = scanner.nextFloat();
                        float z = scanner.nextFloat();
                        float i = scanner.nextFloat();
                        float j = scanner.nextFloat();
                        float k = scanner.nextFloat();
                        float qr = scanner.nextFloat();

                        unorderedCameraPoseList.add(Matrix4.fromQuaternion(i, j, k, qr)
                            .times(Matrix4.translate(-x, -y, -z)));

                        unorderedCameraPoseInvList.add(Matrix4.translate(x, y, z)
                            .times(Matrix3.fromQuaternion(i, j, k, qr).transpose().asMatrix4()));

                        scanner.nextLine();
                        break;
                    }
                    case "P":
                    {
                        // Pose from matrix
                        Matrix4 newPose = Matrix4.fromRows(
                            new Vector4(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat()),
                            new Vector4(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat()),
                            new Vector4(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat()),
                            new Vector4(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat()));

                        unorderedCameraPoseList.add(newPose);
                        unorderedCameraPoseInvList.add(newPose.quickInverse(0.002f));
                        break;
                    }
                    case "d":
                    case "D":
                    {
                        // Skip "center/offset" parameters which are not consistent across all VSET files
                        scanner.nextFloat();
                        scanner.nextFloat();

                        float aspect = scanner.nextFloat();
                        float focalLength = scanner.nextFloat();

                        float sensorWidth;
                        float k1;
                        float k2;
                        float k3;
                        if ("D".equals(id))
                        {
                            sensorWidth = scanner.nextFloat();
                            k1 = scanner.nextFloat();
                            k2 = scanner.nextFloat();
                            k3 = scanner.nextFloat();
                        }
                        else
                        {
                            sensorWidth = 32.0f; // Default sensor width
                            k1 = scanner.nextFloat();
                            k2 = k3 = 0.0f;
                        }

                        float sensorHeight = sensorWidth / aspect;

                        result.getCameraProjectionList().add(new DistortionProjection(
                            sensorWidth, sensorHeight,
                            focalLength, focalLength,
                            sensorWidth / 2, sensorHeight / 2, k1, k2, k3
                        ));

                        scanner.nextLine();
                        break;
                    }
                    case "e":
                    {
                        // Non-linear encoding
                        linearLuminanceList.add(scanner.nextDouble());
                        encodedLuminanceList.add((byte)scanner.nextShort());
                        scanner.nextLine();
                        break;
                    }
                    case "g":
                    {
                        // Gamma
                        gamma = scanner.nextFloat();
                        scanner.nextLine();
                        break;
                    }
                    case "f":
                    {
                        // Skip "center/offset" parameters which are not consistent across all VSET files
                        scanner.next();
                        scanner.next();

                        float aspect = scanner.nextFloat();
                        float fovy = (float)(scanner.nextFloat() * Math.PI / 180.0);

                        result.getCameraProjectionList().add(new SimpleProjection(aspect, fovy));

                        scanner.nextLine();
                        break;
                    }
                    case "l":
                    {
                        float x = scanner.nextFloat();
                        float y = scanner.nextFloat();
                        float z = scanner.nextFloat();
                        result.getLightPositionList().add(new Vector3(x, y, z));

                        float r = scanner.nextFloat();
                        float g = scanner.nextFloat();
                        float b = scanner.nextFloat();
                        result.getLightIntensityList().add(new Vector3(r, g, b));

                        // Skip the rest of the line
                        scanner.nextLine();
                        break;
                    }

                    case "v":
                    {
                        int poseId = scanner.nextInt();
                        int projectionId = scanner.nextInt();
                        int lightId = scanner.nextInt();

                        String imgFilename = scanner.nextLine().trim();

                        result.getCameraPoseList().add(unorderedCameraPoseList.get(poseId));
                        result.getCameraPoseInvList().add(unorderedCameraPoseInvList.get(poseId));
                        result.getCameraProjectionIndexList().add(projectionId);
                        result.getLightIndexList().add(lightId);
                        result.getImageFileNames().add(imgFilename);
                        break;
                    }
                    default:
                        // Skip unrecognized line
                        scanner.nextLine();
                }
            }
        }

        double[] linearLuminanceValues = new double[linearLuminanceList.size()];
        for (int i = 0; i < linearLuminanceValues.length; i++)
        {
            linearLuminanceValues[i] = linearLuminanceList.get(i);
        }

        byte[] encodedLuminanceValues = new byte[encodedLuminanceList.size()];
        for (int i = 0; i < encodedLuminanceValues.length; i++)
        {
            encodedLuminanceValues[i] = encodedLuminanceList.get(i);
        }

        result.setTonemapping(gamma, linearLuminanceValues, encodedLuminanceValues);

        int maxLightIndex = result.getLightIndexList().stream().max(Comparator.naturalOrder()).orElse(-1);

        for (int i = result.getLightIntensityList().size(); i <= maxLightIndex; i++)
        {
            result.getLightPositionList().add(Vector3.ZERO);
            result.getLightIntensityList().add(Vector3.ZERO);
        }

        result.setRootDirectory(vsetFile.getParentFile());

        if (result.getGeometryFile() == null)
        {
            result.setGeometryFileName("manifold.obj"); // Used by some really old datasets
        }

        System.out.println("View Set file loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        return result;
    }

    /**
     * A private class for representing a "sensor" in an Agisoft PhotoScan XML file.
     * @author Michael Tetzlaff
     *
     */
    private static class Sensor
    {
        int index;
        String id;
        float width;
        float height;
        float fx;
        float fy;
        float cx;
        float cy;
        float k1;
        float k2;
        float k3;
        float k4;
        float p1;
        float p2;
        float skew;
        float pixelWidth = 1.0f;
        float pixelHeight = 1.0f;

        Sensor(String id)
        {
            this.id = id;
        }
    }

    /**
     * A private class for representing a "camera" in an Agisoft PhotoScan XML file.
     * @author Michael Tetzlaff
     *
     */
    private static class Camera
    {
        String id;
        String filename;
        Matrix4 transform;
        Sensor sensor;
        int lightIndex;

        @SuppressWarnings("unused")
        int orientation;

        Camera(String id)
        {
            this.id = id;
        }

        Camera(String id, Sensor sensor, int lightIndex)
        {
            this.id = id;
            this.sensor = sensor;
            this.lightIndex = lightIndex;
        }

        @Override
        public boolean equals(Object other)
        {
            if (other instanceof Camera)
            {
                Camera otherCam = (Camera) other;
                return this.id.equals(otherCam.id);
            }
            else
            {
                return false;
            }
        }
    }

    /**
     * Loads a camera definition file exported in XML format from Agisoft PhotoScan.
     * @param file The Agisoft PhotoScan XML camera file to load.
     * @return The newly created ViewSet object.
     * @throws FileNotFoundException Thrown if the XML camera file is not found.
     */
    public static ViewSet loadFromAgisoftXMLFile(File file) throws FileNotFoundException, XMLStreamException
    {
        Map<String, Sensor> sensorSet = new Hashtable<>();
        HashSet<Camera> cameraSet = new HashSet<>();
        
        Sensor sensor = null;
        Camera camera = null;
        int lightIndex = -1;
        int nextLightIndex = 0;
        int defaultLightIndex = -1;

        float globalScale = 1.0f;
        Matrix4 globalRotation = Matrix4.IDENTITY;
        Vector3 globalTranslate = new Vector3(0.0f, 0.0f, 0.0f);
        
        String version = "";
        String chunkLabel = "";
        String groupLabel = "";
        String sensorID = "";
        String cameraID = "";
        String imageFile = "";
        int intVersion = 0;
        
        XMLInputFactory factory = XMLInputFactory.newInstance();

        InputStream xmlStream = new FileInputStream(file);
        XMLStreamReader reader = factory.createXMLStreamReader(xmlStream);
        while (reader.hasNext())
        {
            int event = reader.next();
            switch(event)
            {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName())
                    {
                        case "document":
                            version = reader.getAttributeValue(null, "version");
                            String[] verComponents = version.split("\\.");
                            for (String verComponent : verComponents)
                            {
                                intVersion *= 10;
                                intVersion += Integer.parseInt(verComponent);
                            }
                            System.out.printf("PhotoScan XML version %s (%d)\n", version, intVersion);
                            break;
                        case "chunk":
                            chunkLabel = reader.getAttributeValue(null, "label");
                            if(chunkLabel == null)
                            {
                                chunkLabel = "unnamed";
                            }
                            System.out.printf("Reading chunk '%s'\n", chunkLabel);
                            break;
                        case "group":
                            groupLabel = reader.getAttributeValue(null, "label");
                            System.out.printf("Reading group '%s'\n", groupLabel);
                            lightIndex = nextLightIndex;
                            nextLightIndex++;
                            System.out.printf("Light index: " + lightIndex);
                            break;
                        case "sensor":
                            sensorID = reader.getAttributeValue(null, "id");
                            System.out.printf("\tAdding sensor '%s'\n", sensorID);
                            sensor = new Sensor(sensorID);
                            break;
                        case "camera":
                            cameraID = reader.getAttributeValue(null, "id");
                            if(cameraID == null || cameraSet.contains(new Camera(cameraID)))
                            {
                               camera = null;
                            }
                            else
                            {
                                if (Objects.equals(reader.getAttributeValue(null, "enabled"), "true") ||
                                    Objects.equals(reader.getAttributeValue(null, "enabled"), "1") ||
                                    Objects.equals(reader.getAttributeValue(null, "enabled"), null))
                                {
                                    if (lightIndex < 0)
                                    {
                                        // Set default light index
                                        lightIndex = defaultLightIndex = nextLightIndex;
                                        nextLightIndex++;
                                        System.out.println("Using default light index: " + lightIndex);
                                    }

                                    sensorID = reader.getAttributeValue(null, "sensor_id");
                                    imageFile = reader.getAttributeValue(null, "label");
                                    camera = new Camera(cameraID, sensorSet.get(sensorID), lightIndex);
                                    camera.filename = imageFile;
                                }
                                else
                                {
                                    camera = null;
                                }
                            }
                            break;
                        case "orientation":
                            if(camera != null)
                            {
                                camera.orientation = Integer.parseInt(reader.getElementText());
                            }
                            break;
                        case "image":
                            if (camera != null)
                            {
                                camera.filename = reader.getAttributeValue(null, "path");
                            }
                            break;
                        case "resolution":
                            if (sensor != null)
                            {
                                sensor.width = Float.parseFloat(reader.getAttributeValue(null, "width"));
                                sensor.height = Float.parseFloat(reader.getAttributeValue(null, "height"));
                            }
                            break;
                        case "f":
                            if (sensor != null)
                            {
                                sensor.fx = Float.parseFloat(reader.getElementText());
                                sensor.fy = sensor.fx;
                            }
                            break;
                        case "fx":
                            if (sensor != null)
                            {
                                sensor.fx = Float.parseFloat(reader.getElementText());
                            }
                            break;
                        case "fy":
                            if (sensor != null)
                            {
                                sensor.fy = Float.parseFloat(reader.getElementText());
                            }
                            break;
                        case "cx":
                            if (sensor != null)
                            {
                                sensor.cx = Float.parseFloat(reader.getElementText());
                            }
                            break;
                        case "cy":
                            if (sensor != null)
                            {
                                sensor.cy = Float.parseFloat(reader.getElementText());
                            }
                            break;
                        case "p1":
                            if (sensor != null)
                            {
                                sensor.p1 = Float.parseFloat(reader.getElementText());
                            }
                            break;
                        case "p2":
                            if (sensor != null)
                            {
                                sensor.p2 = Float.parseFloat(reader.getElementText());
                            }
                            break;
                        case "k1":
                            if (sensor != null)
                            {
                                sensor.k1 = Float.parseFloat(reader.getElementText());
                            }
                            break;
                        case "k2":
                            if (sensor != null)
                            {
                                sensor.k2 = Float.parseFloat(reader.getElementText());
                            }
                            break;
                        case "k3":
                            if (sensor != null)
                            {
                                sensor.k3 = Float.parseFloat(reader.getElementText());
                            }
                            break;
                        case "k4":
                            if (sensor != null)
                            {
                                sensor.k4 = Float.parseFloat(reader.getElementText());
                            }
                            break;
                        case "skew":
                            if (sensor != null)
                            {
                                sensor.skew = Float.parseFloat(reader.getElementText());
                            }
                            break;

                        case "transform":
                            if(camera == null && intVersion >= 110)
                            {
                                break;
                            }

                        case "rotation":
                            {
                                String[] components = reader.getElementText().split("\\s");
                                if (("transform".equals(reader.getLocalName()) && components.length < 16) ||
                                    ("rotation".equals(reader.getLocalName()) && components.length < 9))
                                {
                                    System.err.println("Error: Not enough components in the transform/rotation matrix");
                                }
                                else
                                {
                                    int expectedSize = 16;
                                    if("rotation".equals(reader.getLocalName()))
                                    {
                                        expectedSize = 9;
                                    }

                                    if(components.length > expectedSize)
                                    {
                                        System.err.println("Warning: Too many components in the transform/rotation matrix, ignoring extras.");
                                    }

                                    float[] m = new float[expectedSize];
                                    for (int i = 0; i < expectedSize; i++)
                                    {
                                        m[i] = Float.parseFloat(components[i]);
                                    }

                                    if (camera != null)
                                    {
                                        // Negate 2nd and 3rd column to rotate 180 degrees around x-axis
                                        // Invert matrix by transposing rotation and negating translation
                                        Matrix4 trans;
                                        if(expectedSize == 9)
                                        {
                                            trans = Matrix3.fromRows(
                                                    new Vector3( m[0],  m[3],  m[6]),
                                                    new Vector3(-m[1], -m[4], -m[7]),
                                                    new Vector3(-m[2], -m[5], -m[8]))
                                                .asMatrix4();
                                        }
                                        else
                                        {
                                            trans = Matrix3.fromRows(
                                                    new Vector3( m[0],     m[4],  m[8]),
                                                    new Vector3(-m[1], -m[5], -m[9]),
                                                    new Vector3(-m[2], -m[6], -m[10]))
                                                .asMatrix4()
                                                .times(Matrix4.translate(-m[3], -m[7], -m[11]));
                                        }

                                        camera.transform = trans;
                                    }
                                    else
                                    {
                                        if(expectedSize == 9)
                                        {
                                            System.out.println("\tSetting global rotation.");
                                            globalRotation = Matrix3.fromRows(
                                                    new Vector3(m[0], m[3], m[6]),
                                                    new Vector3(m[1], m[4], m[7]),
                                                    new Vector3(m[2], m[5], m[8]))
                                                .asMatrix4();
                                        }
                                        else
                                        {
                                            System.out.println("\tSetting global transformation.");
                                            globalRotation = Matrix3.fromRows(
                                                    new Vector3(m[0], m[4], m[8]),
                                                    new Vector3(m[1], m[5], m[9]),
                                                    new Vector3(m[2], m[6], m[10]))
                                                .asMatrix4()
                                                .times(Matrix4.translate(m[3], m[7], m[11]));
                                        }
                                    }
                                }
                            }
                            break;

                        case "translation":
                            if (camera == null)
                            {
                                System.out.println("\tSetting global translate.");
                                String[] components = reader.getElementText().split("\\s");
                                globalTranslate = new Vector3(
                                        -Float.parseFloat(components[0]),
                                        -Float.parseFloat(components[1]),
                                        -Float.parseFloat(components[2]));
                            }
                            break;

                        case "scale":
                            if (camera == null)
                            {
                                System.out.println("\tSetting global scale.");
                                globalScale = 1.0f / Float.parseFloat(reader.getElementText());
                            }
                            break;

                        case "property":
                            if (sensor != null)
                            {
                                if (Objects.equals(reader.getAttributeValue(null, "name"), "pixel_width"))
                                {
                                    sensor.pixelWidth = Float.parseFloat(reader.getAttributeValue(null, "value"));
                                }
                                else if (Objects.equals(reader.getAttributeValue(null, "name"), "pixel_height"))
                                {
                                    sensor.pixelHeight = Float.parseFloat(reader.getAttributeValue(null, "value"));
                                }
                            }
                            break;
                        case "projections": case "depth":
                        case "frames": case "frame": case "meta": case "R":
                        case "size": case "center": case "region": case "settings":
                        case "ground_control": case "mesh": case "texture":
                        case "model": case "calibration": case "thumbnail":
                        case "point_cloud": case "points": case "sensors":
                        case "cameras":
                           // These can all be safely ignored if version is >= 0.9.1
                        break;

                        case "photo": case "tracks": case "depth_maps":
                        case "depth_map": case "dense_cloud":
                           if(intVersion < 110)
                           {
                               System.out.printf("Unexpected tag '%s' for psz version %s\n",
                                                   reader.getLocalName(), version);
                           }
                        break;

                        default:
                           System.out.printf("Unexpected tag '%s'\n", reader.getLocalName());
                           break;
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                {
                    switch (reader.getLocalName())
                    {
                        case "chunk":
                            System.out.printf("Finished chunk '%s'\n", chunkLabel);
                            chunkLabel = "";
                            break;
                        case "group":
                            System.out.printf("Finished group '%s'\n", groupLabel);
                            groupLabel = "";
                            lightIndex = defaultLightIndex;
                            break;
                        case "sensor":
                            if(sensor != null)
                            {
                                sensorSet.put(sensor.id, sensor);
                                sensor = null;
                            }
                            break;
                        case "camera":
                            if(camera != null && camera.transform != null)
                            {
                               cameraSet.add(camera);
                               System.out.printf("\tAdding camera %s, with sensor %s and image %s\n",
                                       cameraID, sensorID, imageFile);
                               camera = null;
                            }
                            break;
                    }
                }
                break;
            }
        }

        ViewSet result = new ViewSet(cameraSet.size());
        
        Sensor[] sensors = sensorSet.values().toArray(new Sensor[0]);
        
        // Reassign the ID for each sensor to correspond with the sensor's index
        // and add the corresponding projection to the list.
        for (int i = 0; i < sensors.length; i++)
        {
            sensors[i].index = i;
            result.getCameraProjectionList().add(new DistortionProjection(
                sensors[i].width,
                sensors[i].height,
                sensors[i].fx,
                sensors[i].fy,
                sensors[i].cx,
                sensors[i].cy,
                sensors[i].k1,
                sensors[i].k2,
                sensors[i].k3,
                sensors[i].k4,
                sensors[i].p1,
                sensors[i].p2,
                sensors[i].skew
            ));
        }
                
        Camera[] cameras = cameraSet.toArray(new Camera[0]);
        
        // Fill out the camera pose, projection index, and light index lists
        for (Camera cam : cameras)
        {
            // Apply the global transform to each camera
            Matrix4 m1 = cam.transform;
            Vector3 displacement = m1.getColumn(3).getXYZ();
            m1 = Matrix4.translate(displacement.times(1.0f / globalScale).minus(displacement)).times(m1);

            // TODO: Figure out the right way to integrate the global transforms
            cam.transform = m1.times(globalRotation)
                .times(Matrix4.translate(globalTranslate))
            ;//     .times(Matrix4.scale(globalScale));

            result.getCameraPoseList().add(cam.transform);

            // Compute inverse by just reversing steps to build transformation
            Matrix4 cameraPoseInv = //Matrix4.scale(1.0f / globalScale)
                                    /*       .times*/Matrix4.translate(globalTranslate.negated())
                .times(globalRotation.transpose())
                .times(m1.getUpperLeft3x3().transpose().asMatrix4())
                .times(Matrix4.translate(m1.getColumn(3).getXYZ().negated()));
            result.getCameraPoseInvList().add(cameraPoseInv);

            Matrix4 expectedIdentity = cameraPoseInv.times(cam.transform);
            boolean error = false;
            for (int r = 0; r < 4; r++)
            {
                for (int c = 0; c < 4; c++)
                {
                    float expectedValue;
                    if (r == c)
                    {
                        expectedValue = 1.0f;
                    }
                    else
                    {
                        expectedValue = 0.0f;
                    }

                    if (Math.abs(expectedIdentity.get(r, c) - expectedValue) > 0.001f)
                    {
                        error = true;
                        break;
                    }
                }
                if (error)
                {
                    break;
                }
            }

            if (error)
            {
                System.err.println("Warning: matrix inverse could not be computed correctly - transformation is not affine.");
                for (int r = 0; r < 4; r++)
                {
                    for (int c = 0; c < 4; c++)
                    {
                        System.err.print('\t' + String.format("%.3f", expectedIdentity.get(r, c)));
                    }
                    System.err.println();
                }
            }

            result.getCameraProjectionIndexList().add(cam.sensor.index);
            result.getLightIndexList().add(cam.lightIndex);
            result.getImageFileNames().add(cam.filename);
        }
        
        for (int i = 0; i < nextLightIndex; i++)
        {
            result.getLightPositionList().add(Vector3.ZERO);
            result.getLightIntensityList().add(Vector3.ZERO);
        }

        result.setRecommendedFarPlane(findFarPlane(result.getCameraPoseInvList()));
        result.setRecommendedNearPlane(result.getRecommendedFarPlane() / 32.0f);
        System.out.println("Near and far planes: " + result.getRecommendedNearPlane() + ", " + result.getRecommendedFarPlane());

        int primaryViewIndex = 0;
        String primaryViewName = cameras[0].filename;
        for (int i = 1; i < cameras.length; i++)
        {
            if (cameras[i].filename.compareTo(primaryViewName) < 0)
            {
                primaryViewName = cameras[i].filename;
                primaryViewIndex = i;
            }
        }
        
        result.setRootDirectory(file.getParentFile());
        result.setPrimaryView(primaryViewIndex);

        return result;
    }

    /**
     * A subroutine for guessing an appropriate far plane from an Agisoft PhotoScan XML file.
     * Assumes that the object must lie between all of the cameras in the file.
     * @param cameraPoseInvList The list of camera poses.
     * @return A far plane estimate.
     */
    private static float findFarPlane(List<Matrix4> cameraPoseInvList)
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
