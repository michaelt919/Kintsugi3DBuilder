/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.export.resample;

import kintsugi3d.builder.core.ObservableProjectGraphicsRequest;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.RenderableInstance;
import kintsugi3d.builder.core.UserCancellationException;
import kintsugi3d.builder.core.viewset.ReadonlyViewSet;
import kintsugi3d.builder.core.viewset.View;
import kintsugi3d.builder.io.ViewSetReaderFromVSET;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.util.ImageFinder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;

public class ResampleRequest implements ObservableProjectGraphicsRequest
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
    public <ContextType extends Context<ContextType>> void executeRequest(RenderableInstance<ContextType> renderable, ProgressMonitor monitor) throws IOException, UserCancellationException
    {
        ReadonlyViewSet targetViewSet = ViewSetReaderFromVSET.getInstance().readFromFile(resampleVSETFile).finish();

        try
        (
            FramebufferObject<ContextType> framebuffer = renderable.getResources().getContext().buildFramebufferObject(resampleWidth, resampleHeight)
                .addColorAttachment()
                .addDepthAttachment()
                .createFramebufferObject()
        )
        {
            if(monitor != null)
            {
                monitor.setProcessName("Resample");
            }

            int progressCount = 0;
            for (View view : targetViewSet.getViews())
            {
                if (monitor != null)
                {
                    monitor.allowUserCancellation();
                }

                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, /*1.0f*/0.0f);
                framebuffer.clearDepthBuffer();

                renderable.draw(framebuffer, view.getCameraPose(), view.getProjectionMatrix());

                File exportFile = new File(resampleExportPath,
                    ImageFinder.getInstance().getImageFileNameWithExtension(view.getImageFile().getName(), "png"));

                exportFile.getParentFile().mkdirs();
                framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", exportFile);

                if (monitor != null)
                {
                    monitor.setProgress((double) progressCount / (double) targetViewSet.getViewCount(),
                        MessageFormat.format("{0} ({1}/{2})", view,
                            progressCount + 1, targetViewSet.getViewCount()));
                }

                progressCount++;
            }

            Files.copy(resampleVSETFile.toPath(),
                new File(resampleExportPath, resampleVSETFile.getName()).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
            Files.copy(renderable.getViewSet().getGeometryFile().toPath(),
                new File(resampleExportPath, renderable.getViewSet().getGeometryFile().getName()).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
