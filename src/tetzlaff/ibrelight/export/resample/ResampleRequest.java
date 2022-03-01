/*
 * Copyright (c) Michael Tetzlaff 2019
 * Copyright (c) The Regents of the University of Minnesota 2019
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.resample;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.ViewSet;

public class ResampleRequest<ContextType extends Context<ContextType>> implements IBRRequest<ContextType>
{
    private final int resampleWidth;
    private final int resampleHeight;
    private final File resampleVSETFile;
    private final File resampleExportPath;
    
    public ResampleRequest(int width, int height, File targetVSETFile, File exportPath)
    {
        this.resampleWidth = width;
        this.resampleHeight = height;
        this.resampleVSETFile = targetVSETFile;
        this.resampleExportPath = exportPath;
    }

    @Override
    public void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback) throws IOException
    {
        ViewSet targetViewSet = ViewSet.loadFromVSETFile(resampleVSETFile);

        try
        (
            FramebufferObject<ContextType> framebuffer = renderable.getResources().context.buildFramebufferObject(resampleWidth, resampleHeight)
                .addColorAttachment()
                .addDepthAttachment()
                .createFramebufferObject()
        )
        {

            for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
            {
                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, /*1.0f*/0.0f);
                framebuffer.clearDepthBuffer();

                renderable.draw(framebuffer, targetViewSet.getCameraPose(i),
                    targetViewSet.getCameraProjection(targetViewSet.getCameraProjectionIndex(i))
                        .getProjectionMatrix(targetViewSet.getRecommendedNearPlane(), targetViewSet.getRecommendedFarPlane()));

                String[] parts = targetViewSet.getImageFileName(i).split("\\.");
                File exportFile = new File(resampleExportPath,
                    Stream.concat(Arrays.stream(parts, 0, parts.length - 1), Stream.of("png"))
                        .collect(Collectors.joining(".")));

                exportFile.getParentFile().mkdirs();
                framebuffer.saveColorBufferToFile(0, "PNG", exportFile);

                if (callback != null)
                {
                    callback.setProgress((double) i / (double) targetViewSet.getCameraPoseCount());
                }
            }

            Files.copy(resampleVSETFile.toPath(),
                new File(resampleExportPath, resampleVSETFile.getName()).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
            Files.copy(renderable.getActiveViewSet().getGeometryFile().toPath(),
                new File(resampleExportPath, renderable.getActiveViewSet().getGeometryFile().getName()).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
