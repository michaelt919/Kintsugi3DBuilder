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
import tetzlaff.gl.vecmath.Matrix3;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;

/**
 * A class representing a collection of photographs, or views.
 * @author Michael Tetzlaff
 */
public final class ViewSet
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
     * The reference linear luminance values used for decoding pixel colors.
     */
    private double[] linearLuminanceValues;

    /**
     * The reference encoded luminance values used for decoding pixel colors.
     */
    private byte[] encodedLuminanceValues;

    /**
     * A list containing the relative name of the image file corresponding to each view.
     */
    private final List<String> imageFileNames;

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
    private final boolean infiniteLightSources;

    /**
     * The recommended near plane to use when rendering this view set.
     */
    private final float recommendedNearPlane;

    /**
     * The recommended far plane to use when rendering this view set.
     */
    private final float recommendedFarPlane;

    /**
     * The index of the view that sets the initial orientation when viewing, is used for color calibration, etc.
     */
    private int primaryViewIndex = 0;

    private static class Parameters
    {
        List<Matrix4> cameraPoseList;
        List<Matrix4> cameraPoseInvList;
        List<Projection> cameraProjectionList;
        List<Integer> cameraProjectionIndexList;
        List<Vector3> lightPositionList;
        List<Vector3> lightIntensityList;
        List<Integer> lightIndexList;
        List<String> imageFileNames;
        String relativeImagePath;
        String geometryFileName;
        File directory;
        float gamma = 2.2f;
        boolean infiniteLightSources;
        double[] linearLuminanceValues;
        byte[] encodedLuminanceValues;
        float recommendedNearPlane;
        float recommendedFarPlane;
    }

    /**
     * Creates a new view set object.
     * @param params The parameters defining the new view set.
     */
    private ViewSet(Parameters params)
    {
        this.cameraPoseList = params.cameraPoseList;
        this.cameraPoseInvList = params.cameraPoseInvList;
        this.cameraProjectionList = params.cameraProjectionList;
        this.cameraProjectionIndexList = params.cameraProjectionIndexList;
        this.lightPositionList = params.lightPositionList;
        this.lightIntensityList = params.lightIntensityList;
        this.lightIndexList = params.lightIndexList;
        this.imageFileNames = params.imageFileNames;
        this.geometryFileName = params.geometryFileName;
        this.recommendedNearPlane = params.recommendedNearPlane;
        this.recommendedFarPlane = params.recommendedFarPlane;
        this.gamma = params.gamma;
        this.infiniteLightSources = params.infiniteLightSources;
        this.linearLuminanceValues = params.linearLuminanceValues;
        this.encodedLuminanceValues = params.encodedLuminanceValues;
        this.rootDirectory = params.directory;
        this.relativeImagePath = params.relativeImagePath;
    }

    public NativeVectorBuffer getCameraPoseData()
    {
        // Store the poses in a uniform buffer
        if (cameraPoseList != null && !cameraPoseList.isEmpty())
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

    public NativeVectorBuffer getCameraProjectionData()
    {
        // Store the camera projections in a uniform buffer
        if (cameraProjectionList != null && !cameraProjectionList.isEmpty())
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

    public NativeVectorBuffer getCameraProjectionIndexData()
    {
        // Store the camera projection indices in a uniform buffer
        if (cameraProjectionIndexList != null && !cameraProjectionIndexList.isEmpty())
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

    public NativeVectorBuffer getLightPositionData()
    {
        // Store the light positions in a uniform buffer
        if (lightPositionList != null && !lightPositionList.isEmpty())
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

    public NativeVectorBuffer getLightIntensityData()
    {
        // Store the light positions in a uniform buffer
        if (lightIntensityList != null && !lightIntensityList.isEmpty())
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

    public NativeVectorBuffer getLightIndexData()
    {
        // Store the light indices indices in a uniform buffer
        if (lightIndexList != null && !lightIndexList.isEmpty())
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

    /**
     * Loads a VSET file and creates a corresponding ViewSet object.
     * @param vsetFile The VSET file to load.
     * @return The newly created ViewSet object.
     * @throws FileNotFoundException Thrown if the view set file is not found.
     */
    public static ViewSet loadFromVSETFile(File vsetFile) throws FileNotFoundException
    {
        Date timestamp = new Date();

        Parameters params = new Parameters();

        params.gamma = 2.2f;
        params.recommendedNearPlane = 0.0f;
        params.recommendedFarPlane = Float.MAX_VALUE;

        List<Matrix4> unorderedCameraPoseList = new ArrayList<>();
        List<Matrix4> unorderedCameraPoseInvList = new ArrayList<>();

        params.cameraPoseList = new ArrayList<>();
        params.cameraPoseInvList = new ArrayList<>();
        params.cameraProjectionList = new ArrayList<>();
        params.lightPositionList = new ArrayList<>();
        params.lightIntensityList = new ArrayList<>();
        params.cameraProjectionIndexList = new ArrayList<>();
        params.lightIndexList = new ArrayList<>();
        params.imageFileNames = new ArrayList<>();

        List<Double> linearLuminanceList = new ArrayList<>();
        List<Byte> encodedLuminanceList = new ArrayList<>();

        params.geometryFileName = "manifold.obj";
        params.relativeImagePath = null;

        try(Scanner scanner = new Scanner(vsetFile))
        {
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
        return new ViewSet(params);
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
     * @return The newly created ViewSet object.
     * @throws FileNotFoundException Thrown if the XML camera file is not found.
     */
    public static ViewSet loadFromAgisoftXMLFile(File file) throws FileNotFoundException
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
        try
        {
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

                            case "property": case "projections": case "depth":
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
        }
        catch (XMLStreamException e)
        {
            e.printStackTrace();
        }
        
        Parameters params = new Parameters();

        // Initialize internal lists
        params.cameraPoseList = new ArrayList<>();
        params.cameraPoseInvList = new ArrayList<>();
        params.cameraProjectionList = new ArrayList<>();
        params.lightPositionList = new ArrayList<>();
        params.lightIntensityList = new ArrayList<>();
        params.cameraProjectionIndexList = new ArrayList<>();
        params.lightIndexList = new ArrayList<>();
        params.imageFileNames = new ArrayList<>();
        
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

        ViewSet returnValue = new ViewSet(params);
        returnValue.primaryViewIndex = primaryViewIndex;
        return returnValue;
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

    public void writeVSETFileToStream(OutputStream outputStream)
    {
        writeVSETFileToStream(outputStream, null);
    }

    public void writeVSETFileToStream(OutputStream outputStream, Path parentDirectory)
    {
        PrintStream out = new PrintStream(outputStream);
        out.println("# Created by IBRelight");

        out.println("\n# Geometry file name (mesh)");
        out.println("m " + (parentDirectory == null ? geometryFileName : parentDirectory.relativize(getGeometryFile().toPath())));
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
    
    

    /**
     * Gets the camera pose defining the transformation from object space to camera space for a particular view.
     * @param poseIndex The index of the camera pose to retrieve.
     * @return The camera pose as a 4x4 affine transformation matrix.
     */
    public Matrix4 getCameraPose(int poseIndex)
    {
        return this.cameraPoseList.get(poseIndex);
    }

    /**
     * Gets the inverse of the camera pose, defining the transformation from camera space to object space for a particular view.
     * @param poseIndex The index of the camera pose to retrieve.
     * @return The inverse camera pose as a 4x4 affine transformation matrix.
     */
    public Matrix4 getCameraPoseInverse(int poseIndex)
    {
        return this.cameraPoseInvList.get(poseIndex);
    }

    /**
     * Gets the root directory for this view set.
     * @return The root directory.
     */
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

    /**
     * Gets the name of the geometry file associated with this view set.
     * @return The name of the geometry file.
     */
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

    /**
     * Gets the geometry file associated with this view set.
     * @return The geometry file.
     */
    public File getGeometryFile()
    {
        return new File(this.rootDirectory, geometryFileName);
    }

    /**
     * Gets the image file path associated with this view set.
     * @return The image file path.
     */
    public File getImageFilePath()
    {
        return this.relativeImagePath == null ? this.rootDirectory : new File(this.rootDirectory, relativeImagePath);
    }

    /**
     * Sets the image file path associated with this view set.
     * @return imageFilePath The image file path.
     */
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

    /**
     * Gets the relative name of the image file corresponding to a particular view.
     * @param poseIndex The index of the image file to retrieve.
     * @return The image file's relative name.
     */
    public String getImageFileName(int poseIndex)
    {
        return this.imageFileNames.get(poseIndex);
    }

    /**
     * Gets the image file corresponding to a particular view.
     * @param poseIndex The index of the image file to retrieve.
     * @return The image file.
     */
    public File getImageFile(int poseIndex)
    {
        return new File(this.getImageFilePath(), this.imageFileNames.get(poseIndex));
    }
    
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

    /**
     * Gets the projection transformation defining the intrinsic properties of a particular camera.
     * @param projectionIndex The index of the camera whose projection transformation is to be retrieved.
     * IMPORTANT: this is NOT usually the same as the index of the view to be retrieved.
     * @return The projection transformation.
     */
    public Projection getCameraProjection(int projectionIndex)
    {
        return this.cameraProjectionList.get(projectionIndex);
    }

    /**
     * Gets the index of the projection transformation to be used for a particular view,
     * which can subsequently be used with getCameraProjection() to obtain the corresponding projection transformation itself.
     * @param poseIndex The index of the view.
     * @return The index of the projection transformation.
     */
    public Integer getCameraProjectionIndex(int poseIndex)
    {
        return this.cameraProjectionIndexList.get(poseIndex);
    }

    /**
     * Gets the position of a particular light source.
     * Used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
     * Assumed by convention to be in camera space.
     * @param lightIndex The index of the light source.
     * IMPORTANT: this is NOT usually the same as the index of the view to be retrieved.
     * @return The position of the light source.
     */
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

    /**
     * Gets the index of the light source to be used for a particular view,
     * which can subsequently be used with getLightPosition() and getLightIntensity() to obtain the actual position and intensity of the light source.
     * @param poseIndex The index of the view.
     * @return The index of the light source.
     */
    public Integer getLightIndex(int poseIndex)
    {
        return this.lightIndexList.get(poseIndex);
    }

    /**
     * Gets the number of camera poses defined in this view set.
     * @return The number of camera poses defined in this view set.
     */
    public int getCameraPoseCount()
    {
        return this.cameraPoseList.size();
    }

    /**
     * Gets the number of projection transformations defined in this view set.
     * @return The number of projection transformations defined in this view set.
     */
    public int getCameraProjectionCount()
    {
        return this.cameraProjectionList.size();
    }

    /**
     * Gets the number of lights defined in this view set.
     * @return The number of projection transformations defined in this view set.
     */
    public int getLightCount()
    {
        return this.lightPositionList.size();
    }

    /**
     * Gets the recommended near plane to use when rendering this view set.
     * @return The near plane value.
     */
    public float getRecommendedNearPlane()
    {
        return this.recommendedNearPlane;
    }

    /**
     * Gets the recommended far plane to use when rendering this view set.
     * @return The far plane value.
     */
    public float getRecommendedFarPlane()
    {
        return this.recommendedFarPlane;
    }

    public float getGamma()
    {
        return gamma;
    }

    public boolean hasCustomLuminanceEncoding()
    {
        return linearLuminanceValues != null && encodedLuminanceValues != null
            && linearLuminanceValues.length > 0 && encodedLuminanceValues.length > 0;
    }

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

    public boolean areLightSourcesInfinite()
    {
        return infiniteLightSources;
    }
}
