/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.fidelitymetricmatrix;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;

public class FidelityMetricMatrixRequest implements IBRRequest
{
    private final int width;
    private final int height;
    private final File targetVSETFile;
    private final File exportPath;
    
    public FidelityMetricMatrixRequest(int width, int height, File targetVSETFile, File exportPath)
    {
        this.width = width;
        this.height = height;
        this.targetVSETFile = targetVSETFile;
        this.exportPath = exportPath;
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback) throws IOException
    {
        callback.setMaximum(130);
        int progress = 0;

        for (int roughness = 5; roughness <= 50; roughness += 5)
        {
            for (int uvScale = 0; uvScale <= 12; uvScale++)
            {
                MetricRenderRequest request =
                    new MetricRenderRequest(roughness, uvScale, width, height, targetVSETFile,
                        new File(exportPath, String.format("r%02d-uv%02d_%s",
                            roughness, uvScale, renderable.getActiveViewSet().getGeometryFile().getName().split("\\.")[0])));

                request.executeRequest(renderable);

                progress++;

                if (callback != null)
                {
                    callback.setProgress(progress);
                }
            }
        }
    }
}
