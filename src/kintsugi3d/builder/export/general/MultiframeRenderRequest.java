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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.function.Consumer;

import kintsugi3d.builder.core.IBRInstance;
import kintsugi3d.builder.core.ObservableIBRRequest;
import kintsugi3d.builder.core.ProgressMonitor;
import kintsugi3d.builder.core.UserCancellationException;
import kintsugi3d.builder.resources.ibr.IBRResourcesImageSpace;
import kintsugi3d.builder.state.ReadonlySettingsModel;
import kintsugi3d.gl.core.*;

class MultiframeRenderRequest extends RenderRequestBase
{
    private final int frameCount;

    MultiframeRenderRequest(int width, int height, int frameCount, ReadonlySettingsModel settingsModel,
        Consumer<Program<? extends Context<?>>> shaderSetupCallback, File vertexShader, File fragmentShader, File outputDirectory)
    {
        super(width, height, settingsModel, shaderSetupCallback, vertexShader, fragmentShader, outputDirectory);
        this.frameCount = frameCount;
    }

    static class Builder extends BuilderBase
    {
        private final int frameCount;

        Builder(int frameCount, ReadonlySettingsModel settingsModel, File fragmentShader, File outputDirectory)
        {
            super(settingsModel, fragmentShader, outputDirectory);
            this.frameCount = frameCount;
        }

        @Override
        public ObservableIBRRequest create()
        {
            return new MultiframeRenderRequest(getWidth(), getHeight(), frameCount, getSettingsModel(), getShaderSetupCallback(),
                getVertexShader(), getFragmentShader(), getOutputDirectory());
        }
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(IBRInstance<ContextType> renderable, ProgressMonitor monitor)
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
            if(monitor != null){
                monitor.setProcessName("Generic Export");
            }
            for (int i = 0; i < frameCount; i++)
            {
                if (monitor != null)
                {
                    monitor.allowUserCancellation();
                }

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
                framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", exportFile);

                if (monitor != null)
                {
                    monitor.setProgress((double) i / (double) frameCount,
                        MessageFormat.format("Frame {0}/{1}", i+1, frameCount));
                }
            }
        }
    }
}
