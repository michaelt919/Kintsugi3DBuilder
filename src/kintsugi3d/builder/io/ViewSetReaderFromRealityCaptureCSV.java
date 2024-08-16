/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.io;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBeanBuilder;
import kintsugi3d.builder.core.DistortionProjection;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.core.ViewSet.Builder;
import kintsugi3d.gl.vecmath.DoubleMatrix4;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

public final class ViewSetReaderFromRealityCaptureCSV implements ViewSetReaderFromLooseFiles
{
    private static final ViewSetReaderFromLooseFiles INSTANCE = new ViewSetReaderFromRealityCaptureCSV();

    public static ViewSetReaderFromLooseFiles getInstance()
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
        @CsvBindByName(locale = "en-US", required = true) double x;
        @CsvBindByName(locale = "en-US", required = true) double y;
        @CsvBindByName(locale = "en-US", required = true) double alt;
        @CsvBindByName(locale = "en-US", required = true) double heading;
        @CsvBindByName(locale = "en-US", required = true) double pitch;
        @CsvBindByName(locale = "en-US", required = true) double roll;
        @CsvBindByName(locale = "en-US", required = true) float f;
        @CsvBindByName(locale = "en-US") float px;
        @CsvBindByName(locale = "en-US") float py;
        @CsvBindByName(locale = "en-US") float k1;
        @CsvBindByName(locale = "en-US") float k2;
        @CsvBindByName(locale = "en-US") float k3;
        @CsvBindByName(locale = "en-US") float k4;
        @CsvBindByName(locale = "en-US") float t1;
        @CsvBindByName(locale = "en-US") float t2;

        public Matrix4 getPose()
        {
            // Expecting a world-to-camera space transformation, so we need to invert.
            return Matrix4.fromDoublePrecision(
                DoubleMatrix4.rotateY(-roll)
                    .times(DoubleMatrix4.rotateX(-pitch))
                    .times(DoubleMatrix4.rotateZ(-heading))
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
                    float aspect = reader.getAspectRatio(reader.getMinIndex());
                    return new DistortionProjection(35.0f /* 35mm standard */, 35.0f / aspect,
                        f, f, px, py, k1, k2, k3, k4, t1, t2, 0.0f /* skew not supported by RealityCapture export */);
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
