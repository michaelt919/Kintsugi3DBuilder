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

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import kintsugi3d.builder.core.DistortionProjection;
import kintsugi3d.builder.core.SimpleProjection;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.core.ViewSet.Builder;
import kintsugi3d.builder.state.SettingsModel;
import kintsugi3d.builder.state.impl.SimpleSettingsModel;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Handles loading view sets from the VSET text file format
 */
public final class ViewSetReaderFromVSET implements ViewSetReader
{
    private static final Logger log = LoggerFactory.getLogger(ViewSetReaderFromVSET.class);

    private static final ViewSetReaderFromVSET INSTANCE = new ViewSetReaderFromVSET();

    public static ViewSetReaderFromVSET getInstance()
    {
        return INSTANCE;
    }

    private ViewSetReaderFromVSET()
    {
    }

    /**
     * Loads a view set from an input file.
     * The root directory and the supporting files directory will be set as specified.
     * The supporting files directory may be overridden by a directory specified in the file.
     * @param stream The file to load
     * @return The view set
     * @throws IOException If I/O errors occur while reading the file.
     */
    public ViewSet readFromStream(InputStream stream, ViewSetLoadOverrides overrides)
    {
        File root = overrides.projectRoot;
        File supportingFilesDirectory = overrides.supportingFilesDirectory;
        boolean needsUndistort = overrides.needsUndistort;
        Date timestamp = new Date();

        Builder builder = ViewSet.getBuilder(root, supportingFilesDirectory, 128);

        List<Double> linearLuminanceList = new ArrayList<>(8);
        List<Byte> encodedLuminanceList = new ArrayList<>(8);

        SettingsModel settings = new SimpleSettingsModel();

        try(Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8))
        {
            scanner.useLocale(Locale.US);
            
            List<Matrix4> unorderedCameraPoseList = new ArrayList<>(128);

            while (scanner.hasNext())
            {
                String id = scanner.next();
                switch(id)
                {
                    case "U":
                        builder.setUUID(UUID.fromString(scanner.nextLine().trim()));
                        break;
                    case "c":
                    {
                        builder.setRecommendedClipPlanes(scanner.nextFloat(), scanner.nextFloat());
                        scanner.nextLine();
                        break;
                    }
                    case "O":
                    {
                        builder.setOrientationViewIndex(scanner.nextInt());
                        scanner.nextLine();
                        break;
                    }
                    case "r":
                        builder.setOrientationViewRotation(scanner.nextFloat());
                        scanner.nextLine();
                        break;
                    case "m":
                    {
                        builder.setGeometryFileName(scanner.nextLine().trim());
                        break;
                    }
                    case "M":
                    {
                        builder.setMasksDirectory(new File(scanner.nextLine().trim()));
                        break;
                    }
                    case "I":
                    {
                        builder.setRelativeFullResImagePathName(scanner.nextLine().trim());
                        break;
                    }
                    case "i":
                    {
                        builder.setRelativePreviewImagePathName(scanner.nextLine().trim());
                        break;
                    }
                    case "t":
                    {
                        builder.setRelativeSupportingFilesPathName(scanner.nextLine().trim());
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
                        break;
                    }
                    case "d": // Legacy format; generally used with synthetic data
                    case "D": // Legacy format from older IBRelight projects from PhotoScan / Metashape
                    {
                        // Skip cx / cy as they aren't used consistently
                        scanner.nextFloat();
                        scanner.nextFloat();

                        float aspect = scanner.nextFloat();
                        float focalLength = scanner.nextFloat();

                        // For Metashape projects read the sensor width; otherwise assume 32
                        float sensorWidth = "D".equals(id) ? scanner.nextFloat() : 32.0f;
                        float sensorHeight = sensorWidth / aspect;

                        builder.addCameraProjection(new SimpleProjection(
                            aspect, 2.0f * (float) Math.atan2(sensorHeight, 2 * focalLength)));

                        // Skip any distortion parameters as they aren't used consistently
                        // and images are probably undistorted from PhotoScan/Metashape
                        scanner.nextLine();
                        break;
                    }
                    case "s":
                    {
                        float cx = scanner.nextFloat(); // relative to (0, 0)
                        float cy = scanner.nextFloat();

                        float aspect = scanner.nextFloat();
                        float focalLength = scanner.nextFloat();
                        float sensorWidth = scanner.nextFloat();
                        float sensorHeight = sensorWidth / aspect;
                        float k1 = scanner.nextFloat();
                        float k2 = scanner.nextFloat();
                        float k3 = scanner.nextFloat();
                        float k4 = scanner.nextFloat();
                        float p1 = scanner.nextFloat();
                        float p2 = scanner.nextFloat();
                        float b1 = scanner.nextFloat(); // fx - fy
                        float b2 = scanner.nextFloat(); // a.k.a. skew

                        DistortionProjection distortionProj = new DistortionProjection(
                            sensorWidth, sensorHeight,
                            focalLength + b1, focalLength,
                            cx, cy, k1, k2, k3, k4, p1, p2, b2
                        );

                        if (needsUndistort)
                        {
                            builder.addCameraProjection(distortionProj);
                        }
                        else
                        {
                            builder.addCameraProjection(new SimpleProjection(
                                distortionProj.getAspectRatio(), distortionProj.getVerticalFieldOfView()));
                        }

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
                        // Gamma -- no longer used
//                        gamma = scanner.nextFloat();
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

                        builder.addCameraProjection(new SimpleProjection(aspect, fovy));

                        scanner.nextLine();
                        break;
                    }
                    case "l":
                    {
                        float x = scanner.nextFloat();
                        float y = scanner.nextFloat();
                        float z = scanner.nextFloat();

                        float r = scanner.nextFloat();
                        float g = scanner.nextFloat();
                        float b = scanner.nextFloat();

                        builder.addLight(new Vector3(x, y, z), new Vector3(r, g, b));

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

                        builder.setCurrentCameraPose(unorderedCameraPoseList.get(poseId))
                            .setCurrentCameraProjectionIndex(projectionId)
                            .setCurrentLightIndex(lightId)
                            .setCurrentImageFile(new File(imgFilename))
                            .commitCurrentCameraPose();
                        break;
                    }
                    case "k":
                    {
                        int cameraId = scanner.nextInt();

                        String imgFilename = scanner.nextLine().trim();

                        builder.addMask(cameraId, imgFilename);
                        break;
                    }
                    case "zb":
                        // boolean setting
                        settings.createBooleanSetting(scanner.next(), Boolean.parseBoolean(scanner.next()));
                        scanner.nextLine(); // Ignore rest of line
                        break;
                    case "zi":
                        // integer setting
                        settings.createNumericSetting(scanner.next(), Integer.parseInt(scanner.next()));
                        scanner.nextLine(); // Ignore rest of line
                        break;
                    case "zf":
                        // integer setting
                        settings.createNumericSetting(scanner.next(), Float.parseFloat(scanner.next()));
                        scanner.nextLine(); // Ignore rest of line
                        break;
                    default:
                        // Skip unrecognized line
                        scanner.nextLine();
                }
            }
        }

        ViewSet result = builder.finish();
        result.getViewSetSettings().copyFrom(settings);

        // Tonemapping
        double[] linearLuminanceValues = new double[linearLuminanceList.size()];
        Arrays.setAll(linearLuminanceValues, linearLuminanceList::get);

        byte[] encodedLuminanceValues = new byte[encodedLuminanceList.size()];
        for (int i = 0; i < encodedLuminanceValues.length; i++)
        {
            encodedLuminanceValues[i] = encodedLuminanceList.get(i);
        }

        result.setTonemapping(linearLuminanceValues, encodedLuminanceValues);

        if (result.getSupportingFilesFilePath() != null)
        {
            // Make sure the supporting files directory exists
            result.getSupportingFilesFilePath().mkdirs();
        }

        log.info("View Set file loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        return result;
    }

    /**
     * Loads a view set from an input file.
     * By default, the view set's root directory as well as the supporting files directory will be set to the specified root.
     * The supporting files directory may be overridden by a directory specified in the file.
     * @param stream
     * @param root
     * @return The view set
     * @throws Exception If errors occur while reading the file.
     */
    public ViewSet readFromStream(InputStream stream, File root)
    {
        // Use root directory as supporting files directory
        ViewSetLoadOverrides overrides = new ViewSetLoadOverrides();
        overrides.projectRoot = root;
        overrides.supportingFilesDirectory = root;
        overrides.needsUndistort = true;
        return readFromStream(stream, overrides);
    }

    /**
     * Loads a view set from an input file.
     * By default, the view set's root directory will be set to the parent directory of the specified file.
     * The supporting files directory will be set as specified by default but may be overridden by a directory specified in the file.
     * @param file The file to load
     * @param supportingFilesDirectory
     * @return The view set
     * @throws Exception If errors occur while reading the file.
     */
    public ViewSet readFromFile(File file, File supportingFilesDirectory) throws IOException
    {
        try (InputStream stream = new FileInputStream(file))
        {
            ViewSetLoadOverrides overrides = new ViewSetLoadOverrides();
            overrides.projectRoot = file;
            overrides.supportingFilesDirectory = supportingFilesDirectory;
            overrides.needsUndistort = true;
            return readFromStream(stream, overrides);
        }
    }

    /**
     * Loads a view set from an input file.
     * By default, the view set's root directory and supporting files directory will be set to the parent directory of the specified file.
     * The supporting files directory may be overridden by a directory specified in the file.
     * @param file The file to load
     * @return The view set
     * @throws IOException If I/O errors occur while reading the file.
     */
    public ViewSet readFromFile(File file) throws IOException
    {
        try (InputStream stream = new FileInputStream(file))
        {
            return readFromStream(stream, file.getParentFile());
        }
    }
}
