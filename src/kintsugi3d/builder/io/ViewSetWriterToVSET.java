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

import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.state.ReadonlySettingsModel;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Map;

public final class ViewSetWriterToVSET implements ViewSetWriter
{
    private static final Logger log = LoggerFactory.getLogger(ViewSet.class);
    private static final ViewSetWriter INSTANCE = new ViewSetWriterToVSET();

    public static ViewSetWriter getInstance()
    {
        return INSTANCE;
    }

    private ViewSetWriterToVSET()
    {
    }

    @Override
    public void writeToStream(ReadonlyViewSet viewSet, OutputStream outputStream)
    {
        PrintStream out = new PrintStream(outputStream, false, StandardCharsets.UTF_8);
        out.println("# Created by Kintsugi 3D Builder");

        out.println();
        out.println("# ViewSet UUID");
        out.println("U " + viewSet.getUUID());

        if (viewSet.getGeometryFileName() != null)
        {
            out.println();
            out.println("# Geometry file name (mesh)");
            out.println("m " + viewSet.getGeometryFileName());
        }

        out.println();
        out.println("# Full resolution image file path");
        out.println("I " + viewSet.getRelativeFullResImagePathName());

        out.println();
        out.println("# Preview resolution image file path");
        out.println("i " + viewSet.getRelativePreviewImagePathName());

        out.println();
        out.println("# Supporting files (texture fit results) file path");
        out.println("t " + viewSet.getRelativeSupportingFilesPathName());

        out.println();
        out.println("# Estimated near and far planes");
        out.printf("c\t%.8f\t%.8f", viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane());
        out.println();

        // Correct for the primary view selection index being moved to 0 when written to the vset file
        int correctedOrientationViewIndex = viewSet.getOrientationViewIndex();
        if (viewSet.getOrientationViewIndex() >= 0)
        {
            if (viewSet.getPrimaryViewIndex() == correctedOrientationViewIndex)
            {
                correctedOrientationViewIndex = 0;
            }
            else if (viewSet.getPrimaryViewIndex() > correctedOrientationViewIndex)
            {
                correctedOrientationViewIndex += 1;
            }
        }

        out.println();
        out.println("# Reference orientation view index");
        out.println("O " + correctedOrientationViewIndex);

        out.println();
        out.println("# Reference View Pose Rotation (degrees)");
        out.println("r " + viewSet.getOrientationViewRotationDegrees());

        boolean firstSetting = true;
        for (ReadonlySettingsModel.Setting setting : viewSet.getProjectSettings())
        {
            if (firstSetting) // print only for the first setting; do not print at all if no settings
            {
                out.println();
                out.println("# Additional settings");
                firstSetting = false;
            }

            Class<?> type = setting.getType();
            if (type.equals(Boolean.class))
            {
                out.print("zb ");
            }
            else if (type.equals(Integer.class) || type.equals(Short.class))
            {
                out.print("zi ");
                // no support at present for 64-bit integers, could add if needed
            }
            else if (type.equals(Float.class) || type.equals(Double.class))
            {
                out.print("zf ");
                // doubles will be reduced to single-precision floats when the file is read
            }
            else
            {
                log.warn("Unrecognized type in view set settings model when writing to file: " + type.toString());
            }

            out.print(setting.getName());
            out.print(' ');
            out.println(setting.getValue());
        }

        if (viewSet.getResourceMap() != null)
        {
            out.println();
            out.println("# Resource files");

            for (var resource : viewSet.getResourceMap().entrySet())
            {
                out.print("zr ");
                out.print(resource.getKey());
                out.print(' ');
                out.println(resource.getValue());
            }
        }

        if (viewSet.hasMasks())
        {
            out.println();
            out.println("# Masks directory");
            out.println("M " + viewSet.getMasksDirectory().getAbsolutePath());

            out.println();
            Map<Integer, File> masksMap = viewSet.getMasksMap();
            out.println("# " + masksMap.size() + " masks");
            for (var entry : masksMap.entrySet())
            {
                if (entry.getValue() != null)
                {
                    out.println(MessageFormat.format("k\t{0}\t{1}", entry.getKey(), entry.getValue().getName()));
                }
            }
        }

        out.println();
        out.println("# " + viewSet.getCameraProjectionCount() + (viewSet.getCameraProjectionCount() == 1 ? " Sensor" : " Sensors"));
        for (int i = 0; i < viewSet.getCameraProjectionCount(); i++)
        {
            out.println(viewSet.getCameraProjection(i).toVSETString());
        }

        if (viewSet.hasCustomLuminanceEncoding())
        {
            out.println();
            out.println("# Luminance encoding: Munsell 2/3.5/5.6.5/8/9.5");
            out.println("#\tCIE-Y/100\tEncoded");

            double[] linearLuminanceValues = viewSet.getLinearLuminanceValues();
            byte[] encodedLuminanceValues = viewSet.getEncodedLuminanceValues();

            for (int i = 0; i < linearLuminanceValues.length && i < encodedLuminanceValues.length; i++)
            {
                out.printf("e\t%.8f\t\t%3d", linearLuminanceValues[i], 0x00FF & encodedLuminanceValues[i]);
                out.println();
            }
        }

        out.println();
        out.println("# " + viewSet.getCameraPoseCount() + (viewSet.getCameraPoseCount() == 1 ? " Camera" : " Cameras"));
        for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
        {
            Matrix4 pose = viewSet.getCameraPose(i);

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
            out.printf("P\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f",
                pose.get(0, 0), pose.get(0, 1), pose.get(0, 2), pose.get(0, 3),
                pose.get(1, 0), pose.get(1, 1), pose.get(1, 2), pose.get(1, 3),
                pose.get(2, 0), pose.get(2, 1), pose.get(2, 2), pose.get(2, 3),
                pose.get(3, 0), pose.get(3, 1), pose.get(3, 2), pose.get(3, 3));
            //}
            out.println();
        }

        if (viewSet.getLightCount() > 0)
        {
            out.println();
            out.println("# " + viewSet.getLightCount() + (viewSet.getLightCount() == 1 ? " Light" : " Lights"));
            for (int id = 0; id < viewSet.getLightCount(); id++)
            {
                Vector3 pos = viewSet.getLightPosition(id);
                Vector3 intensity = viewSet.getLightIntensity(id);
                out.printf("l\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f", pos.x, pos.y, pos.z, intensity.x, intensity.y, intensity.z);
                out.println();
            }
        }

        out.println();
        out.println("# " + viewSet.getCameraPoseCount() + (viewSet.getCameraPoseCount() == 1 ? " View" : " Views"));

        // Primary view first (so that next time the view set is loaded it will be index 0)
        out.printf("v\t%d\t%d\t%d\t%s", viewSet.getPrimaryViewIndex(),
            viewSet.getCameraProjectionIndex(viewSet.getPrimaryViewIndex()),
            viewSet.getLightIndex(viewSet.getPrimaryViewIndex()),
            viewSet.getImageFileName(viewSet.getPrimaryViewIndex()));
        out.println();

        for (int id = 0; id < viewSet.getCameraPoseCount(); id++)
        {
            if (id != viewSet.getPrimaryViewIndex())
            {
                out.printf("v\t%d\t%d\t%d\t%s", id, viewSet.getCameraProjectionIndex(id), viewSet.getLightIndex(id), viewSet.getImageFileName(id));
                out.println();
            }
        }

        out.close();
    }
}
