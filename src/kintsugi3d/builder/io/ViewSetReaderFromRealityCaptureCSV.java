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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBeanBuilder;
import kintsugi3d.builder.core.DistortionProjection;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.core.ViewSet.Builder;
import kintsugi3d.gl.vecmath.*;

public final class ViewSetReaderFromRealityCaptureCSV implements ViewSetReader
{
    private static final ViewSetReader INSTANCE = new ViewSetReaderFromRealityCaptureCSV();

    public static ViewSetReader getInstance()
    {
        return INSTANCE;
    }

    private ViewSetReaderFromRealityCaptureCSV()
    {
    }

    @SuppressWarnings("PackageVisibleField")
    public static final class Camera
    {
        @CsvBindByName(column = "#name", required = true) String name;

        @CsvBindByName(required = true) double x;
        @CsvBindByName(required = true) double y;
        @CsvBindByName(required = true) double alt;
        @CsvBindByName(required = true) double heading;
        @CsvBindByName(required = true) double pitch;
        @CsvBindByName(required = true) double roll;
        @CsvBindByName(required = true) float f;
        @CsvBindByName float px;
        @CsvBindByName float py;
        @CsvBindByName float k1;
        @CsvBindByName float k2;
        @CsvBindByName float k3;
        @CsvBindByName float k4;
        @CsvBindByName float t1;
        @CsvBindByName float t2;

        public Matrix4 getPose()
        {
            // Expecting a world-to-camera space transformation, so we need to invert.
            return Matrix4.fromDoublePrecision(
                DoubleMatrix4.rotateY(-roll * Math.PI / 180) // Reality capture specifies angles in degrees.
                    .times(DoubleMatrix4.rotateX(-pitch * Math.PI / 180))
                    .times(DoubleMatrix4.rotateZ(heading * Math.PI / 180))
                    .times(DoubleMatrix4.translate(-x, -y, -alt)));
        }

        public DistortionProjection getDistortionProjection(File imageRootDirectory) throws IOException
        {
            // Have to read part of image file to determine aspect ratio
            try(var in = ImageIO.createImageInputStream(new File(imageRootDirectory, name)))
            {
                if (ImageIO.getImageReaders(in).hasNext())
                {
                    ImageReader reader = ImageIO.getImageReaders(in).next();
                    reader.setInput(in);

                    int width = reader.getWidth(reader.getMinIndex());
                    int height = reader.getHeight(reader.getMinIndex());

                    float fScaled = f * width / 36.0f; // 36 mm standard for Reality Capture

                    return new DistortionProjection(width, height, fScaled, fScaled,
                            px * width + (float)width / 2, py * height + (float)height / 2,
                            k1, k2, k3, k4, t1, t2, 0.0f);
                }
                else
                {
                    throw new IOException(MessageFormat.format("Could not find an image reader for file: {0}", name));
                }
            }
        }
    }

    @Override
    public ViewSet readFromStream(InputStream stream, File root, File geometryFile, File fullResImageDirectory) throws IOException
    {
        List<Camera> cameras = new CsvToBeanBuilder<Camera>(new InputStreamReader(stream, StandardCharsets.UTF_8))
            .withType(Camera.class).build().parse();

        // Try to figure out what the camera groups were
        Map<DistortionProjection, List<String>> cameraMap = new HashMap<>(1);
        for (Camera cam : cameras)
        {
            DistortionProjection distortionProjection = cam.getDistortionProjection(fullResImageDirectory);
            cameraMap.computeIfAbsent(distortionProjection, k -> new ArrayList<>(1)).add(cam.name);
        }

        // Start building the view set
        Builder builder = ViewSet.getBuilder(root, cameras.size());

        // Invert the mapping so that we can retrieve the distortion by camera
        Map<String, Integer> cameraMapInverted = new HashMap<>(cameras.size());
        for (var entry : cameraMap.entrySet())
        {
            int nextDistortionID = builder.getNextCameraProjectionIndex();

            // Add the distortion projection to the view set
            builder.addCameraProjection(entry.getKey());

            for (String camName : entry.getValue())
            {
                cameraMapInverted.put(camName, nextDistortionID);
            }
        }

        for (Camera cam : cameras)
        {
            // All cameras have the same light group when importing from Reality Capture
            // (although we try to reconstruct the camera groups it's not a guarantee we did it right,
            // so light groups should not be based on that reconstruction).
            builder.setCurrentCameraPose(cam.getPose())
                .setCurrentCameraProjectionIndex(cameraMapInverted.get(cam.name))
                .setCurrentLightIndex(0)
                .setCurrentImageFile(new File(cam.name))
                .commitCurrentCameraPose();
        }

        // Setup default light calibration (setting to zero is OK; will be overridden at a later stage)
        builder.addLight(Vector3.ZERO, Vector3.ZERO);

        ViewSet result = builder.finish();
        result.setGeometryFile(geometryFile);
        result.setFullResImageDirectory(fullResImageDirectory);
        return result;
    }
}
