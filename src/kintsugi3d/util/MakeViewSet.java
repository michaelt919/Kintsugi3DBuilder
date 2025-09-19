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

package kintsugi3d.util;

import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.io.ViewSetWriterToVSET;
import kintsugi3d.gl.vecmath.Vector3;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MakeViewSet
{
    private MakeViewSet()
    {
    }

    static ReadonlyViewSet makeViewSet(int viewCount, float distance, float nearPlane, float aspect, float sensorWidth, float focal)
    {
        List<Vector3> viewDirections = new ArrayList<>(viewCount);

        double densityFactor = Math.sqrt(Math.PI * viewCount);
        int sampleRows = (int)Math.ceil(densityFactor / 2) + 1;

        for (int i = 0; i < sampleRows; i++)
        {
            double r = Math.sin(0.001 + (Math.PI - 0.002) * (double)i / (double)(sampleRows-1));
            int sampleColumns = Math.max(1, (int)Math.ceil(densityFactor * r));

            for (int j = 0; j < sampleColumns; j++)
            {
                viewDirections.add(new Vector3(
                    (float)(r * Math.cos(2 * Math.PI * (double)j / (double)sampleColumns)),
                    (float) Math.cos(Math.PI * (double)i / (double)(sampleRows-1)),
                    (float)(r * Math.sin(2 * Math.PI * (double)j / (double)sampleColumns))));
            }
        }

        return ViewSet.createFromLookAt(viewDirections, Vector3.ZERO, new Vector3(0.0f, 1.0f, 0.0f), distance,
            nearPlane, aspect, sensorWidth, focal);
    }

    public static void main(String[] args)
    {
        ReadonlyViewSet viewSet = makeViewSet(Integer.parseInt(args[0]), Float.parseFloat(args[1]),
            Float.parseFloat(args[2]), Float.parseFloat(args[3]), Float.parseFloat(args[4]), Float.parseFloat(args[5]));
        try (PrintStream out = new PrintStream(String.format(args[6], viewSet.getCameraPoseCount()), StandardCharsets.UTF_8))
        {
            ViewSetWriterToVSET.getInstance().writeToStream(viewSet, out);
        }
        catch (IOException|RuntimeException e)
        {
            e.printStackTrace();
        }
    }
}
