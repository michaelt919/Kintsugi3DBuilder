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

package kintsugi3d.builder.export.screenshot;

import kintsugi3d.builder.core.ObservableProjectGraphicsRequest;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;

import java.io.File;
import java.io.IOException;

public class ScreenshotRequest implements ObservableProjectGraphicsRequest
{
    private final int width;
    private final int height;
    private final File exportFile;

    public interface Builder<RequestType extends ScreenshotRequest>
    {
        Builder<RequestType> setWidth(int width);
        Builder<RequestType> setHeight(int height);
        Builder<RequestType> setExportFile(File exportFile);
        RequestType create();
    }

    protected static class BuilderImplementation
            implements Builder<ScreenshotRequest>
    {
        private int width;
        private int height;
        private File exportFile;

        protected int getWidth()
        {
            return width;
        }

        protected int getHeight()
        {
            return height;
        }

        protected File getExportFile()
        {
            return exportFile;
        }

        @Override
        public Builder<ScreenshotRequest> setWidth(int width)
        {
            this.width = width;
            return this;
        }

        @Override
        public Builder<ScreenshotRequest> setHeight(int height)
        {
            this.height = height;
            return this;
        }

        @Override
        public Builder<ScreenshotRequest> setExportFile(File exportFile)
        {
            this.exportFile = exportFile;
            return this;
        }


        @Override
        public ScreenshotRequest create()
        {
            return new ScreenshotRequest(getWidth(), getHeight(), getExportFile());
        }
    }

    protected ScreenshotRequest(int width, int height, File exportFile)
    {
        this.width = width;
        this.height = height;
        this.exportFile = exportFile;
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(
        ProjectInstance<ContextType> renderable, ProgressMonitor monitor) throws IOException
    {
        try
        (
            FramebufferObject<ContextType> framebuffer = renderable.getResources().getContext().buildFramebufferObject(width, height)
                .addColorAttachment()
                .addDepthAttachment()
                .createFramebufferObject()
        )
        {
            if(monitor != null){
                monitor.setProcessName("Screenshot");
            }
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, /*1.0f*/0.0f);
            framebuffer.clearDepthBuffer();

            renderable.draw(framebuffer, null, null, 320, 180);

            exportFile.getParentFile().mkdirs();
            String fileNameLowerCase = exportFile.getName().toLowerCase();
            if (fileNameLowerCase.endsWith(".png"))
            {
                framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", exportFile);
            }
            else if (fileNameLowerCase.endsWith(".jpg") || fileNameLowerCase.endsWith(".jpeg"))
            {
                framebuffer.getTextureReaderForColorAttachment(0).saveToFile("JPEG", exportFile);
            }

            if (monitor != null)
            {
                monitor.setProgress(1.0, "Screenshot complete.");
            }
        }
    }
}
