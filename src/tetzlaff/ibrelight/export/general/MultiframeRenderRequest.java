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

package tetzlaff.ibrelight.export.general;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.gl.core.Program;
import tetzlaff.ibrelight.core.IBRInstance;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.rendering.resources.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

class MultiframeRenderRequest<ContextType extends Context<ContextType>> extends RenderRequestBase<ContextType>
{
    private final int frameCount;

    MultiframeRenderRequest(int width, int height, int frameCount, ReadonlySettingsModel settingsModel,
        Consumer<Program<ContextType>> shaderSetupCallback, File vertexShader, File fragmentShader, File outputDirectory)
    {
        super(width, height, settingsModel, shaderSetupCallback, vertexShader, fragmentShader, outputDirectory);
        this.frameCount = frameCount;
    }

    static class Builder<ContextType extends Context<ContextType>> extends BuilderBase<ContextType>
    {
        private final int frameCount;

        Builder(int frameCount, ReadonlySettingsModel settingsModel, File fragmentShader, File outputDirectory)
        {
            super(settingsModel, fragmentShader, outputDirectory);
            this.frameCount = frameCount;
        }

        @Override
        public IBRRequest<ContextType> create()
        {
            return new MultiframeRenderRequest<>(getWidth(), getHeight(), frameCount, getSettingsModel(), getShaderSetupCallback(),
                getVertexShader(), getFragmentShader(), getOutputDirectory());
        }
    }

    @Override
    public void executeRequest(IBRInstance<ContextType> renderable, LoadingMonitor callback) throws IOException
    {
        IBRResources<ContextType> resources = renderable.getIBRResources();

        try
        (
            Program<ContextType> program = createProgram(resources);
            FramebufferObject<ContextType> framebuffer = createFramebuffer(resources.context)
        )
        {
            Drawable<ContextType> drawable = createDrawable(program, resources);

            for (int i = 0; i < frameCount; i++)
            {
                program.setUniform("frame", i);
                program.setUniform("frameCount", frameCount);
                program.setUniform("model_view", renderable.getActiveViewSet().getCameraPose(0));
                program.setUniform("projection",
                    renderable.getActiveViewSet().getCameraProjection(
                        renderable.getActiveViewSet().getCameraProjectionIndex(0))
                        .getProjectionMatrix(renderable.getActiveViewSet().getRecommendedNearPlane(),
                            renderable.getActiveViewSet().getRecommendedFarPlane()));

                render(drawable, framebuffer);

                File exportFile = new File(getOutputDirectory(), String.format("%04d.png", i));
                getOutputDirectory().mkdirs();
                framebuffer.saveColorBufferToFile(0, "PNG", exportFile);

                if (callback != null)
                {
                    callback.setProgress((double) i / (double) frameCount);
                }
            }
        }
    }
}
