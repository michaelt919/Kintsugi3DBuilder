/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.io;

import kintsugi3d.builder.core.DistortionProjection;
import kintsugi3d.builder.core.Projection;
import kintsugi3d.builder.core.SimpleProjection;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.core.ViewSet.Builder;
import kintsugi3d.builder.io.metashape.MetashapeChunk;
import kintsugi3d.builder.resources.project.MissingImagesException;
import kintsugi3d.gl.vecmath.Matrix3;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;
import kintsugi3d.util.UnzipHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Handles loading view sets from a camera definition file exported in XML format from Agisoft PhotoScan/Metashape.
 */
public final class ViewSetReaderFromAgisoftXML implements ViewSetReader
{
    private static final Logger LOG = LoggerFactory.getLogger(ViewSetReaderFromAgisoftXML.class);

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
     *
     * @author Michael Tetzlaff
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
     *
     * @author Michael Tetzlaff
     */
    private static class Camera
    {
        final String id;
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

        @Override
        public int hashCode()
        {
            return Objects.hashCode(id);
        }
    }

    /**
     * Loads a view set from an input file.
     * The root directory will be set as specified.
     * The supporting files directory will default to the root directory.
     *
     * @param stream The file to load
     * @return
     * @throws XMLStreamException
     */
    @Override
    public Builder readFromStream(InputStream stream, ViewSetDirectories directories) throws XMLStreamException
    {
        if (directories.supportingFilesDirectory == null)
        {
            directories.supportingFilesDirectory = directories.projectRoot;
        }

        return readFromStream(stream, directories, null, null, null, -1, false);
    }

    /**
     * Loads a view set from an input file.
     * The root directory and the supporting files directory will be set as specified.
     * The supporting files directory may be overridden by a directory specified in the file.
     * * @param stream
     *
     * @param imagePathMap             A map of image IDs to paths, if passed this will override the paths being assigned to the images.
     * @param metashapeVersionOverride A parameter that can be passed to override the version of the XML document being read to circumvent formatting differences.
     * @param ignoreGlobalTransforms   Used to ignore global transformations set in Metashape projects which would break rendering if not accounted for.
     * @return
     * @throws XMLStreamException
     */
    public static Builder readFromStream(InputStream stream, ViewSetDirectories directories, String modelID,
        Map<Integer, String> imagePathMap, Map<Integer, String> maskPathMap, int metashapeVersionOverride, boolean ignoreGlobalTransforms)
        throws XMLStreamException
    {
        Map<String, Sensor> sensorSet = new HashMap<>(16);
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
                    Integer.parseInt(c2.id);

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

        Matrix4 globalTransform = Matrix4.IDENTITY;
        float globalScale = 1.0f;
        Vector3 globalTranslate = new Vector3(0.0f, 0.0f, 0.0f);

        String currentModelID = null; // takes on the model ID while within a <model> tag
        Matrix4 modelTransform = Matrix4.IDENTITY;

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
                            LOG.debug("Agisoft XML version {} ({})", version, intVersion);
                            break;
                        }
                        case "chunk":
                        {
                            chunkLabel = reader.getAttributeValue(null, "label");
                            if (chunkLabel == null)
                            {
                                chunkLabel = "unnamed";
                            }
                            LOG.debug("Reading chunk '{}'", chunkLabel);

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
                            LOG.debug("Reading group '{}'", groupLabel);
                            lightIndex = nextLightIndex;
                            nextLightIndex++;
                            LOG.debug("Light index: {}", lightIndex);
                            break;
                        case "sensor":
                            sensorID = reader.getAttributeValue(null, "id");
                            LOG.debug("\tAdding sensor '{}'", sensorID);
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
                                            LOG.debug("Using default light index: {}", lightIndex);
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
                            if (currentModelID == null && camera == null && intVersion >= 110)
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
                                else if (modelID != null && Objects.equals(currentModelID, modelID))
                                {
                                    // model transform (nested within a <model> tag)
                                    // This is only expected for PSX import (not exported XML files)
                                    // See Mia's shishi model for an example of where this is used.
                                    // I don't know if we've actually tested for any models where the upper-left 3x3 was anything other than identity
                                    // -- but I think this is NOT supposed to be inverted
                                    // (previously it was partially inverted where the rotation-scale was transposed,
                                    // and the translation was applied first but NOT negated -- which would yield
                                    // the same result as not inverting if the matrix was pure translation)
                                    if (expectedSize == 9)
                                    {
                                        LOG.debug("\tSetting model rotation.");
                                        modelTransform = Matrix3.fromColumns(
                                                new Vector3(m[0], m[3], m[6]),
                                                new Vector3(m[1], m[4], m[7]),
                                                new Vector3(m[2], m[5], m[8]))
                                            .asMatrix4();
                                    }
                                    else
                                    {
                                        LOG.debug("\tSetting model transformation.");
                                        modelTransform = Matrix4.fromColumns(
                                            new Vector4(m[0], m[4], m[8], 0),
                                            new Vector4(m[1], m[5], m[9], 0),
                                            new Vector4(m[2], m[6], m[10], 0),
                                            new Vector4(m[3], m[7], m[11], 1));
                                    }
                                }
                                else
                                {
                                    if (expectedSize == 9)
                                    {
                                        LOG.debug("\tSetting global rotation.");
                                        globalTransform = Matrix3.fromColumns(
                                                new Vector3(m[0], m[3], m[6]),
                                                new Vector3(m[1], m[4], m[7]),
                                                new Vector3(m[2], m[5], m[8]))
                                            .asMatrix4();
                                    }
                                    else
                                    {
                                        LOG.debug("\tSetting global transformation.");

                                        globalTransform = Matrix4.fromColumns(
                                            new Vector4(m[0], m[4], m[8], 0),
                                            new Vector4(m[1], m[5], m[9], 0),
                                            new Vector4(m[2], m[6], m[10], 0),
                                            new Vector4(m[3], m[7], m[11], 1));

//                                        Matrix3 rotationScaleTranspose = Matrix3.fromRows(
//                                            new Vector3(m[0], m[4], m[8]),
//                                            new Vector3(m[1], m[5], m[9]),
//                                            new Vector3(m[2], m[6], m[10]));
//                                        float scale = rotationScaleTranspose.determinant();
//
//                                        // Note: for the longest time this translation was not negated,  nor was the scale inverted
//                                        // and I'm not sure if 4x4 global transforms actually exist in real Metashape files
//                                        // (typically they're separated as rotation / translation / scale -- going back even to old light field datasets)
//                                        // but I'm pretty sure this is basically inverting the matrix
//                                        // (transposing the rotation and applying the inverse translation first)
//                                        // so these other inverse steps should be necessary.
//                                        globalTransform = rotationScaleTranspose.times(1.0f / (scale * scale)).asMatrix4()
//                                            .times(Matrix4.translate(-m[3], -m[7], -m[11]));
                                    }
                                }
                            }
                        }
                        break;

                        case "translation":
                            if (camera == null)
                            {
                                LOG.debug("\tSetting global translate.");
                                String[] components = reader.getElementText().split("\\s");
                                globalTranslate = new Vector3(
                                    Float.parseFloat(components[0]),
                                    Float.parseFloat(components[1]),
                                    Float.parseFloat(components[2]));
                            }
                            break;

                        case "scale":
                            if (camera == null)
                            {
                                LOG.debug("\tSetting global scale.");
                                globalScale = Float.parseFloat(reader.getElementText());
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
                        case "model":
                            currentModelID = reader.getAttributeValue(null, "id");
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
                                LOG.debug("Unexpected tag '{}' for psz version {}", reader.getLocalName(), version);
                            }
                            break;

                        default:
//                            LOG.debug("Unexpected tag '{}'", reader.getLocalName());
                            break;
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                {
                    switch (reader.getLocalName())
                    {
                        case "chunk":
                            LOG.debug("Finished chunk '{}'", chunkLabel);
                            chunkLabel = "";
                            break;
                        case "group":
                            LOG.debug("Finished group '{}'", groupLabel);
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
                                    LOG.debug("\tAdding camera {}, with sensor {} and image {}",
                                        cameraID, sensorID, imageFile);
                                }
                                camera = null; // Clear the camera regardless
                            }
                            break;
                        case "model":
                            currentModelID = null;
                            break;
                    }
                }
                break;
            }
        }

        Builder builder = ViewSet.getBuilder(directories.projectRoot, directories.supportingFilesDirectory, cameraSet.size());

        Sensor[] sensors = sensorSet.values().toArray(new Sensor[0]);

        if (intVersion >= 140) // centers were pre-offset prior to version 1.4
        {
            for (Sensor s : sensors)
            {
                s.cx += s.width * 0.5f;
                s.cy += s.height * 0.5f;
            }
        }

        // Reassign the ID for each sensor to correspond with the sensor's index
        // and add the corresponding projection to the list.
        for (int i = 0; i < sensors.length; i++)
        {
            sensors[i].index = i;
            Projection distortionProj = new DistortionProjection(
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

            if (directories.fullResImagesNeedUndistort)
            {
                builder.addCameraProjection(distortionProj);
            }
            else
            {
                builder.addCameraProjection(new SimpleProjection(
                    distortionProj.getAspectRatio(), distortionProj.getVerticalFieldOfView()));
            }
        }

        // Try to separate rotation and scale from the model transform
        // Scale based on determinant
        float modelScale = modelTransform.getUpperLeft3x3().determinant();

        // If non-zero, remove scale from the rest of the model transform
        Matrix4 modelTransformNoScale;
        if (modelScale == 0.0f)
        {
            modelTransformNoScale = modelTransform;
        }
        else
        {
            modelTransformNoScale = Matrix4.scale(1.0f / modelScale).times(modelTransform);
        }

        // Do the same to extract global scale from transform
        float globalScaleFromTransform = globalTransform.getUpperLeft3x3().determinant();

        Matrix4 globalTransformNoScale;
        if (globalScaleFromTransform == 0.0f)
        {
            globalTransformNoScale = globalTransform;
        }
        else
        {
            globalTransformNoScale = Matrix4.scale(1.0f / globalScaleFromTransform).times(globalTransform);
        }

        // "World transform" for projective texture mapping might be based on "global" transform
        // (typical for XML import, need to account for transformation from camera optimization space to XML export space)
        // or could be based on "model" transform (typical for PSX import, transformation from raw PLY space to camera optimization space
        //  -- often identity but not always especially with an imported and/or aligned model).
        Matrix4 projTexWorldTransform;

        // Similarly, keep track of the scale that we want to ultimately apply when exporting.
        float projTexWorldScaleCombined;

        if (ignoreGlobalTransforms)
        {
            // PSX import typically
            // modelTransform includes, translation, rotation, and scale.
            // We do the model rotation and translation to get the orientation and location right for projective texture mapping,
            // but we keep the scale at the imported model scale
            // Order of transformation is:
            // 1. "model" transformation from internal PLY space to global camera optimization space
            // 2. camera transformation from global camera optimization space to individual camera POV
            projTexWorldTransform = modelTransformNoScale;
            projTexWorldScaleCombined = modelScale;

            // Transform object all the way from imported PLY space, first to camera optimization space
            // and then to the space that would be used for OBJ export

            // Scale may come from the globalTransform and modelTransform matrices, or from a separate global scale attribute.
            // All three should be accounted for when exporting the object scale.
            builder.setObjectScale(globalScale * globalScaleFromTransform * modelScale);

            Matrix4 combinedTransformNoScale = globalTransformNoScale.times(modelTransformNoScale);
            builder.setOrientationMatrix(combinedTransformNoScale.getUpperLeft3x3());
            builder.setObjectTranslation(combinedTransformNoScale.getColumn(3).getXYZ().plus(globalTranslate));
        }
        else
        {
            // XML import typically
            // globalTransform may be just rotation, or may also incorporate translation and scale.
            // We do the global rotation and translation to get the orientation and location right for projective texture mapping,
            // but we keep the scale at the imported model scale
            // Order of transformation is:
            // 1. inverse of "global" transformation, i.e. from OBJ export space to camera optimization space
            // 2. camera transformation from camera optimization space to individual camera POV
            // The global transformation needs to be inverted based on how it's represented by Metashape
            // (camera opt. space to OBJ export space) so we would do translation, then rotation, than scale last.
            // Rather than actually applying the scale, we will adjust the camera transformations later.
            projTexWorldTransform = globalTransformNoScale.quickInverse(0.001f)
                .times(Matrix4.translate(globalTranslate.negated()));
            projTexWorldScaleCombined = 1.0f / (globalScale * globalScaleFromTransform);

            // Object orientation, translation, and scale should just default; imported mesh should already be at the right orientation.
            // Since we avoid actually scaling the model, the glTF will export at the same scale as imported aside from any user adjustments
        }

        Camera[] cameras = cameraSet.toArray(new Camera[0]);

        // Fill out the camera pose, projection index, and light index lists
        for (Camera cam : cameras)
        {
            // camera's coordinates are expressed in optimization coordinates
            // we're planning to work in exported model coordinates
            // so we need to scale the camera's displacement by 1 / projTexWorldScaleCombined
            // unscaledCamTransform should basically be equivalent to scale(1 / projTexWorldScaleCombined) * cam.transform * scale(projTexWorldScaleCombined)
            Vector3 displacement = cam.transform.getColumn(3).getXYZ();
            Matrix4 unscaledCamTransform = Matrix4.translate(displacement.times(1.0f / projTexWorldScaleCombined).minus(displacement))
                .times(cam.transform);

            // Technically we'd need to also apply scale from global or model tramsform to cam.transform.
            // Rather than actually applying the scale, we just adjusted the camera translation above.
            // We could apply global scale after unscaledCamTransform (scale(projTexWorldScaleCombined) * unscaledCamTransform)
            // and it would be equivalent to applying scale before the original cam.transform (i.e. cam.transform * scale(projTexWorldScaleCombined))
            // Keeping the original scale avoids breaking code that assumes rendering at the scale of the raw geometry
            // but doesn't affect projective texture mapping if scale would be the last transformation in the sequence
            cam.transform = unscaledCamTransform.times(projTexWorldTransform);

            builder.setCurrentCameraPose(cam.transform);

            builder.setCurrentCameraProjectionIndex(cam.sensor.index);
            builder.setCurrentLightIndex(cam.lightIndex);

            int camID = Integer.parseInt(cam.id);

            if (imagePathMap != null && imagePathMap.containsKey(camID))
            {
                builder.setCurrentImageFile(new File(imagePathMap.get(camID)));
            }
            else
            {
                builder.setCurrentImageFile(new File(cam.filename));
                if (imagePathMap != null)
                {
                    LOG.error("Camera path override not found for camera: {}", camID);
                }
            }

            if (maskPathMap != null && maskPathMap.containsKey(camID))
            {
                builder.setCurrentMaskFile(new File(maskPathMap.get(camID)));
            }

            builder.commitCurrentCameraPose();
        }

        for (int i = 0; i < nextLightIndex; i++)
        {
            // Setup default light calibration (setting to zero is OK; will be overridden at a later stage)
            builder.addLight(Vector3.ZERO, new Vector3(1.0f));
        }

        // Set full res image directory according to input argument
        builder.setFullResImageDirectory(directories.fullResImageDirectory);

        return builder;
    }

    public static Builder loadViewsetFromChunk(MetashapeChunk metashapeChunk, Collection<File> disabledImageFiles)
        throws IOException, XMLStreamException, MissingImagesException
    {
        // Get reference to the chunk directory
        File chunkDirectory = new File(metashapeChunk.getChunkDirectoryPath());
        if (!chunkDirectory.exists())
        {
            throw new FileNotFoundException(MessageFormat.format("Chunk directory does not exist: {0}", chunkDirectory));
        }

        File rootDirectory = new File(metashapeChunk.getParentDocument().getPsxFilePath()).getParentFile();
        if (!rootDirectory.exists())
        {
            throw new FileNotFoundException(MessageFormat.format("Root directory does not exist: {0}", rootDirectory));
        }

        // 1) Construct camera ID to filename map from frame's ZIP
        Map<Integer, String> cameraPathsMap = metashapeChunk.buildCameraPathsMap(true, disabledImageFiles);

        // Mask info is inside frame.xml, so we need to read it outside of readFromStream() which takes chunk.xml
        Map<Integer, String> maskPathsMap = null;
        File masksDir = metashapeChunk.getMasksDirectory();
        if (masksDir != null && masksDir.toString().endsWith(".zip") && masksDir.exists())
        {
            try
            {
                maskPathsMap = extractMaskFilenames(masksDir);
            }
            catch (IOException e)
            {
                // Suppress exception so that a missing masks directory doesn't cause the whole project to fail to load.
                LOG.warn("Error extracting masks: {}", masksDir);
            }
        }

        // 2) Load ViewSet from ZipInputStream from chunk's ZIP (eventually will accept the filename map as a parameter)
        File zipFile = new File(chunkDirectory, "chunk.zip");
        try (FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis)))
        {
            ZipEntry entry;

            // Specify the desired file name
            String targetFileName = "doc.xml";

            while ((entry = zis.getNextEntry()) != null)
            {
                if (entry.getName().equals(targetFileName))
                {
                    // Found the desired file inside the zip
                    InputStream fileStream = new BufferedInputStream(zis);

                    ViewSetDirectories directories = new ViewSetDirectories();
                    directories.projectRoot = rootDirectory;
                    directories.supportingFilesDirectory = null;
                    directories.fullResImagesNeedUndistort = true;

                    // TODO: USING A HARD CODED VERSION VALUE (200)
                    // Create and store ViewSet
                    Builder viewSetBuilder = readFromStream(fileStream, directories,
                            String.valueOf(metashapeChunk.getCurrModelID()),
                            cameraPathsMap,
                            maskPathsMap,
                            200,
                            true);

                    // Send masksDirectory to viewset and have it process them once the progress bars are ready for it
                    viewSetBuilder.setMasksDirectory(masksDir);

                    // 3) load geometry from ZipInputStream from model's ZIP
                    String modelPath = metashapeChunk.getCurrentModelPath();
                    viewSetBuilder.setGeometryFile(new File(chunkDirectory, "0/" + modelPath));
                    if (modelPath.isEmpty())
                    {
                        throw new FileNotFoundException("Could not find model path");
                    }

                    // 4) Set image directory to be parent directory of MetaShape project (and add to the photos' paths)
                    File psxFile = new File(metashapeChunk.getParentDocument().getPsxFilePath());

                    //TODO: how is this working? This is not the directory of full res photos. Does this work because of relative paths in frame.xml or something?
                    File fullResImageDirectory = new File(psxFile.getParent()); // The directory of full res photos
                    // Print error to log if unable to find fullResImageDirectory
                    if (!fullResImageDirectory.exists())
                    {
                        throw new FileNotFoundException(MessageFormat.format("Unable to find fullResImageDirectory: {0}", fullResImageDirectory));
                    }

                    File override = metashapeChunk.getSelectedModel().getLoadPreferences().getFullResOverride();
                    if (override != null)
                    {
                        viewSetBuilder.setFullResImageDirectory(override);
                    }
                    else
                    {
                        // Set the fullResImage Directory to be the root directory
                        viewSetBuilder.setFullResImageDirectory(fullResImageDirectory);
                    }

                    return viewSetBuilder;
                }
            }

            throw new FileNotFoundException(MessageFormat.format("Could not find file {0} in zip {1}", targetFileName, zipFile));
        }
    }

    private static Map<Integer, String> extractMaskFilenames(File masksZipFile) throws IOException
    {
        LOG.info("Unzipping masks folder...");
        Document docXml = UnzipHelper.unzipToDocument(masksZipFile, "doc.xml");
        NodeList maskList = docXml.getElementsByTagName("mask");

        Map<Integer, String> maskFiles = new HashMap<>(maskList.getLength());

        for (int i = 0; i < maskList.getLength(); ++i)
        {
            Element maskElem = (Element) maskList.item(i);
            int cameraId = Integer.parseInt(maskElem.getAttribute("camera_id"));
            String path = maskElem.getAttribute("path");
            maskFiles.put(cameraId, path);
        }

        return maskFiles;
    }
}
