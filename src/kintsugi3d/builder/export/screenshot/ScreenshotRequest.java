/*
 * Copyright (c) 2019-2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.export.screenshot;

import java.io.File;
import java.io.IOException;

import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.builder.core.IBRInstance;
import kintsugi3d.builder.core.IBRRequest;
import kintsugi3d.builder.core.LoadingMonitor;

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
                framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", exportFile);
            }
            else if (fileNameLowerCase.endsWith(".jpg") || fileNameLowerCase.endsWith(".jpeg"))
            {
                framebuffer.getTextureReaderForColorAttachment(0).saveToFile("JPEG", exportFile);
            }

            if (callback != null)
            {
                callback.setProgress(1.0);
            }
        }
    }
}
