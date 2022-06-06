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

package tetzlaff.ibrelight.export.simpleanimation;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.ibrelight.core.IBRInstance;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;

public abstract class SimpleAnimationRequestBase<ContextType extends Context<ContextType>> implements IBRRequest<ContextType>
{
    private final int width;
    private final int height;
    private final int frameCount;
    private final File exportPath;

    public interface Builder<RequestType extends SimpleAnimationRequestBase>
    {
        Builder<RequestType> setWidth(int width);
        Builder<RequestType> setHeight(int height);
        Builder<RequestType> setFrameCount(int frameCount);
        Builder<RequestType> setExportPath(File exportPath);
        RequestType create();
    }

    protected abstract static class BuilderBase<RequestType extends SimpleAnimationRequestBase> implements  Builder<RequestType>
    {
        private int width;
        private int height;
        private int frameCount;
        private File exportPath;

        protected int getWidth()
        {
            return width;
        }

        protected int getHeight()
        {
            return height;
        }

        protected int getFrameCount()
        {
            return frameCount;
        }

        protected File getExportPath()
        {
            return exportPath;
        }

        @Override
        public Builder<RequestType> setWidth(int width)
        {
            this.width = width;
            return this;
        }

        @Override
        public Builder<RequestType> setHeight(int height)
        {
            this.height = height;
            return this;
        }

        @Override
        public Builder<RequestType> setFrameCount(int frameCount)
        {
            this.frameCount = frameCount;
            return this;
        }

        @Override
        public Builder<RequestType> setExportPath(File exportPath)
        {
            this.exportPath = exportPath;
            return this;
        }
    }

    protected SimpleAnimationRequestBase(int width, int height, int frameCount, File exportPath)
    {
        this.width = width;
        this.height = height;
        this.frameCount = frameCount;
        this.exportPath = exportPath;
    }

    protected abstract Matrix4 getViewMatrix(int frame, Matrix4 baseViewMatrix);

    public int getFrameCount()
    {
        return frameCount;
    }

    @Override
    public void executeRequest(IBRInstance<ContextType> renderable, LoadingMonitor callback) throws IOException
    {
        try
        (
            FramebufferObject<ContextType> framebuffer = renderable.getIBRResources().context.buildFramebufferObject(width, height)
                .addColorAttachment()
                .addDepthAttachment()
                .createFramebufferObject()
        )
        {

            for (int i = 0; i < frameCount; i++)
            {
                framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, /*1.0f*/0.0f);
                framebuffer.clearDepthBuffer();

                renderable.draw(framebuffer,
                    renderable.getSceneModel().getUnscaledMatrix(getViewMatrix(i, renderable.getSceneModel().getCameraModel().getLookMatrix()))
                        .times(renderable.getSceneModel().getBaseModelMatrix()),
                    null, 320, 180);

                File exportFile = new File(exportPath, String.format("%04d.png", i));
                exportFile.getParentFile().mkdirs();
                framebuffer.saveColorBufferToFile(0, "PNG", exportFile);

                if (callback != null)
                {
                    callback.setProgress((double) i / (double) frameCount);
                }
            }
        }
    }
}
