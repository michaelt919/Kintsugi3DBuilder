/*
 * Copyright (c) 2023 Seth Berrier, Michael Tetzlaff, Josh Lyu, Luke Denney, Jacob Buelow
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package tetzlaff.ibrelight.io;

import tetzlaff.gl.vecmath.Matrix3;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibrelight.core.DistortionProjection;
import tetzlaff.ibrelight.core.SimpleProjection;
import tetzlaff.ibrelight.core.ViewSet;

import java.io.InputStream;
import java.util.*;

/**
 * Handles loading view sets from the VSET text file format
 */
public final class ViewSetReaderFromVSET implements ViewSetReader
{
    private static final ViewSetReader INSTANCE = new ViewSetReaderFromVSET();

    public static ViewSetReader getInstance()
    {
        return INSTANCE;
    }

    private ViewSetReaderFromVSET()
    {
    }

    @Override
    public ViewSet readFromStream(InputStream stream)
    {
        Date timestamp = new Date();

        ViewSet result = new ViewSet(128);

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

        if (result.getRelativeFullResImagePathName() == null && result.getRelativePreviewImagePathName() != null)
        {
            // If no full res images, just use preview images as full res
            result.setRelativeFullResImagePathName(result.getRelativePreviewImagePathName());
        }
        else if (result.getRelativePreviewImagePathName() == null && result.getRelativeFullResImagePathName() != null)
        {
            // Conversely, if no preview images, default to just using full res images
            result.setRelativePreviewImagePathName(result.getRelativeFullResImagePathName());
        }

        if (result.getGeometryFile() == null)
        {
            result.setGeometryFileName("manifold.obj"); // Used by some really old datasets
        }

        System.out.println("View Set file loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

        return result;
    }
}