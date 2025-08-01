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

package kintsugi3d.builder.export.general;

import kintsugi3d.builder.core.IBRInstance;
import kintsugi3d.builder.core.ObservableIBRRequest;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.UserCancellationException;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.gl.core.*;
import kintsugi3d.util.ImageFinder;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.function.Consumer;

class MultiviewRenderRequest extends RenderRequestBase
{
    MultiviewRenderRequest(int width, int height, Consumer<Program<? extends Context<?>>> shaderSetupCallback,
                           File vertexShader, File fragmentShader, File outputDirectory)
    {
        super(width, height, shaderSetupCallback, vertexShader, fragmentShader, outputDirectory);
    }

    static class Builder extends BuilderBase
    {
        Builder(File fragmentShader, File outputDirectory)
        {
            super(fragmentShader, outputDirectory);
        }

        @Override
        public ObservableIBRRequest create()
        {
            return new MultiviewRenderRequest(getWidth(), getHeight(), getShaderSetupCallback(),
                getVertexShader(), getFragmentShader(), getOutputDirectory());
        }
    }

    public <ContextType extends Context<ContextType>> void executeRequest(
        IBRInstance<ContextType> renderable, ProgressMonitor monitor)
            throws IOException, UserCancellationException
    {
        IBRResourcesImageSpace<ContextType> resources = renderable.getIBRResources();

        try
        (
            ProgramObject<ContextType> program = createProgram(resources);
            FramebufferObject<ContextType> framebuffer = createFramebuffer(resources.getContext());
            Drawable<ContextType> drawable = createDrawable(program, resources)
        )
        {
            for (int i = 0; i < resources.getViewSet().getCameraPoseCount(); i++)
            {
                if (monitor != null)
                {
                    monitor.allowUserCancellation();
                }
                program.setUniform("viewIndex", i);
                program.setUniform("model_view", renderable.getActiveViewSet().getCameraPose(i));
                program.setUniform("projection",
                    renderable.getActiveViewSet().getCameraProjection(
                        renderable.getActiveViewSet().getCameraProjectionIndex(i))
                        .getProjectionMatrix(renderable.getActiveViewSet().getRecommendedNearPlane(),
                            renderable.getActiveViewSet().getRecommendedFarPlane()));

                render(drawable, framebuffer);

                String fileName = ImageFinder.getInstance().getImageFileNameWithFormat(
                    renderable.getActiveViewSet().getImageFileName(i), "png");

                File exportFile = new File(getOutputDirectory(), fileName);
                getOutputDirectory().mkdirs();
                framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", exportFile);

                if (monitor != null)
                {
                    monitor.setProgress((double) i / (double) resources.getViewSet().getCameraPoseCount(),
                        MessageFormat.format("{0} ({1}/{2})", resources.getViewSet().getImageFileName(i), i+1, resources.getViewSet().getCameraPoseCount()));
                }
            }
        }
    }
}
