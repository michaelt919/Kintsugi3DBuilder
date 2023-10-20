/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.io;

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

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Handles loading view sets from the VSET text file format
 */
public final class ViewSetReaderFromVSET implements ViewSetReader
{
    private static final Logger log = LoggerFactory.getLogger(ViewSetReaderFromVSET.class);

    private static final ViewSetReader INSTANCE = new ViewSetReaderFromVSET();

    public static ViewSetReader getInstance()
    {
        return INSTANCE;
    }

    private ViewSetReaderFromVSET()
    {
    }

    @Override
    public ViewSet readFromStream(InputStream stream, File root, File supportingFilesDirectory)
    {
        Date timestamp = new Date();

        ViewSet result = new ViewSet(128);
        result.setRootDirectory(root);
        result.setSupportingFilesDirectory(supportingFilesDirectory);

        float gamma = 2.2f;

        List<Double> linearLuminanceList = new ArrayList<>(8);
        List<Byte> encodedLuminanceList = new ArrayList<>(8);

        try(Scanner scanner = new Scanner(stream))
        {
            List<Matrix4> unorderedCameraPoseList = new ArrayList<>(128);
            List<Matrix4> unorderedCameraPoseInvList = new ArrayList<>(128);

            while (scanner.hasNext())
            {
                String id = scanner.next();
                switch(id)
                {
                    case "U":
                        result.setUuid(UUID.fromString(scanner.nextLine().trim()));
                        break;
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
                    case "I":
                    {
                        result.setRelativeFullResImagePathName(scanner.nextLine().trim());
                        break;
                    }
                    case "i":
                    {
                        result.setRelativePreviewImagePathName(scanner.nextLine().trim());
                        break;
                    }
                    case "t":
                    {
                        result.setRelativeSupportingFilesPathName(scanner.nextLine().trim());
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

                        result.getCameraProjectionList().add(new SimpleProjection(
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

                        result.getCameraProjectionList().add(new DistortionProjection(
                            sensorWidth, sensorHeight,
                            focalLength + b1, focalLength,
                            cx, cy, k1, k2, k3, k4, p1, p2, b2
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

        // Add default-initialized error metrics for each view
        IntStream.range(0, result.getCameraPoseCount()).mapToObj(i -> new ViewRMSE())
            .forEach(result.getViewErrorMetrics()::add);

        double[] linearLuminanceValues = new double[linearLuminanceList.size()];
        Arrays.setAll(linearLuminanceValues, linearLuminanceList::get);

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

        if (result.getGeometryFile() == null)
        {
            result.setGeometryFileName("manifold.obj"); // Used by some really old datasets
        }

        // Make sure the supporting files directory exists
        result.getSupportingFilesFilePath().mkdirs();

        log.info("View Set file loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        return result;
    }
}
