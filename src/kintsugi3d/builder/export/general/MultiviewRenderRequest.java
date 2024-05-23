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

package kintsugi3d.builder.export.general;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import kintsugi3d.gl.core.*;
import kintsugi3d.builder.core.IBRInstance;
import kintsugi3d.builder.core.ObservableIBRRequest;
import kintsugi3d.builder.core.LoadingMonitor;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.builder.state.ReadonlySettingsModel;

class MultiviewRenderRequest extends RenderRequestBase
{
    MultiviewRenderRequest(int width, int height, ReadonlySettingsModel settingsModel, Consumer<Program<? extends Context<?>>> shaderSetupCallback,
                           File vertexShader, File fragmentShader, File outputDirectory)
    {
        super(width, height, settingsModel, shaderSetupCallback, vertexShader, fragmentShader, outputDirectory);
    }

    static class Builder extends BuilderBase
    {
        Builder(ReadonlySettingsModel settingsModel, File fragmentShader, File outputDirectory)
        {
            super(settingsModel, fragmentShader, outputDirectory);
        }

        @Override
        public ObservableIBRRequest create()
        {
            return new MultiviewRenderRequest(getWidth(), getHeight(), getSettingsModel(), getShaderSetupCallback(),
                getVertexShader(), getFragmentShader(), getOutputDirectory());
        }
    }

    public <ContextType extends Context<ContextType>> void executeRequest(
        IBRInstance<ContextType> renderable, LoadingMonitor callback) throws IOException
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
                program.setUniform("viewIndex", i);
                program.setUniform("model_view", renderable.getActiveViewSet().getCameraPose(i));
                program.setUniform("projection",
                    renderable.getActiveViewSet().getCameraProjection(
                        renderable.getActiveViewSet().getCameraProjectionIndex(i))
                        .getProjectionMatrix(renderable.getActiveViewSet().getRecommendedNearPlane(),
                            renderable.getActiveViewSet().getRecommendedFarPlane()));

                render(drawable, framebuffer);

                String fileName = renderable.getActiveViewSet().getImageFileName(i);

                if (!fileName.endsWith(".png"))
                {
                    String[] parts = fileName.split("\\.");
                    parts[parts.length - 1] = "png";
                    fileName = String.join(".", parts);
                }

                File exportFile = new File(getOutputDirectory(), fileName);
                getOutputDirectory().mkdirs();
                framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", exportFile);

                if (callback != null)
                {
                    callback.setProgress((double) i / (double) resources.getViewSet().getCameraPoseCount());
                }
            }
        }
    }
}
