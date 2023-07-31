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

package kintsugi3d.builder.export.general;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import kintsugi3d.gl.core.*;
import kintsugi3d.builder.core.IBRInstance;
import kintsugi3d.builder.core.IBRRequest;
import kintsugi3d.builder.core.LoadingMonitor;
import kintsugi3d.builder.resources.IBRResourcesImageSpace;
import kintsugi3d.builder.state.ReadonlySettingsModel;

class SingleFrameRenderRequest<ContextType extends Context<ContextType>> extends RenderRequestBase<ContextType>
{
    private final String outputImageName;

    SingleFrameRenderRequest(int width, int height, String outputImageName, ReadonlySettingsModel settingsModel,
        Consumer<Program<ContextType>> shaderSetupCallback, File vertexShader, File fragmentShader, File outputDirectory)
    {
        super(width, height, settingsModel, shaderSetupCallback, vertexShader, fragmentShader, outputDirectory);
        this.outputImageName = outputImageName;
    }

    static class Builder<ContextType extends Context<ContextType>> extends BuilderBase<ContextType>
    {
        private final String outputImageName;

        Builder(String outputImageName, ReadonlySettingsModel settingsModel, File fragmentShader, File outputDirectory)
        {
            super(settingsModel, fragmentShader, outputDirectory);
            this.outputImageName = outputImageName;
        }

        @Override
        public IBRRequest<ContextType> create()
        {
            return new SingleFrameRenderRequest<>(getWidth(), getHeight(), outputImageName, getSettingsModel(), getShaderSetupCallback(),
                getVertexShader(), getFragmentShader(), getOutputDirectory());
        }
    }

    @Override
    public void executeRequest(IBRInstance<ContextType> renderable, LoadingMonitor callback) throws IOException
    {
        IBRResourcesImageSpace<ContextType> resources = renderable.getIBRResources();

        try
        (
            ProgramObject<ContextType> program = createProgram(resources);
            FramebufferObject<ContextType> framebuffer = createFramebuffer(resources.getContext())
        )
        {
            Drawable<ContextType> drawable = createDrawable(program, resources);

            program.setUniform("model_view", renderable.getActiveViewSet().getCameraPose(0));
            program.setUniform("projection",
                renderable.getActiveViewSet().getCameraProjection(
                    renderable.getActiveViewSet().getCameraProjectionIndex(0))
                    .getProjectionMatrix(renderable.getActiveViewSet().getRecommendedNearPlane(),
                        renderable.getActiveViewSet().getRecommendedFarPlane()));

            render(drawable, framebuffer);

            File exportFile = new File(getOutputDirectory(), outputImageName);
            getOutputDirectory().mkdirs();
            framebuffer.getTextureReaderForColorAttachment(0).saveToFile("PNG", exportFile);
        }
    }
}
