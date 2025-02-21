/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.io;

import java.io.File;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import kintsugi3d.builder.core.DistortionProjection;
import kintsugi3d.builder.core.SimpleProjection;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.metrics.ViewRMSE;
import kintsugi3d.gl.vecmath.Matrix3;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles loading view sets from a camera definition file exported in XML format from Agisoft PhotoScan/Metashape.
 */
public final class ViewSetReaderFromAgisoftXML implements ViewSetReader
{
    private static final Logger log = LoggerFactory.getLogger(ViewSetReaderFromAgisoftXML.class);

    private static final ViewSetReader INSTANCE = new ViewSetReaderFromAgisoftXML();

    public static ViewSetReader getInstance()
    {
        return INSTANCE;
    }

    private ViewSetReaderFromAgisoftXML()
    {
    }

    /**
     * A private class for representing a "sensor" in an Agisoft PhotoScan/Metashape XML file.
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
     * A private class for representing a "camera" in an Agisoft PhotoScan/Metashape XML file.
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
        public boolean equals(Object obj)
        {
            if (obj instanceof Camera)
            {
                Camera otherCam = (Camera) obj;
                return this.id.equals(otherCam.id);
            }
            else
            {
                return false;
            }
        }
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


    /**
     * Loads a view set from an input file.
     * The root directory and the supporting files directory will be set as specified.
     * The supporting files directory may be overridden by a directory specified in the file.
     * * @param stream The file to load
     * @param root
     * @param supportingFilesDirectory
     * @param imagePathMap A map of image IDs to paths, if passed this will override the paths being assigned to the images.
     * @return
     * @throws XMLStreamException
     */
    public ViewSet readFromStream(InputStream stream, File root, File supportingFilesDirectory,
        Map<Integer, String> imagePathMap, boolean needsUndistortion) throws XMLStreamException
    {
        return readFromStream(stream, root, supportingFilesDirectory, imagePathMap,
            needsUndistortion, -1, false);
    }

    /**
     * Loads a view set from an input file.
     * The root directory and the supporting files directory will be set as specified.
     * The supporting files directory may be overridden by a directory specified in the file.
     * * @param stream
     * @param root
     * @param supportingFilesDirectory
     * @param imagePathMap A map of image IDs to paths, if passed this will override the paths being assigned to the images.
     * @param needsUndistortion Whether or not the images need undistortion.  Should be true if loading original photos,
     *                          or false if loading images that have already been undistorted by photogrammetry software.
     * @param metashapeVersionOverride A parameter that can be passed to override the version of the XML document being read to circumvent formatting differences.
     * @param directAgisoftImport Used to ignore global transformations set in Metashape projects which would break rendering if not accounted for.

     * @return
     * @throws XMLStreamException
     */
    public ViewSet readFromStream(InputStream stream, File root, File supportingFilesDirectory, Map<Integer, String> imagePathMap,
        boolean needsUndistortion, int metashapeVersionOverride, boolean directAgisoftImport) throws XMLStreamException
    {
        Map<String, Sensor> sensorSet = new Hashtable<>();
        TreeSet<Camera> cameraSet = new TreeSet<>((c1, c2) ->
        {
            // Attempt to sort the camera IDs which are probably integers but not guaranteed to be.
            try
            {
                int id1 = Integer.parseInt(c1.id);

                try
                {
                    // Both are integers; compare as numbers.
                    int id2 = Integer.parseInt(c2.id);
                    return Integer.compare(id1, id2);
                }
                catch (NumberFormatException e)
                {
                    // id1 is a number but id2 isn't
                    return -1;
                }
            }
            catch (NumberFormatException e)
            {
                try
                {
                    int id2 = Integer.parseInt(c2.id);

                    // id2 is a number but id1 isn't
                    return 1;
                }
                catch (NumberFormatException e2)
                {
                    // Neither are numbers; compare as strings.
                    return c1.id.compareTo(c2.id);
                }
            }
        });

        Sensor sensor = null;
        Camera camera = null;
        int lightIndex = -1;
        int nextLightIndex = 0;
        int defaultLightIndex = -1;

        float globalScale = 1.0f;
        Matrix4 globalRotation = Matrix4.IDENTITY;
        Vector3 globalTranslate = new Vector3(0.0f, 0.0f, 0.0f);

        float b1 = 0.0f; // fx - fy: https://www.agisoft.com/forum/index.php?topic=6437.0

        String version = "";
        String chunkLabel = "";
        String groupLabel = "";
        String sensorID = "";
        String cameraID = "";
        String imageFile = "";
        int intVersion = Math.max(metashapeVersionOverride, 0);

        XMLInputFactory factory = XMLInputFactory.newInstance();

        XMLStreamReader reader = factory.createXMLStreamReader(stream);
        while (reader.hasNext())
        {
            int event = reader.next();
            switch (event)
            {
                case XMLStreamConstants.START_ELEMENT:
                    switch (reader.getLocalName())
                    {
                        case "document":
                        {
                            version = reader.getAttributeValue(null, "version");
                            String[] verComponents = version.split("\\.");
                            for (String verComponent : verComponents)
                            {
                                intVersion *= 10;
                                intVersion += Integer.parseInt(verComponent);
                            }
                            log.debug("Agisoft XML version {} ({})\n", version, intVersion);
                            break;
                        }
                        case "chunk":
                        {
                            chunkLabel = reader.getAttributeValue(null, "label");
                            if (chunkLabel == null)
                            {
                                chunkLabel = "unnamed";
                            }
                            log.debug("Reading chunk '{}'\n", chunkLabel);

                            // Commented out; chunk XMLs seem to always be labelled version 1.2.0; regardless of Metashape version or actual format details.
//                            // chunk XMLs put the version in the chunk tag
//                            String tryVersion = reader.getAttributeValue(null, "version");
//                            if (tryVersion != null)
//                            {
//                                version = tryVersion;
//                                String[] verComponents = version.split("\\.");
//                                for (String verComponent : verComponents)
//                                {
//                                    intVersion *= 10;
//                                    intVersion += Integer.parseInt(verComponent);
//                                }
//                            }

                            break;
                        }
                        case "group":
                            groupLabel = reader.getAttributeValue(null, "label");
                            log.debug("Reading group '{}'\n", groupLabel);
                            lightIndex = nextLightIndex;
                            nextLightIndex++;
                            log.debug("Light index: " + lightIndex);
                            break;
                        case "sensor":
                            sensorID = reader.getAttributeValue(null, "id");
                            log.debug("\tAdding sensor '{}'\n", sensorID);
                            sensor = new Sensor(sensorID);
                            break;
                        case "camera":
                            cameraID = reader.getAttributeValue(null, "id");
                            if (cameraID == null || cameraSet.contains(new Camera(cameraID)))
                            {
                                camera = null;
                            }
                            else
                            {
                                if (Objects.equals(reader.getAttributeValue(null, "enabled"), "true") ||
                                    Objects.equals(reader.getAttributeValue(null, "enabled"), "1") ||
                                    Objects.equals(reader.getAttributeValue(null, "enabled"), null))
                                {
                                    sensorID = reader.getAttributeValue(null, "sensor_id");
                                    imageFile = reader.getAttributeValue(null, "label");

                                    if (sensorID != null && imageFile != null)
                                    {
                                        if (lightIndex < 0)
                                        {
                                            // Set default light index
                                            lightIndex = defaultLightIndex = nextLightIndex;
                                            nextLightIndex++;
                                            log.debug("Using default light index: " + lightIndex);
                                        }

                                        camera = new Camera(cameraID, sensorSet.get(sensorID), lightIndex);
                                        camera.filename = imageFile;
                                    }
                                    else
                                    {
                                        // Camera is incomplete for use as a calibrated photo (i.e. keyframe)
                                        camera = null;
                                    }
                                }
                                else
                                {
                                    // Camera is disabled
                                    camera = null;
                                }

                            }
                            break;
                        case "orientation":
                            if (camera != null)
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
                                sensor.fy = Float.parseFloat(reader.getElementText());
                                sensor.fx = sensor.fy + b1;
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
                        case "b1":
                            b1 = Float.parseFloat(reader.getElementText());
                            if (sensor != null)
                            {
                                sensor.fx = sensor.fy + b1;
                            }
                            break;
                        case "b2": // See https://www.agisoft.com/forum/index.php?topic=6437.0
                        case "skew":
                            if (sensor != null)
                            {
                                sensor.skew = Float.parseFloat(reader.getElementText());
                            }
                            break;
                        case "transform":
                            if (camera == null && intVersion >= 110)
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
                                if ("rotation".equals(reader.getLocalName()))
                                {
                                    expectedSize = 9;
                                }

                                if (components.length > expectedSize)
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
                                    if (expectedSize == 9)
                                    {
                                        trans = Matrix3.fromRows(
                                                new Vector3(m[0], m[3], m[6]),
                                                new Vector3(-m[1], -m[4], -m[7]),
                                                new Vector3(-m[2], -m[5], -m[8]))
                                            .asMatrix4();
                                    }
                                    else
                                    {
                                        trans = Matrix3.fromRows(
                                                new Vector3(m[0], m[4], m[8]),
                                                new Vector3(-m[1], -m[5], -m[9]),
                                                new Vector3(-m[2], -m[6], -m[10]))
                                            .asMatrix4()
                                            .times(Matrix4.translate(-m[3], -m[7], -m[11]));
                                    }

                                    camera.transform = trans;
                                }
                                else
                                {
                                    if (!directAgisoftImport){
                                        if (expectedSize == 9) {
                                            log.debug("\tSetting global rotation.");
                                            globalRotation = Matrix3.fromRows(
                                                            new Vector3(m[0], m[3], m[6]),
                                                            new Vector3(m[1], m[4], m[7]),
                                                            new Vector3(m[2], m[5], m[8]))
                                                    .asMatrix4();
                                        } else {
                                            log.debug("\tSetting global transformation.");
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
                        }
                        break;

                        case "translation":
                            if (camera == null && !directAgisoftImport)
                            {
                                log.debug("\tSetting global translate.");
                                String[] components = reader.getElementText().split("\\s");
                                globalTranslate = new Vector3(
                                    -Float.parseFloat(components[0]),
                                    -Float.parseFloat(components[1]),
                                    -Float.parseFloat(components[2]));
                            }
                            break;

                        case "scale":
                            if (camera == null && !directAgisoftImport)
                            {
                                log.debug("\tSetting global scale.");
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
                        case "projections":
                        case "depth":
                        case "frames":
                        case "frame":
                        case "meta":
                        case "R":
                        case "size":
                        case "center":
                        case "region":
                        case "settings":
                        case "ground_control":
                        case "mesh":
                        case "texture":
                        case "model":
                        case "calibration":
                        case "thumbnail":
                        case "point_cloud":
                        case "points":
                        case "sensors":
                        case "cameras":
                            // These can all be safely ignored if version is >= 0.9.1
                            break;

                        case "photo":
                        case "tracks":
                        case "depth_maps":
                        case "depth_map":
                        case "dense_cloud":
                            if (intVersion < 110)
                            {
                                log.debug("Unexpected tag \'{}\' for psz version {}\n",
                                    reader.getLocalName(), version);
                            }
                            break;

                        default:
                            log.debug("Unexpected tag '{}'\n", reader.getLocalName());
                            break;
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                {
                    switch (reader.getLocalName())
                    {
                        case "chunk":
                            log.debug("Finished chunk '{}'\n", chunkLabel);
                            chunkLabel = "";
                            break;
                        case "group":
                            log.debug("Finished group '{}'\n", groupLabel);
                            groupLabel = "";
                            lightIndex = defaultLightIndex;
                            break;
                        case "sensor":
                            if (sensor != null)
                            {
                                sensorSet.put(sensor.id, sensor);
                                sensor = null;
                            }
                            break;
                        case "camera":
                            if (camera != null)
                            {
                                if (camera.transform != null)
                                {
                                    // Only add camera if it has a valid transform
                                    cameraSet.add(camera);
                                    log.debug("\tAdding camera {}, with sensor {} and image {}\n",
                                        cameraID, sensorID, imageFile);
                                }
                                camera = null; // Clear the camera regardless
                            }
                            break;
                    }
                }
                break;
            }
        }

        ViewSet result = new ViewSet(cameraSet.size());
        result.setRootDirectory(root);
        result.setSupportingFilesDirectory(supportingFilesDirectory);

        Sensor[] sensors = sensorSet.values().toArray(new Sensor[0]);

        if (intVersion >= 140) // centers were pre-offset prior to version 1.4
        {
            for (Sensor s : sensors)
            {
                s.cx += s.width * 0.5;
                s.cy += s.height * 0.5;
            }
        }

        // Reassign the ID for each sensor to correspond with the sensor's index
        // and add the corresponding projection to the list.
        for (int i = 0; i < sensors.length; i++)
        {
            sensors[i].index = i;

            DistortionProjection distortionProjection = new DistortionProjection(
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
            );

            if (needsUndistortion)
            {
                result.getCameraProjectionList().add(distortionProjection);
            }
            else
            {
                result.getCameraProjectionList().add(new SimpleProjection(
                    distortionProjection.getAspectRatio(), distortionProjection.getVerticalFieldOfView()));
            }
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

            if (imagePathMap != null && imagePathMap.containsKey(Integer.parseInt(cam.id))) {
                result.getImageFiles().add(new File(imagePathMap.get(Integer.parseInt(cam.id))));
            }else{
                result.getImageFiles().add(new File(cam.filename));
                if (imagePathMap != null) {
                    log.error("Camera path override not found for camera: " + cam.id);
                }
            }

            result.getViewErrorMetrics().add(new ViewRMSE());
        }

        for (int i = 0; i < nextLightIndex; i++)
        {
            result.getLightPositionList().add(Vector3.ZERO);
            result.getLightIntensityList().add(Vector3.ZERO);
        }

        result.setRecommendedFarPlane(findFarPlane(result.getCameraPoseInvList()));
        result.setRecommendedNearPlane(result.getRecommendedFarPlane() / 32.0f);
        log.debug("Near and far planes: " + result.getRecommendedNearPlane() + ", " + result.getRecommendedFarPlane());

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

        result.setPrimaryViewIndex(primaryViewIndex);

        return result;
    }
}
