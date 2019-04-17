/*
 * Copyright (c) 2019
 * The Regents of the University of Minnesota
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import umn.gl.vecmath.Matrix3;
import umn.gl.vecmath.Matrix4;
import umn.gl.vecmath.Vector3;
import umn.gl.vecmath.Vector4;

/**
 * A class representing a collection of photographs, or views.
 * @author Michael Tetzlaff
 */
public final class ViewSetImpl extends ViewSetBase
{
    /**
     * A list containing the relative name of the image file corresponding to each view.
     */
    private final List<String> imageFileNames;

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
     * A list containing an entry for every view which designates the index of the light source position and intensity that should be used for each view.
     */
    private final List<Integer> lightIndexList;

    /**
     * The recommended near plane to use when rendering this view set.
     */
    private final float recommendedNearPlane;

    /**
     * The recommended far plane to use when rendering this view set.
     */
    private final float recommendedFarPlane;

    public static class Parameters extends ParametersBase
    {
        public final List<Matrix4> cameraPoseList = new ArrayList<>(128);
        public final List<Matrix4> cameraPoseInvList = new ArrayList<>(128);
        public final List<Projection> cameraProjectionList = new ArrayList<>(128);
        public final List<Integer> cameraProjectionIndexList = new ArrayList<>(128);
        public final List<Integer> lightIndexList = new ArrayList<>(128);
        public final List<String> imageFileNames = new ArrayList<>(128);
    }

    /**
     * Creates a new view set object.
     * @param params The parameters defining the new view set.
     */
    public ViewSetImpl(Parameters params)
    {
        super(params);
        this.cameraPoseList = params.cameraPoseList;
        this.cameraPoseInvList = params.cameraPoseInvList;
        this.cameraProjectionList = params.cameraProjectionList;
        this.cameraProjectionIndexList = params.cameraProjectionIndexList;
        this.lightIndexList = params.lightIndexList;
        this.recommendedNearPlane = params.recommendedNearPlane;
        this.recommendedFarPlane = params.recommendedFarPlane;
        this.imageFileNames = params.imageFileNames;
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
    public void setPrimaryView(String viewName)
    {
        int poseIndex = this.imageFileNames.indexOf(viewName);
        if (poseIndex >= 0)
        {
            this.setPrimaryView(poseIndex);
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
    public float getRecommendedNearPlane()
    {
        return this.recommendedNearPlane;
    }

    @Override
    public float getRecommendedFarPlane()
    {
        return this.recommendedFarPlane;
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

    /**
     * Loads a VSET file and creates a corresponding ViewSetImpl object.
     * @param vsetFile The VSET file to load.
     * @return The newly created ViewSetImpl object.
     * @throws FileNotFoundException Thrown if the view set file is not found.
     */
    public static ViewSet loadFromVSETFile(File vsetFile) throws FileNotFoundException
    {
        Date timestamp = new Date();

        Parameters params = new Parameters();

        params.gamma = 2.2f;
        params.recommendedNearPlane = 0.0f;
        params.recommendedFarPlane = Float.MAX_VALUE;

        params.geometryFileName = "manifold.obj";
        params.relativeImagePath = null;

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
                        params.recommendedNearPlane = scanner.nextFloat();
                        params.recommendedFarPlane = scanner.nextFloat();
                        scanner.nextLine();
                        break;
                    }
                    case "m":
                    {
                        params.geometryFileName = scanner.nextLine().trim();
                        break;
                    }
                    case "i":
                    {
                        params.relativeImagePath = scanner.nextLine().trim();
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

                        params.cameraProjectionList.add(new DistortionProjection(
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
                        params.gamma = scanner.nextFloat();
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

                        params.cameraProjectionList.add(new SimpleProjection(aspect, fovy));

                        scanner.nextLine();
                        break;
                    }
                    case "l":
                    {
                        float x = scanner.nextFloat();
                        float y = scanner.nextFloat();
                        float z = scanner.nextFloat();
                        params.lightPositionList.add(new Vector3(x, y, z));

                        float r = scanner.nextFloat();
                        float g = scanner.nextFloat();
                        float b = scanner.nextFloat();
                        params.lightIntensityList.add(new Vector3(r, g, b));

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

                        params.cameraPoseList.add(unorderedCameraPoseList.get(poseId));
                        params.cameraPoseInvList.add(unorderedCameraPoseInvList.get(poseId));
                        params.cameraProjectionIndexList.add(projectionId);
                        params.lightIndexList.add(lightId);
                        params.imageFileNames.add(imgFilename);
                        break;
                    }
                    default:
                        // Skip unrecognized line
                        scanner.nextLine();
                }
            }
        }

        params.linearLuminanceValues = new double[linearLuminanceList.size()];
        for (int i = 0; i < params.linearLuminanceValues.length; i++)
        {
            params.linearLuminanceValues[i] = linearLuminanceList.get(i);
        }

        params.encodedLuminanceValues = new byte[encodedLuminanceList.size()];
        for (int i = 0; i < params.encodedLuminanceValues.length; i++)
        {
            params.encodedLuminanceValues[i] = encodedLuminanceList.get(i);
        }

        System.out.println("View Set file loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        params.directory = vsetFile.getParentFile();
        return new ViewSetImpl(params);
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
          Camera otherCam = (Camera)other;
          return this.id.equals(otherCam.id);
        }
    }

    /**
     * Loads a camera definition file exported in XML format from Agisoft PhotoScan.
     * @param file The Agisoft PhotoScan XML camera file to load.
     * @return The newly created ViewSetImpl object.
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
                                    Objects.equals(reader.getAttributeValue(null, "enabled"), "1"))
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
        
        Parameters params = new Parameters();
        
        Sensor[] sensors = sensorSet.values().toArray(new Sensor[0]);
        
        // Reassign the ID for each sensor to correspond with the sensor's index
        // and add the corresponding projection to the list.
        for (int i = 0; i < sensors.length; i++)
        {
            sensors[i].index = i;
            params.cameraProjectionList.add(new DistortionProjection(
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
                
        Camera[] cameras = cameraSet.toArray(new Camera[cameraSet.size()]);
        
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

            params.cameraPoseList.add(cam.transform);

            // Compute inverse by just reversing steps to build transformation
            Matrix4 cameraPoseInv = //Matrix4.scale(1.0f / globalScale)
                                    /*       .times*/Matrix4.translate(globalTranslate.negated())
                .times(globalRotation.transpose())
                .times(m1.getUpperLeft3x3().transpose().asMatrix4())
                .times(Matrix4.translate(m1.getColumn(3).getXYZ().negated()));
            params.cameraPoseInvList.add(cameraPoseInv);

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

            params.cameraProjectionIndexList.add(cam.sensor.index);
            params.lightIndexList.add(cam.lightIndex);
            params.imageFileNames.add(cam.filename);
        }
        
        for (int i = 0; i < nextLightIndex; i++)
        {
            params.lightPositionList.add(Vector3.ZERO);
            params.lightIntensityList.add(new Vector3(1.0f));
        }
        params.infiniteLightSources = true; // TODO Could be set to false if support for automatically computing light intensities based on camera distance is added.

        params.recommendedFarPlane = findFarPlane(params.cameraPoseInvList);
        params.recommendedNearPlane = params.recommendedFarPlane / 32.0f;
        System.out.println("Near and far planes: " + params.recommendedNearPlane + ", " + params.recommendedFarPlane);

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
        
        params.directory = file.getParentFile();

        ViewSetImpl returnValue = new ViewSetImpl(params);
        returnValue.setPrimaryView(primaryViewIndex);
        return returnValue;
    }
}
