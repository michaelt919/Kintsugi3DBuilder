/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.screenshot;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.ibrelight.core.IBRInstance;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;

public class ScreenshotRequest<ContextType extends Context<ContextType>> implements IBRRequest<ContextType>
{
    private final int width;
    private final int height;
    private final File exportFile;

    public interface Builder<ContextType extends Context<ContextType>, RequestType extends ScreenshotRequest<ContextType>>
    {
        Builder<ContextType, RequestType> setWidth(int width);
        Builder<ContextType, RequestType> setHeight(int height);
        Builder<ContextType, RequestType> setExportFile(File exportFile);
        RequestType create();
    }

    protected static class BuilderImplementation<ContextType extends Context<ContextType>>
            implements Builder<ContextType, ScreenshotRequest<ContextType>>
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
        public Builder<ContextType, ScreenshotRequest<ContextType>> setWidth(int width)
        {
            this.width = width;
            return this;
        }

        @Override
        public Builder<ContextType, ScreenshotRequest<ContextType>> setHeight(int height)
        {
            this.height = height;
            return this;
        }

        @Override
        public Builder<ContextType, ScreenshotRequest<ContextType>> setExportFile(File exportFile)
        {
            this.exportFile = exportFile;
            return this;
        }


        @Override
        public ScreenshotRequest<ContextType> create()
        {
            return new ScreenshotRequest<>(getWidth(), getHeight(), getExportFile());
        }
    }

    protected ScreenshotRequest(int width, int height, File exportFile)
    {
        this.width = width;
        this.height = height;
        this.exportFile = exportFile;
    }

    @Override
    public void executeRequest(IBRInstance<ContextType> renderable, LoadingMonitor callback) throws IOException
    {
        try
        (
            FramebufferObject<ContextType> framebuffer = renderable.getIBRResources().getContext().buildFramebufferObject(width, height)
                .addColorAttachment()
                .addDepthAttachment()
                .createFramebufferObject()
        )
        {
            framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, /*1.0f*/0.0f);
            framebuffer.clearDepthBuffer();

            renderable.draw(framebuffer, null, null, 320, 180);

            exportFile.getParentFile().mkdirs();
            String fileNameLowerCase = exportFile.getName().toLowerCase();
            if (fileNameLowerCase.endsWith(".png"))
            {
                framebuffer.saveColorBufferToFile(0, "PNG", exportFile);
            }
            else if (fileNameLowerCase.endsWith(".jpg") || fileNameLowerCase.endsWith(".jpeg"))
            {
                framebuffer.saveColorBufferToFile(0, "JPEG", exportFile);
            }

            if (callback != null)
            {
                callback.setProgress(1.0);
            }
        }
    }
}
